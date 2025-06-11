package org.example.collectfocep.services.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.example.collectfocep.constants.ErrorMessages;
import org.example.collectfocep.dto.BalanceVerificationDTO;
import org.example.collectfocep.dto.MouvementCommissionDTO;
import org.example.collectfocep.dto.MouvementProjection;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.CompteNotFoundException;
import org.example.collectfocep.exceptions.SoldeInsuffisantException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.MouvementMapperV2;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.exceptions.MontantMaxRetraitException;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.services.interfaces.DateTimeService;
import org.example.collectfocep.services.interfaces.JournalService;
import org.example.collectfocep.services.interfaces.MouvementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.exceptions.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MouvementServiceImpl implements MouvementService {


    private final DateTimeService dateTimeService;
    private final CompteRepository compteRepository;
    private final CompteClientRepository compteClientRepository;
    private final CompteCollecteurRepository compteCollecteurRepository;
    private final CompteLiaisonRepository compteLiaisonRepository;
    private final ClientRepository clientRepository;
    private final JournalRepository journalRepository;
    private final CompteService compteService;
    private final CommissionService commissionService;
    private final TransactionService transactionService;
    private final ClientAccountInitializationService clientAccountInitializationService;
    private final SystemAccountService systemAccountService;
    private final CollecteurAccountService collecteurAccountService;
    private final JournalService journalService;

    // Métriques injectées via Spring
    private final Counter epargneCounter;
    private final Timer mouvementTimer;

    @Autowired
    private MouvementRepository mouvementRepository;

    @Autowired
    private MouvementMapperV2 mouvementMapper;

    @Autowired
    public MouvementServiceImpl(
            DateTimeService dateTimeService,
            SystemAccountService systemAccountService,
            CollecteurAccountService collecteurAccountService,
            MouvementRepository mouvementRepository,
            CompteRepository compteRepository,
            CompteClientRepository compteClientRepository,
            CompteCollecteurRepository compteCollecteurRepository,
            CompteLiaisonRepository compteLiaisonRepository,
            ClientRepository clientRepository,
            JournalService journalService,
            JournalRepository journalRepository,
            CompteService compteService,
            CommissionService commissionService,
            TransactionService transactionService,
            ClientAccountInitializationService clientAccountInitializationService,
            Counter epargneCounter,
            Timer mouvementTimer) {

        this.dateTimeService = dateTimeService;
        this.systemAccountService = systemAccountService;
        this.collecteurAccountService = collecteurAccountService;
        this.mouvementRepository = mouvementRepository;
        this.compteRepository = compteRepository;
        this.compteClientRepository = compteClientRepository;
        this.compteCollecteurRepository = compteCollecteurRepository;
        this.compteLiaisonRepository = compteLiaisonRepository;
        this.clientRepository = clientRepository;
        this.journalRepository = journalRepository;
        this.journalService = journalService;
        this.compteService = compteService;
        this.commissionService = commissionService;
        this.transactionService = transactionService;
        this.clientAccountInitializationService = clientAccountInitializationService;
        this.epargneCounter = epargneCounter;
        this.mouvementTimer = mouvementTimer;
    }

    /**
     * Récupération du compte service d'un collecteur
     */
    private CompteCollecteur getCompteServiceCollecteur(Collecteur collecteur) {
        log.debug("Recherche du compte service pour collecteur ID={}", collecteur.getId());

        try {
            CompteCollecteur compteServiceCollecteur = compteService.findServiceAccount(collecteur);
            log.debug("Compte service trouvé via CompteService: ID={}, Numéro={}",
                    compteServiceCollecteur.getId(), compteServiceCollecteur.getNumeroCompte());
            return compteServiceCollecteur;
        } catch (ResourceNotFoundException e) {
            log.debug("Compte service non trouvé via CompteService, tentative de création...");
        }

        ensureCollecteurAccountsExist(collecteur);

        try {
            CompteCollecteur compteServiceCollecteur = compteService.findServiceAccount(collecteur);
            log.debug("Compte service trouvé après création: ID={}, Numéro={}",
                    compteServiceCollecteur.getId(), compteServiceCollecteur.getNumeroCompte());
            return compteServiceCollecteur;
        } catch (ResourceNotFoundException e) {
            log.error("Compte service toujours non trouvé après création pour collecteur ID={}",
                    collecteur.getId());
            throw new CompteNotFoundException("Compte service du collecteur non trouvé après création");
        }
    }

    /**
     * S'assurer que les comptes du collecteur existent
     */
    private void ensureCollecteurAccountsExist(Collecteur collecteur) {
        log.debug("Vérification de l'existence des comptes pour collecteur ID={}", collecteur.getId());

        try {
            compteService.createCollecteurAccounts(collecteur);
            log.info("Comptes créés/vérifiés pour collecteur ID={}", collecteur.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la création/vérification des comptes: {}", e.getMessage());
            throw new BusinessException("Impossible de créer les comptes du collecteur",
                    "COMPTE_CREATION_ERROR", e.getMessage());
        }
    }

    /**
     * Effectue un mouvement entre deux comptes avec gestion des transactions
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {BusinessException.class, Exception.class},
            timeout = 30
    )
    public Mouvement effectuerMouvement(Mouvement mouvement) {
        log.info("DÉBUT TRANSACTION [{}]: Source={}, Destination={}, Montant={}, Sens={}",
                UUID.randomUUID().toString().substring(0, 8),
                mouvement.getCompteSource() != null ? mouvement.getCompteSource().getNumeroCompte() : "null",
                mouvement.getCompteDestination() != null ? mouvement.getCompteDestination().getNumeroCompte() : "null",
                mouvement.getMontant(),
                mouvement.getSens());

        return transactionService.executeInTransaction(status -> {
            try {
                log.debug("Démarrage de la transaction pour le mouvement entre comptes");

                // Validation des comptes
                Compte compteSource = validateAndGetCompte(mouvement.getCompteSource().getId());
                Compte compteDestination = validateAndGetCompte(mouvement.getCompteDestination().getId());

                log.debug("Comptes validés - Source: {}, Destination: {}",
                        compteSource.getNumeroCompte(), compteDestination.getNumeroCompte());

                // Verrouillage optimiste avec @Version dans l'entité Compte
                verifierSoldeDisponible(compteSource, mouvement.getMontant(), mouvement.getSens());
                log.debug("Vérification du solde réussie - Compte: {}, Solde actuel: {}, Montant opération: {}",
                        compteSource.getNumeroCompte(), compteSource.getSolde(), mouvement.getMontant());

                // Enregistrer l'état avant modification pour journalisation
                double soldeSourceAvant = compteSource.getSolde();
                double soldeDestinationAvant = compteDestination.getSolde();

                mettreAJourSoldes(compteSource, compteDestination, mouvement.getMontant(), mouvement.getSens());

                log.debug("Mise à jour des soldes - Source: {} ({} → {}), Destination: {} ({} → {})",
                        compteSource.getNumeroCompte(), soldeSourceAvant, compteSource.getSolde(),
                        compteDestination.getNumeroCompte(), soldeDestinationAvant, compteDestination.getSolde());

                // Sauvegarde des modifications
                compteRepository.save(compteSource);
                compteRepository.save(compteDestination);

                mouvement.setDateOperation(dateTimeService.getCurrentDateTime());
                Mouvement mouvementSauvegarde = mouvementRepository.save(mouvement);

                log.info("Mouvement réussi: ID={}, Montant={}, Source={} (Solde={}), Destination={} (Solde={})",
                        mouvementSauvegarde.getId(), mouvementSauvegarde.getMontant(),
                        compteSource.getNumeroCompte(), compteSource.getSolde(),
                        compteDestination.getNumeroCompte(), compteDestination.getSolde());

                return mouvementSauvegarde;

            } catch (Exception e) {
                log.error("Erreur lors de l'exécution du mouvement - Cause: {}", e.getMessage(), e);
                status.setRollbackOnly();
                throw new BusinessException("Erreur lors de l'exécution du mouvement: " + e.getMessage(),
                        "MOUVEMENT_ERROR", e.getMessage());
            }
        });
    }

    /**
     * Traitement asynchrone des commissions dans une nouvelle transaction
     */
    @Override
    public void traiterCommissionsAsync(Mouvement mouvement) {
        log.info("Début du traitement asynchrone des commissions pour mouvement ID={}", mouvement.getId());

        transactionService.executeInNewTransaction(status -> {
            try {
                log.debug("Traitement des commissions pour mouvement: {}", mouvement.getId());
                appliquerCommissions(mouvement);
                log.info("Commission appliquée avec succès pour mouvement ID={}", mouvement.getId());
                return null;
            } catch (Exception e) {
                log.error("Erreur lors du traitement des commissions - Mouvement: {}, Cause: {}",
                        mouvement.getId(), e.getMessage(), e);
                status.setRollbackOnly();
                throw new BusinessException("Erreur lors du traitement des commissions", "COMMISSION_ERROR", e.getMessage());
            }
        });
    }

    /**
     * Validation et récupération d'un compte avec verrouillage
     */
    private Compte validateAndGetCompte(Long compteId) {
        log.debug("Validation du compte ID={}", compteId);
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> {
                    log.error("Compte non trouvé: ID={}", compteId);
                    return new CompteNotFoundException(
                            String.format(ErrorMessages.RESOURCE_NOT_FOUND, "Compte " + compteId));
                });
        log.debug("Compte validé: ID={}, Numéro={}, Solde={}", compte.getId(), compte.getNumeroCompte(), compte.getSolde());
        return compte;
    }

    /**
     * Vérifie que le solde du compte est suffisant pour l'opération
     */
    private void verifierSoldeDisponible(Compte compte, double montant, String sens) throws SoldeInsuffisantException {
        log.debug("Vérification du solde disponible: Compte={}, Solde={}, Montant={}, Sens={}",
                compte.getNumeroCompte(), compte.getSolde(), montant, sens);

        if (("debit".equals(sens) || "retrait".equals(sens) || "versement".equals(sens))
                && compte.getSolde() < montant) {
            log.warn("Solde insuffisant: Compte={}, Solde={}, Montant={}",
                    compte.getNumeroCompte(), compte.getSolde(), montant);
            throw new SoldeInsuffisantException("Solde insuffisant sur le compte " + compte.getId() +
                    " : " + compte.getSolde() + " < " + montant);
        }

        log.debug("Vérification du solde réussie");
    }

    /**
     * Création d'un mouvement d'épargne
     */
    private Mouvement creerMouvementEpargne(
            Compte source,
            Compte destination,
            double montant,
            Client client,
            Journal journal) {

        log.debug("Création d'un mouvement d'épargne: Client={}, Montant={}, Journal={}",
                client.getNom() + " " + client.getPrenom(), montant, journal != null ? journal.getId() : "null");

        Mouvement mouvement = new Mouvement();
        mouvement.setMontant(montant);
        mouvement.setLibelle(String.format("Epargne client : %s %s", client.getNom(), client.getPrenom()));
        mouvement.setSens("epargne");
        // UTILISATION DU DateTimeService
        mouvement.setDateOperation(dateTimeService.getCurrentDateTime());
        mouvement.setCompteSource(source);
        mouvement.setCompteDestination(destination);
        mouvement.setJournal(journal);

        log.debug("Mouvement d'épargne créé: Client={}, Montant={}, Source={}, Destination={}",
                client.getNom() + " " + client.getPrenom(), montant, source.getNumeroCompte(), destination.getNumeroCompte());

        return mouvement;
    }

    /**
     * Met à jour les soldes des comptes selon le sens de l'opération
     */
    private void mettreAJourSoldes(Compte compteSource, Compte compteDestination, double montant, String sens) {
        log.debug("Mise à jour des soldes: Source={} (Solde={}), Destination={} (Solde={}), Montant={}, Sens={}",
                compteSource.getNumeroCompte(), compteSource.getSolde(),
                compteDestination.getNumeroCompte(), compteDestination.getSolde(),
                montant, sens);

        switch(sens.toLowerCase()) {
            case "debit":
                compteSource.setSolde(compteSource.getSolde() - montant);
                compteDestination.setSolde(compteDestination.getSolde() + montant);
                break;
            case "credit":
                compteSource.setSolde(compteSource.getSolde() + montant);
                compteDestination.setSolde(compteDestination.getSolde() - montant);
                break;
            case "epargne":
                // Pour l'épargne, on débite le compte service du collecteur et on crédite le compte client
                compteSource.setSolde(compteSource.getSolde() - montant);
                compteDestination.setSolde(compteDestination.getSolde() + montant);
                break;
            case "retrait":
                // Pour le retrait, on débite le compte client et on crédite le compte service
                compteSource.setSolde(compteSource.getSolde() - montant);
                compteDestination.setSolde(compteDestination.getSolde() + montant);
                break;
            case "versement":
                // Pour le versement en agence, on débite le compte liaison et on crédite le compte service
                compteSource.setSolde(compteSource.getSolde() - montant);
                compteDestination.setSolde(compteDestination.getSolde() + montant);
                break;
            default:
                log.error("Type d'opération non reconnu: {}", sens);
                throw new IllegalArgumentException("Sens d'opération non reconnu: " + sens);
        }

        log.debug("Soldes mis à jour - Source: {} (Nouveau solde={}), Destination: {} (Nouveau solde={})",
                compteSource.getNumeroCompte(), compteSource.getSolde(),
                compteDestination.getNumeroCompte(), compteDestination.getSolde());
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {BusinessException.class, Exception.class},
            timeout = 30
    )
    public Mouvement enregistrerEpargne(Client client, double montant, Journal journal) {
        log.info("Début enregistrement épargne: Client={} (ID={}), Montant={}",
                client.getNom() + " " + client.getPrenom(), client.getId(), montant);

        return transactionService.executeInTransaction(status -> {
            try {
                // 1. Recharger le client avec toutes ses relations
                Client clientWithRelations = clientRepository.findByIdWithAllRelations(client.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

                // 2. ✅ RÉCUPÉRATION AUTOMATIQUE DU JOURNAL DU JOUR avec DateTimeService
                Journal journalDuJour;
                if (journal != null) {
                    journalDuJour = journal;
                } else {
                    journalDuJour = journalService.getOrCreateJournalDuJour(
                            clientWithRelations.getCollecteur().getId(),
                            dateTimeService.getCurrentDate() // ✅ UTILISATION DU SERVICE
                    );
                    log.info("📅 Journal automatique: ID={}, Date={}",
                            journalDuJour.getId(), journalDuJour.getDateDebut());
                }

                // 3. Récupérer les comptes
                CompteClient compteClient = compteClientRepository.findByClient(clientWithRelations)
                        .orElseGet(() -> clientAccountInitializationService.ensureClientAccountExists(clientWithRelations));

                CompteCollecteur compteService = getCompteServiceCollecteur(clientWithRelations.getCollecteur());

                // 4. Créer le mouvement avec le journal du jour
                Mouvement mouvement = creerMouvementEpargne(
                        compteService,
                        compteClient,
                        montant,
                        clientWithRelations,
                        journalDuJour
                );

                // 5. Définir le type de mouvement
                mouvement.setTypeMouvement("EPARGNE");
                mouvement.setCollecteur(clientWithRelations.getCollecteur());
                mouvement.setClient(clientWithRelations);

                // 6. Exécuter le mouvement
                Mouvement mouvementEnregistre = effectuerMouvement(mouvement);

                log.info("✅ Épargne enregistrée: ID={}, Journal={}, Client={}",
                        mouvementEnregistre.getId(), journalDuJour.getId(),
                        clientWithRelations.getNom());

                return mouvementEnregistre;

            } catch (Exception e) {
                log.error("❌ Erreur épargne: {}", e.getMessage(), e);
                status.setRollbackOnly();
                throw new BusinessException("Erreur lors de l'enregistrement de l'épargne",
                        "EPARGNE_ERROR", e.getMessage());
            }
        });
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {BusinessException.class, Exception.class},
            timeout = 30
    )
    public Mouvement enregistrerRetrait(Client client, double montant, Journal journal) {
        log.info("Début enregistrement retrait: Client={} (ID={}), Montant={}",
                client.getNom() + " " + client.getPrenom(), client.getId(), montant);

        try {
            // 1. Recharger le client avec ses relations
            Client clientRecharge = clientRepository.findByIdWithAllRelations(client.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

            // 2. ✅ RÉCUPÉRATION AUTOMATIQUE DU JOURNAL DU JOUR avec DateTimeService
            Journal journalDuJour;
            if (journal != null) {
                journalDuJour = journal;
            } else {
                journalDuJour = journalService.getOrCreateJournalDuJour(
                        clientRecharge.getCollecteur().getId(),
                        dateTimeService.getCurrentDate() // ✅ UTILISATION DU SERVICE
                );
                log.info("📅 Journal automatique retrait: ID={}, Date={}",
                        journalDuJour.getId(), journalDuJour.getDateDebut());
            }

            // 3. Récupération des comptes et validations
            CompteClient compteClient = compteClientRepository.findByClient(clientRecharge)
                    .orElseThrow(() -> new CompteNotFoundException("Compte client non trouvé"));

            CompteCollecteur compteService = getCompteServiceCollecteur(clientRecharge.getCollecteur());

            validateRetrait(compteClient, clientRecharge.getCollecteur(), montant);

            // 4. Création du mouvement avec journal automatique
            Mouvement mouvement = creerMouvementRetrait(
                    compteClient,
                    compteService,
                    montant,
                    clientRecharge,
                    journalDuJour
            );

            // 5. Définir le type et les relations
            mouvement.setTypeMouvement("RETRAIT");
            mouvement.setCollecteur(clientRecharge.getCollecteur());
            mouvement.setClient(clientRecharge);

            // 6. Exécution directe du mouvement
            Compte compteSource = validateAndGetCompte(mouvement.getCompteSource().getId());
            Compte compteDestination = validateAndGetCompte(mouvement.getCompteDestination().getId());

            verifierSoldeDisponible(compteSource, mouvement.getMontant(), mouvement.getSens());

            double soldeSourceAvant = compteSource.getSolde();
            double soldeDestinationAvant = compteDestination.getSolde();

            mettreAJourSoldes(compteSource, compteDestination, mouvement.getMontant(), mouvement.getSens());

            compteRepository.save(compteSource);
            compteRepository.save(compteDestination);

            // ✅ UTILISATION DU DateTimeService
            mouvement.setDateOperation(dateTimeService.getCurrentDateTime());
            Mouvement mouvementEnregistre = mouvementRepository.save(mouvement);

            log.info("✅ Retrait enregistré: ID={}, Journal={}, Client={}",
                    mouvementEnregistre.getId(), journalDuJour.getId(),
                    clientRecharge.getNom());

            return mouvementEnregistre;

        } catch (SoldeInsuffisantException | MontantMaxRetraitException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur retrait: {}", e.getMessage(), e);
            throw new BusinessException("Erreur lors de l'enregistrement du retrait",
                    "RETRAIT_ERROR", e.getMessage());
        }
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {BusinessException.class, Exception.class},
            timeout = 30
    )
    public Mouvement enregistrerVersement(Collecteur collecteur, double montant, Journal journal) {
        log.info("Début enregistrement versement: Collecteur={} (ID={}), Montant={}, Journal={}",
                collecteur.getNom() + " " + collecteur.getPrenom(), collecteur.getId(), montant, journal != null ? journal.getId() : "null");

        return transactionService.executeInTransaction(status -> {
            try {
                log.debug("Démarrage transaction de versement");

                // Validation et récupération des comptes
                CompteCollecteur compteService = getCompteServiceCollecteur(collecteur);
                log.debug("Compte service collecteur trouvé: ID={}, Numéro={}, Solde={}",
                        compteService.getId(), compteService.getNumeroCompte(), compteService.getSolde());

                CompteLiaison compteLiaison = compteLiaisonRepository
                        .findByAgenceAndTypeCompte(collecteur.getAgence(), "LIAISON")
                        .orElseThrow(() -> {
                            log.error("Compte de liaison non trouvé pour agence ID={}", collecteur.getAgence().getId());
                            return new CompteNotFoundException(
                                    String.format(ErrorMessages.RESOURCE_NOT_FOUND, "Compte de liaison")
                            );
                        });
                log.debug("Compte liaison trouvé: ID={}, Numéro={}, Solde={}",
                        compteLiaison.getId(), compteLiaison.getNumeroCompte(), compteLiaison.getSolde());

                // Création et exécution du mouvement
                Mouvement mouvement = creerMouvementVersement(
                        compteLiaison,
                        compteService,
                        montant,
                        collecteur,
                        journal
                );

                Mouvement mouvementEnregistre = effectuerMouvement(mouvement);
                log.info("Versement enregistré avec succès: ID={}, Collecteur={} (ID={}), Montant={}",
                        mouvementEnregistre.getId(), collecteur.getNom() + " " + collecteur.getPrenom(), collecteur.getId(), montant);

                return mouvementEnregistre;

            } catch (Exception e) {
                log.error("Erreur lors de l'enregistrement du versement - Collecteur: {} (ID={}), Montant: {}, Erreur: {}",
                        collecteur.getNom() + " " + collecteur.getPrenom(), collecteur.getId(), montant, e.getMessage(), e);
                status.setRollbackOnly();
                throw new BusinessException("Erreur lors de l'enregistrement du versement",
                        "VERSEMENT_ERROR", e.getMessage());
            }
        });
    }

    /**
     * Applique les commissions pour un mouvement selon les règles d'entreprise
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {BusinessException.class, Exception.class}
    )
    private void appliquerCommissions(Mouvement mouvement) {
        log.info("Début application des commissions pour mouvement ID={}", mouvement.getId());

        transactionService.executeInTransaction(status -> {
            try {
                log.debug("Application des commissions pour le mouvement: {}", mouvement.getId());

                double tvaRate = 0.1925;
                double montantCommission = commissionService.calculerCommission(mouvement);
                double tva = montantCommission * tvaRate;
                double netCommission = montantCommission - tva;

                log.debug("Calcul commission: Mouvement={}, Montant={}, Commission={}, TVA={}, Net={}",
                        mouvement.getId(), mouvement.getMontant(), montantCommission, tva, netCommission);

                // Récupération ou création des comptes nécessaires
                systemAccountService.ensureSystemAccountsExist();

                Compte compteAttente = compteRepository.findByTypeCompte("ATTENTE")
                        .orElseGet(() -> {
                            log.warn("Compte d'attente système non trouvé, création en cours...");
                            return systemAccountService.ensureSystemCompteExists("ATTENTE", "Compte Attente Système", "ATT-SYS");
                        });

                Compte compteTaxe = compteRepository.findByTypeCompte("TAXE")
                        .orElseGet(() -> {
                            log.warn("Compte taxe système non trouvé, création en cours...");
                            return systemAccountService.ensureSystemCompteExists("TAXE", "Compte Taxe Système", "TAXE-SYS");
                        });

                Compte compteProduit = compteRepository.findByTypeCompte("PRODUIT")
                        .orElseGet(() -> {
                            log.warn("Compte produit système non trouvé, création en cours...");
                            return systemAccountService.ensureSystemCompteExists("PRODUIT", "Compte Produit FOCEP", "PROD-SYS");
                        });

                // Si le mouvement est lié à un collecteur, utiliser son compte d'attente personnel
                CompteCollecteur compteAttenteCollecteur = null;
                if (mouvement.getCompteDestination() instanceof CompteClient) {
                    Client client = ((CompteClient) mouvement.getCompteDestination()).getClient();
                    if (client != null && client.getCollecteur() != null) {
                        compteAttenteCollecteur = collecteurAccountService.ensureCompteAttenteExists(client.getCollecteur());
                        if (compteAttenteCollecteur != null) {
                            compteAttente = compteAttenteCollecteur;
                            log.debug("Utilisation du compte d'attente du collecteur: ID={}, Solde={}",
                                    compteAttente.getId(), compteAttente.getSolde());
                        }
                    }
                }

                // Répartition des fonds
                compteAttente.setSolde(compteAttente.getSolde() + netCommission);
                compteTaxe.setSolde(compteTaxe.getSolde() + tva);

                // FOCEP reçoit 30% de la commission nette
                double partFOCEP = netCommission * 0.3;
                compteProduit.setSolde(compteProduit.getSolde() + partFOCEP);

                log.debug("Répartition: Attente (+{} → {}), Taxe (+{} → {}), Produit (+{} → {})",
                        netCommission, compteAttente.getSolde(),
                        tva, compteTaxe.getSolde(),
                        partFOCEP, compteProduit.getSolde());

                // Enregistrer les mouvements de commission
                Mouvement mouvementAttente = creerMouvementCommission(
                        mouvement.getCompteSource(),
                        compteAttente,
                        netCommission,
                        "Commission sur " + mouvement.getLibelle(),
                        mouvement.getJournal()
                );

                Mouvement mouvementTva = creerMouvementCommission(
                        mouvement.getCompteSource(),
                        compteTaxe,
                        tva,
                        "TVA sur commission - " + mouvement.getLibelle(),
                        mouvement.getJournal()
                );

                Mouvement mouvementProduit = creerMouvementCommission(
                        compteAttente,
                        compteProduit,
                        partFOCEP,
                        "Part FOCEP sur commission - " + mouvement.getLibelle(),
                        mouvement.getJournal()
                );

                // Mise à jour des comptes
                compteRepository.save(compteAttente);
                compteRepository.save(compteTaxe);
                compteRepository.save(compteProduit);

                // Enregistrer les mouvements de commission
                mouvementRepository.save(mouvementAttente);
                mouvementRepository.save(mouvementTva);
                mouvementRepository.save(mouvementProduit);

                log.info("Commissions appliquées avec succès pour mouvement ID={}: Commission={}, TVA={}, Part FOCEP={}",
                        mouvement.getId(), montantCommission, tva, partFOCEP);

                return null;
            } catch (Exception e) {
                log.error("Erreur lors de l'application des commissions - Mouvement: {}, Erreur: {}",
                        mouvement.getId(), e.getMessage(), e);
                status.setRollbackOnly();
                throw new BusinessException("Erreur lors de l'application des commissions",
                        "COMMISSION_ERROR", e.getMessage());
            }
        });
    }

    /**
     * Crée un mouvement de commission
     */
    private Mouvement creerMouvementCommission(Compte source, Compte destination, double montant, String libelle, Journal journal) {
        log.debug("Création mouvement commission: Source={}, Destination={}, Montant={}, Libellé={}",
                source.getNumeroCompte(), destination.getNumeroCompte(), montant, libelle);

        Mouvement mouvement = new Mouvement();
        mouvement.setMontant(montant);
        mouvement.setLibelle(libelle);
        mouvement.setSens("debit");
        // ✅ UTILISATION DU DateTimeService
        mouvement.setDateOperation(dateTimeService.getCurrentDateTime());
        mouvement.setCompteSource(source);
        mouvement.setCompteDestination(destination);
        mouvement.setJournal(journal);

        return mouvement;
    }

    /**
     * Clôture une journée avec gestion des transactions
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = {BusinessException.class, Exception.class},
            timeout = 30
    )
    public Journal cloturerJournee(Collecteur collecteur, Journal journal) {
        log.info("Début clôture de journée: Collecteur={} (ID={}), Journal={}",
                collecteur.getNom() + " " + collecteur.getPrenom(), collecteur.getId(), journal.getId());

        return transactionService.executeInTransaction(status -> {
            try {
                log.debug("Démarrage transaction de clôture de journée");

                // Validation
                if (!journal.getCollecteur().equals(collecteur)) {
                    log.error("Journal invalide pour le collecteur: Journal.collecteur={}, Collecteur.id={}",
                            journal.getCollecteur().getId(), collecteur.getId());
                    throw new BusinessException(
                            "Journal invalide",
                            "JOURNAL_ERROR",
                            "Ce journal n'appartient pas au collecteur spécifié"
                    );
                }

                journal.setEstCloture(true);
                // ✅ UTILISATION DU DateTimeService
                journal.setDateCloture(dateTimeService.getCurrentDateTime());

                Journal journalCloture = journalService.saveJournal(journal);
                log.info("Clôture de journée effectuée avec succès pour journal ID={}, collecteur={}",
                        journalCloture.getId(), collecteur.getNom() + " " + collecteur.getPrenom());
                return journalCloture;

            } catch (Exception e) {
                log.error("Erreur lors de la clôture de journée - Collecteur: {}, Journal: {}, Erreur: {}",
                        collecteur.getId(), journal.getId(), e.getMessage(), e);
                status.setRollbackOnly();
                throw new BusinessException("Erreur lors de la clôture de journée",
                        "CLOTURE_ERROR", e.getMessage());
            }
        });
    }

    private void validateRetrait(CompteClient compteClient, Collecteur collecteur, double montant)
            throws SoldeInsuffisantException, MontantMaxRetraitException {
        log.debug("Validation du retrait: Client={}, Collecteur={}, Montant={}, SoldeClient={}, MontantMaxRetrait={}",
                compteClient.getClient().getNom(), collecteur.getNom(), montant, compteClient.getSolde(), collecteur.getMontantMaxRetrait());

        if (compteClient.getSolde() < montant) {
            log.warn("Solde insuffisant pour le retrait: Compte={}, Solde={}, Montant demandé={}",
                    compteClient.getNumeroCompte(), compteClient.getSolde(), montant);
            throw new SoldeInsuffisantException(
                    String.format(ErrorMessages.INSUFFICIENT_FUNDS,
                            compteClient.getSolde(), montant)
            );
        }

        if (montant > collecteur.getMontantMaxRetrait()) {
            log.warn("Montant de retrait supérieur au maximum autorisé: Montant={}, Maximum={}",
                    montant, collecteur.getMontantMaxRetrait());
            throw new MontantMaxRetraitException(
                    String.format("Le montant du retrait (%s) dépasse le montant maximal autorisé (%s)",
                            montant, collecteur.getMontantMaxRetrait())
            );
        }

        log.debug("Validation du retrait réussie");
    }

    @Override
    public double calculerMontantCommission(Mouvement mouvement) {
        double montant = mouvement.getMontant();
        log.debug("Calcul du montant de commission pour mouvement ID={}, Montant={}, Type={}",
                mouvement.getId(), montant, mouvement.getSens());

        // S'il s'agit d'une épargne
        if ("epargne".equals(mouvement.getSens())) {
            double commission = montant * 0.02;
            log.debug("Commission pour épargne: {}% de {} = {}", 2, montant, commission);
            return commission;
        }

        // Pour les retraits
        if ("retrait".equals(mouvement.getSens())) {
            double commission = montant * 0.01;
            log.debug("Commission pour retrait: {}% de {} = {}", 1, montant, commission);
            return commission;
        }

        // Pour d'autres types d'opérations, aucune commission
        log.debug("Pas de commission pour opération de type: {}", mouvement.getSens());
        return 0.0;
    }

    private Mouvement creerMouvementRetrait(
            Compte source,
            Compte destination,
            double montant,
            Client client,
            Journal journal) {

        log.debug("Création mouvement retrait: Client={}, Montant={}, Source={}, Destination={}",
                client.getNom() + " " + client.getPrenom(), montant, source.getNumeroCompte(), destination.getNumeroCompte());

        Mouvement mouvement = new Mouvement();
        mouvement.setMontant(montant);
        mouvement.setLibelle(String.format("Retrait client : %s %s", client.getNom(), client.getPrenom()));
        mouvement.setSens("retrait");
        // ✅ UTILISATION DU DateTimeService
        mouvement.setDateOperation(dateTimeService.getCurrentDateTime());
        mouvement.setCompteSource(source);
        mouvement.setCompteDestination(destination);
        mouvement.setJournal(journal);
        return mouvement;
    }

    private Mouvement creerMouvementVersement(
            Compte source,
            Compte destination,
            double montant,
            Collecteur collecteur,
            Journal journal) {

        log.debug("Création mouvement versement: Collecteur={}, Montant={}, Source={}, Destination={}",
                collecteur.getNom() + " " + collecteur.getPrenom(), montant, source.getNumeroCompte(), destination.getNumeroCompte());

        Mouvement mouvement = new Mouvement();
        mouvement.setMontant(montant);
        mouvement.setLibelle(String.format("Versement en agence pour collecteur : %s", collecteur.getNom()));
        mouvement.setSens("versement");
        // ✅ UTILISATION DU DateTimeService
        mouvement.setDateOperation(dateTimeService.getCurrentDateTime());
        mouvement.setCompteSource(source);
        mouvement.setCompteDestination(destination);
        mouvement.setJournal(journal);
        return mouvement;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mouvement> findByJournalId(Long journalId) {
        log.debug("Récupération des mouvements pour le journal: {}", journalId);
        return mouvementRepository.findByJournalId(journalId);
    }

    /**
     * Méthode optimisée utilisant les projections
     */
    @Override
    @Transactional(readOnly = true)
    public List<MouvementCommissionDTO> findMouvementsDtoByJournalId(Long journalId) {
        List<MouvementProjection> projections = mouvementRepository.findMouvementProjectionsByJournalId(journalId);
        return projections.stream()
                .map(mouvementMapper::projectionToDto)
                .collect(Collectors.toList());
    }

    /**
     * Méthode avec JOIN FETCH (plus flexible)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Mouvement> findByJournalIdWithAccounts(Long journalId) {
        return mouvementRepository.findByJournalIdWithAccounts(journalId);
    }

    /**
     * Conversion des entités en DTO
     */
    @Override
    public List<MouvementCommissionDTO> convertToDto(List<Mouvement> mouvements) {
        return mouvements.stream()
                .map(mouvementMapper::toCommissionDto)
                .collect(Collectors.toList());
    }

    /**
     * Trouver les mouvements par collecteur et date
     */
    @Override
    public Page<Mouvement> findByCollecteurAndDate(Long collecteurId, String date, Pageable pageable) {
        log.info("Recherche des mouvements pour collecteur {} à la date {}", collecteurId, date);

        try {
            LocalDate localDate = LocalDate.parse(date);
            // ✅ UTILISATION DU DateTimeService
            LocalDateTime startOfDay = dateTimeService.toStartOfDay(localDate);
            LocalDateTime endOfDay = dateTimeService.toEndOfDay(localDate);

            return mouvementRepository.findByCollecteurAndDate(
                    collecteurId, startOfDay, endOfDay, pageable);
        } catch (Exception e) {
            log.error("Erreur lors de la recherche des mouvements", e);
            throw new BusinessException("Erreur lors de la recherche des mouvements: " + e.getMessage(),
                    "SEARCH_ERROR", e.getMessage());
        }
    }

    /**
     * Vérifier le solde d'un client pour un retrait
     */
    public BalanceVerificationDTO verifyClientBalance(Long clientId, Double montant) {
        log.info("Vérification du solde pour client {} montant {}", clientId, montant);

        try {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

            // Récupérer le compte épargne du client
            CompteClient compteClient = compteClientRepository.findByClient(client)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte client non trouvé"));

            Double soldeDisponible = compteClient.getSolde();
            Boolean sufficient = soldeDisponible >= montant;
            Double soldeApresOperation = sufficient ? soldeDisponible - montant : soldeDisponible;

            String message = sufficient
                    ? "Solde suffisant pour effectuer l'opération"
                    : String.format("Solde insuffisant. Solde disponible: %.2f FCFA, Montant demandé: %.2f FCFA",
                    soldeDisponible, montant);

            return BalanceVerificationDTO.builder()
                    .sufficient(sufficient)
                    .soldeDisponible(soldeDisponible)
                    .montantDemande(montant)
                    .soldeApresOperation(soldeApresOperation)
                    .message(message)
                    .clientNom(client.getNom())
                    .clientPrenom(client.getPrenom())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la vérification du solde", e);
            throw new BusinessException("Erreur lors de la vérification du solde: " + e.getMessage());
        }
    }
}