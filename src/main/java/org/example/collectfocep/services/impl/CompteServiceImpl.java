package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.interfaces.CompteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CompteServiceImpl implements CompteService {

    @PersistenceContext
    private EntityManager entityManager;

    private final CompteRepository compteRepository;
    private final CompteCollecteurRepository compteCollecteurRepository;
    private final CompteLiaisonRepository compteLiaisonRepository;
    private final CompteServiceRepository compteServiceRepository;
    private final CompteManquantRepository compteManquantRepository;
    private final CompteRemunerationRepository compteRemunerationRepository;
    private final CompteAttenteRepository compteAttenteRepository;
    private final CompteChargeRepository compteChargeRepository;
    private final CollecteurRepository collecteurRepository;
    private final CompteClientRepository compteClientRepository;
    private final ClientRepository clientRepository;
    private final AgenceRepository agenceRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void createCollecteurAccounts(Collecteur collecteur) {
        log.info("Début de la création des comptes pour le collecteur ID: {}", collecteur.getId());

        // Vérifier que le collecteur est persisté
        if (collecteur.getId() == null) {
            throw new IllegalStateException("Le collecteur doit être persisté avant de créer ses comptes");
        }

        // Récupérer l'agence avec son code
        Agence agence = agenceRepository.findById(collecteur.getAgence().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée"));

        String codeAgence = agence.getCodeAgence() != null ? agence.getCodeAgence() : "001";
        String collecteurIdFormatted = String.format("%08d", collecteur.getId());

        // Créer chaque type de compte avec gestion correcte des versions
        createCompteService(collecteur, codeAgence, collecteurIdFormatted);
        createCompteManquant(collecteur, codeAgence, collecteurIdFormatted);
        createCompteRemuneration(collecteur, codeAgence, collecteurIdFormatted);
        createCompteAttente(collecteur, codeAgence, collecteurIdFormatted);
        createCompteCharge(collecteur, codeAgence, collecteurIdFormatted);

        // Force la synchronisation avec la base
        entityManager.flush();

        log.info("Fin de la création des comptes pour le collecteur: {}", collecteur.getId());
    }

    private void createCompteService(Collecteur collecteur, String codeAgence, String collecteurIdFormatted) {
        if (!compteServiceRepository.existsByCollecteur(collecteur)) {
            CompteServiceEntity compteService = CompteServiceEntity.builder()
                    .collecteur(collecteur)
                    .typeCompte("SERVICE")
                    .nomCompte("Compte Principal Collecte " + collecteur.getNom())
                    .numeroCompte("373" + codeAgence + collecteurIdFormatted)
                    .solde(0.0)
                    .version(0L)  // Initialiser la version
                    .build();

            CompteServiceEntity saved = compteServiceRepository.saveAndFlush(compteService);
            entityManager.refresh(saved); // Refresh pour récupérer la version générée
            log.info("Compte SERVICE créé pour le collecteur: {}", collecteur.getId());
        }
    }

    private void createCompteManquant(Collecteur collecteur, String codeAgence, String collecteurIdFormatted) {
        if (!compteManquantRepository.existsByCollecteur(collecteur)) {
            CompteManquant compteManquant = CompteManquant.builder()
                    .collecteur(collecteur)
                    .typeCompte("MANQUANT")
                    .nomCompte("Compte Manquant " + collecteur.getNom())
                    .numeroCompte("374" + codeAgence + collecteurIdFormatted)
                    .solde(0.0)
                    .version(0L)
                    .build();

            CompteManquant saved = compteManquantRepository.saveAndFlush(compteManquant);
            entityManager.refresh(saved);
            log.info("Compte MANQUANT créé pour le collecteur: {}", collecteur.getId());
        }
    }

    private void createCompteRemuneration(Collecteur collecteur, String codeAgence, String collecteurIdFormatted) {
        if (!compteRemunerationRepository.existsByCollecteur(collecteur)) {
            CompteRemuneration compteRemuneration = CompteRemuneration.builder()
                    .collecteur(collecteur)
                    .typeCompte("REMUNERATION")
                    .nomCompte("Compte Rémunération " + collecteur.getNom())
                    .numeroCompte("375" + codeAgence + collecteurIdFormatted)
                    .solde(0.0)
                    .version(0L)
                    .build();

            CompteRemuneration saved = compteRemunerationRepository.saveAndFlush(compteRemuneration);
            entityManager.refresh(saved);
            log.info("Compte REMUNERATION créé pour le collecteur: {}", collecteur.getId());
        }
    }

    private void createCompteAttente(Collecteur collecteur, String codeAgence, String collecteurIdFormatted) {
        if (!compteAttenteRepository.existsByCollecteur(collecteur)) {
            CompteAttente compteAttente = CompteAttente.builder()
                    .collecteur(collecteur)
                    .typeCompte("ATTENTE")
                    .nomCompte("Compte Attente " + collecteur.getNom())
                    .numeroCompte("ATT" + codeAgence + collecteurIdFormatted)
                    .solde(0.0)
                    .version(0L)
                    .build();

            CompteAttente saved = compteAttenteRepository.saveAndFlush(compteAttente);
            entityManager.refresh(saved);
            log.info("Compte ATTENTE créé pour le collecteur: {}", collecteur.getId());
        }
    }

    private void createCompteCharge(Collecteur collecteur, String codeAgence, String collecteurIdFormatted) {
        if (!compteChargeRepository.existsByCollecteur(collecteur)) {
            CompteCharge compteCharge = CompteCharge.builder()
                    .collecteur(collecteur)
                    .typeCompte("CHARGE")
                    .nomCompte("Compte Charge " + collecteur.getNom())
                    .numeroCompte("376" + codeAgence + collecteurIdFormatted)
                    .solde(0.0)
                    .version(0L)
                    .build();

            CompteCharge saved = compteChargeRepository.saveAndFlush(compteCharge);
            entityManager.refresh(saved);
            log.info("Compte CHARGE créé pour le collecteur: {}", collecteur.getId());
        }
    }

    // Méthodes existantes inchangées mais avec amélioration de la gestion des erreurs
    @Override
    public CompteCollecteur findServiceAccount(Collecteur collecteur) {
        log.debug("Recherche du compte service pour collecteur ID={}", collecteur.getId());

        // 1. Utiliser la méthode correcte du repository
        // Chercher d'abord dans CompteServiceEntity
        Optional<CompteServiceEntity> compteService = compteServiceRepository
                .findFirstByCollecteur(collecteur);

        if (compteService.isPresent()) {
            log.debug("CompteServiceEntity trouvé, conversion en CompteCollecteur");
            return createCompteCollecteurFromService(compteService.get(), collecteur);
        }

        // 2. Si pas trouvé, essayer CompteCollecteur (si vous gardez l'architecture hybride)
        // Note: Cette méthode nécessiterait d'ajouter une méthode dans CompteCollecteurRepository
        // Pour l'instant, on lève une exception
        log.error("Aucun compte service trouvé pour collecteur ID={}", collecteur.getId());
        throw new ResourceNotFoundException("Compte service non trouvé pour le collecteur: " + collecteur.getId());
    }

    /**
     * Créer un CompteCollecteur à partir d'un CompteServiceEntity
     * sans modifier la base de données
     */
    private CompteCollecteur createCompteCollecteurFromService(CompteServiceEntity serviceEntity, Collecteur collecteur) {
        CompteCollecteur compteCollecteur = new CompteCollecteur();
        compteCollecteur.setId(serviceEntity.getId());
        compteCollecteur.setCollecteur(collecteur);
        compteCollecteur.setNomCompte(serviceEntity.getNomCompte());
        compteCollecteur.setNumeroCompte(serviceEntity.getNumeroCompte());
        compteCollecteur.setSolde(serviceEntity.getSolde());
        compteCollecteur.setTypeCompte("SERVICE");
        compteCollecteur.setVersion(serviceEntity.getVersion());
        return compteCollecteur;
    }


    @Override
    public Page<Compte> getAllComptes(Pageable pageable) {
        log.debug("Récupération paginée de tous les comptes");
        return compteRepository.findAll(pageable);
    }

    @Override
    public List<Compte> getAllComptes() {
        log.debug("Récupération de tous les comptes");
        return compteRepository.findAll();
    }

    @Override
    public CompteCollecteur findWaitingAccount(Collecteur collecteur) {
        // Utiliser CompteAttenteRepository directement
        Optional<CompteAttente> compteAttente = compteAttenteRepository.findFirstByCollecteur(collecteur);

        if (compteAttente.isPresent()) {
            return convertToCompteCollecteur(compteAttente.get(), collecteur);
        }

        throw new ResourceNotFoundException("Compte d'attente non trouvé pour le collecteur: " + collecteur.getId());
    }

    @Override
    public CompteCollecteur findSalaryAccount(Collecteur collecteur) {
        // Utiliser CompteRemunerationRepository directement
        Optional<CompteRemuneration> compteRemuneration = compteRemunerationRepository.findFirstByCollecteur(collecteur);

        if (compteRemuneration.isPresent()) {
            return convertToCompteCollecteur(compteRemuneration.get(), collecteur);
        }

        throw new ResourceNotFoundException("Compte salaire non trouvé pour le collecteur: " + collecteur.getId());
    }

    @Override
    public CompteLiaison findLiaisonAccount(Agence agence) {
        return compteLiaisonRepository.findByAgenceAndTypeCompte(agence, "LIAISON")
                .orElseThrow(() -> new ResourceNotFoundException("Compte de liaison non trouvé pour l'agence: " + agence.getId()));
    }

    @Override
    public Compte findProduitAccount() {
        return compteRepository.findByTypeCompte("PRODUIT")
                .orElseThrow(() -> new ResourceNotFoundException("Compte produit non trouvé"));
    }

    @Override
    public Compte findTVAAccount() {
        return compteRepository.findByTypeCompte("TVA")
                .orElseThrow(() -> new ResourceNotFoundException("Compte TVA non trouvé"));
    }

    @Override
    public Compte findChargeAccount(Collecteur collecteur) {
        Optional<CompteCharge> compteCharge = compteChargeRepository.findFirstByCollecteur(collecteur);

        if (compteCharge.isPresent()) {
            return compteCharge.get();
        }

        return compteRepository.findByTypeCompteAndCollecteurId("CHARGE", collecteur.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte de charge non trouvé"));
    }

    @Override
    public Optional<Compte> findByTypeCompte(String typeCompte) {
        return compteRepository.findByTypeCompte(typeCompte);
    }

    @Override
    public List<Compte> findByAgenceId(Long agenceId) {
        return compteRepository.findByAgenceId(agenceId);
    }

    @Override
    public List<Compte> findByCollecteurId(Long collecteurId) {
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        List<Compte> comptes = new ArrayList<>();
        comptes.addAll(compteCollecteurRepository.findByCollecteur(collecteur));
        comptes.addAll(compteServiceRepository.findAllByCollecteur(collecteur));
        comptes.addAll(compteManquantRepository.findAllByCollecteur(collecteur));
        comptes.addAll(compteRemunerationRepository.findAllByCollecteur(collecteur));
        comptes.addAll(compteAttenteRepository.findAllByCollecteur(collecteur));
        comptes.addAll(compteChargeRepository.findAllByCollecteur(collecteur));

        return comptes;
    }

    @Override
    public Page<Compte> findByCollecteurId(Long collecteurId, Pageable pageable) {
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        List<Compte> comptes = new ArrayList<>();
        comptes.addAll(compteCollecteurRepository.findByCollecteur(collecteur));
        compteServiceRepository.findAllByCollecteur(collecteur).forEach(comptes::add);
        comptes.addAll(compteManquantRepository.findAllByCollecteur(collecteur));
        comptes.addAll(compteRemunerationRepository.findAllByCollecteur(collecteur));
        comptes.addAll(compteAttenteRepository.findAllByCollecteur(collecteur));
        comptes.addAll(compteChargeRepository.findAllByCollecteur(collecteur));

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), comptes.size());

        if (start > comptes.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, comptes.size());
        }

        return new PageImpl<>(comptes.subList(start, end), pageable, comptes.size());
    }

    @Override
    public Page<Compte> findByAgenceId(Long agenceId, Pageable pageable) {
        List<Compte> comptes = compteRepository.findByAgenceId(agenceId);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), comptes.size());

        if (start > comptes.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, comptes.size());
        }

        return new PageImpl<>(comptes.subList(start, end), pageable, comptes.size());
    }

    private CompteCollecteur convertToCompteCollecteur(Compte compte, Collecteur collecteur) {
        if (compte == null) return null;

        CompteCollecteur compteCollecteur = new CompteCollecteur();
        compteCollecteur.setId(compte.getId());
        compteCollecteur.setCollecteur(collecteur);
        compteCollecteur.setNomCompte(compte.getNomCompte());
        compteCollecteur.setNumeroCompte(compte.getNumeroCompte());
        compteCollecteur.setSolde(compte.getSolde());
        compteCollecteur.setTypeCompte(compte.getTypeCompte());
        compteCollecteur.setVersion(compte.getVersion());
        return compteCollecteur;
    }

    @Override
    @Transactional
    public void deleteCompte(Long id) {
        log.info("Suppression du compte avec l'ID: {}", id);

        Compte compte = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé avec l'ID: " + id));

        if (compte.getSolde() != 0) {
            throw new IllegalStateException("Impossible de supprimer un compte avec un solde non nul: " + compte.getSolde());
        }

        compteRepository.deleteById(id);
        log.info("Compte {} supprimé avec succès", id);
    }

    @Override
    @Transactional
    public Compte saveCompte(Compte compte) {
        log.info("Sauvegarde du compte: {}", compte.getId() != null ? compte.getId() : "nouveau compte");

        if (compte.getNomCompte() == null || compte.getNomCompte().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du compte ne peut pas être vide");
        }

        if (compte.getNumeroCompte() == null || compte.getNumeroCompte().trim().isEmpty()) {
            String prefixe = compte.getTypeCompte().substring(0, Math.min(3, compte.getTypeCompte().length())).toUpperCase();
            String numeroGenere = prefixe + "-" + System.currentTimeMillis();
            compte.setNumeroCompte(numeroGenere);
            log.info("Génération d'un nouveau numéro de compte: {}", numeroGenere);
        }

        if (compte.getId() != null) {
            compteRepository.findAll().stream()
                    .filter(c -> c.getNumeroCompte().equals(compte.getNumeroCompte()))
                    .filter(c -> !c.getId().equals(compte.getId()))
                    .findAny()
                    .ifPresent(c -> {
                        throw new IllegalStateException("Un compte avec le numéro " + compte.getNumeroCompte() + " existe déjà");
                    });
        }

        Compte compteSauvegarde = compteRepository.save(compte);
        log.info("Compte sauvegardé avec succès: {}", compteSauvegarde.getId());

        return compteSauvegarde;
    }

    @Override
    public Optional<Compte> getCompteById(Long id) {
        log.debug("Récupération du compte avec l'ID: {}", id);
        return compteRepository.findById(id);
    }

    @Override
    public List<Compte> findByClientId(Long clientId) {
        log.debug("Récupération des comptes pour le client ID: {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));

        List<Compte> comptes = new ArrayList<>();
        comptes.addAll(compteClientRepository.findAllByClient(client));

        return comptes;
    }

    @Override
    public CompteManquant findManquantAccount(Collecteur collecteur) {
        log.debug("Recherche du compte manquant pour collecteur ID={}", collecteur.getId());

        return compteManquantRepository.findFirstByCollecteur(collecteur)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte manquant non trouvé pour le collecteur: " + collecteur.getId()));
    }

    @Override
    public CompteAttente findAttenteAccount(Collecteur collecteur) {
        log.debug("Recherche du compte attente pour collecteur ID={}", collecteur.getId());

        return compteAttenteRepository.findFirstByCollecteur(collecteur)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte attente non trouvé pour le collecteur: " + collecteur.getId()));
    }

    @Override
    public CompteRemuneration findRemunerationAccount(Collecteur collecteur) {
        log.debug("Recherche du compte rémunération pour collecteur ID={}", collecteur.getId());

        return compteRemunerationRepository.findFirstByCollecteur(collecteur)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte rémunération non trouvé pour le collecteur: " + collecteur.getId()));
    }


    @Override
    @Transactional
    public void ajouterAuSolde(Long compteId, Double montant) {
        log.info("Ajout de {} au solde du compte {}", montant, compteId);

        // Récupérer le solde actuel et calculer le nouveau
        Double soldeActuel = getSolde(compteId);
        Double nouveauSolde = soldeActuel + montant;
        updateSoldeCompte(compteId, nouveauSolde);

        log.info("✅ Montant ajouté: nouveau solde {} pour compte {}", nouveauSolde, compteId);
    }

    @Override
    @Transactional
    public void retirerDuSolde(Long compteId, Double montant) {
        log.info("Retrait de {} du solde du compte {}", montant, compteId);

        // Récupérer le solde actuel et calculer le nouveau
        Double soldeActuel = getSolde(compteId);
        Double nouveauSolde = soldeActuel - montant;

        // Vérification du solde négatif selon le type de compte
        if (nouveauSolde < 0 && !isNegativeBalanceAllowedForId(compteId)) {
            throw new IllegalArgumentException("Solde insuffisant pour le compte: " + compteId);
        }

        updateSoldeCompte(compteId, nouveauSolde);
        log.info("✅ Montant retiré: nouveau solde {} pour compte {}", nouveauSolde, compteId);
    }

    private boolean isNegativeBalanceAllowedForId(Long compteId) {
        // Vérifier dans CompteManquant (autorise négatif)
        if (compteManquantRepository.findById(compteId).isPresent()) {
            return true;
        }

        // Vérifier dans CompteCharge (autorise négatif)
        if (compteChargeRepository.findById(compteId).isPresent()) {
            return true;
        }

        // Par défaut, pas de solde négatif autorisé
        return false;
    }



    @Override
    @Transactional
    public void transfererEntreComptes(Long compteSourceId, Long compteDestinationId,
                                       Double montant, String motif) {
        log.info("Transfert de {} de compte {} vers compte {} - Motif: {}",
                montant, compteSourceId, compteDestinationId, motif);

        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        try {
            // Débiter le compte source
            retirerDuSolde(compteSourceId, montant);

            // Créditer le compte destination
            ajouterAuSolde(compteDestinationId, montant);

            log.info("✅ Transfert réussi: {} de {} vers {}", montant, compteSourceId, compteDestinationId);

        } catch (Exception e) {
            log.error("❌ Erreur lors du transfert: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors du transfert: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void remettreAZero(Long compteId) {
        log.info("Remise à zéro du compte {}", compteId);
        updateSoldeCompte(compteId, 0.0);
    }

    @Override
    public boolean hasCompteService(Collecteur collecteur) {
        return compteServiceRepository.existsByCollecteur(collecteur);
    }

    @Override
    public boolean hasCompteManquant(Collecteur collecteur) {
        return compteManquantRepository.existsByCollecteur(collecteur);
    }

    @Override
    public boolean hasCompteAttente(Collecteur collecteur) {
        return compteAttenteRepository.existsByCollecteur(collecteur);
    }

    @Override
    public boolean hasCompteRemuneration(Collecteur collecteur) {
        return compteRemunerationRepository.existsByCollecteur(collecteur);
    }

    @Override
    public List<Compte> getAllComptesByCollecteur(Collecteur collecteur) {
        log.debug("Récupération de tous les comptes pour collecteur ID={}", collecteur.getId());

        List<Compte> comptes = new ArrayList<>();

        // Ajouter tous les types de comptes
        compteServiceRepository.findAllByCollecteur(collecteur).forEach(comptes::add);
        compteManquantRepository.findAllByCollecteur(collecteur).forEach(comptes::add);
        compteAttenteRepository.findAllByCollecteur(collecteur).forEach(comptes::add);
        compteRemunerationRepository.findAllByCollecteur(collecteur).forEach(comptes::add);
        compteChargeRepository.findAllByCollecteur(collecteur).forEach(comptes::add);

        return comptes;
    }

    @Override
    public Double calculateTotalSoldeByTypeAndAgence(String typeCompte, Long agenceId) {
        log.debug("Calcul total solde type {} pour agence {}", typeCompte, agenceId);

        // Cette méthode nécessiterait une requête personnalisée
        // Pour l'instant, retourner 0.0 ou implémenter selon vos besoins
        return 0.0;
    }

    @Override
    public List<Collecteur> getCollecteursWithNonZeroBalance(String typeCompte, Long agenceId) {
        log.debug("Recherche collecteurs avec solde non nul type {} agence {}", typeCompte, agenceId);

        // Implémentation selon le type de compte
        switch (typeCompte.toUpperCase()) {
            case "MANQUANT":
                return collecteurRepository.findByAgenceId(agenceId).stream()
                        .filter(c -> {
                            try {
                                CompteManquant compte = findManquantAccount(c);
                                return compte.getSolde() != 0;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

            case "ATTENTE":
                return collecteurRepository.findByAgenceId(agenceId).stream()
                        .filter(c -> {
                            try {
                                CompteAttente compte = findAttenteAccount(c);
                                return compte.getSolde() != 0;
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

            default:
                return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public void archiverAnciensMouvements(int nombreJours) {
        log.info("Archivage des mouvements de plus de {} jours", nombreJours);
        // Implémentation selon vos besoins d'archivage
        // Cette méthode pourrait déplacer les anciens mouvements vers une table d'archive
    }

    // Méthode utilitaire
    private boolean isNegativeBalanceAllowed(String typeCompte) {
        // Certains types de comptes peuvent avoir des soldes négatifs
        return "MANQUANT".equals(typeCompte) || "CHARGE".equals(typeCompte);
    }

    public CompteServiceEntity findServiceEntityAccount(Collecteur collecteur) {
        return compteServiceRepository.findFirstByCollecteur(collecteur)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte service non trouvé pour le collecteur: " + collecteur.getId()));
    }

    @Override
    @Transactional
    public void updateSoldeCompte(Long compteId, Double nouveauSolde) {
        log.info("Mise à jour solde compte {} vers {}", compteId, nouveauSolde);

        // Essayer de trouver le compte dans tous les repositories
        Optional<CompteServiceEntity> compteService = compteServiceRepository.findById(compteId);
        if (compteService.isPresent()) {
            CompteServiceEntity compte = compteService.get();
            Double ancienSolde = compte.getSolde();
            compte.setSolde(nouveauSolde);
            compteServiceRepository.save(compte);
            log.info("✅ Solde CompteService mis à jour: {} → {} pour compte {}", ancienSolde, nouveauSolde, compteId);
            return;
        }

        Optional<CompteManquant> compteManquant = compteManquantRepository.findById(compteId);
        if (compteManquant.isPresent()) {
            CompteManquant compte = compteManquant.get();
            Double ancienSolde = compte.getSolde();
            compte.setSolde(nouveauSolde);
            compteManquantRepository.save(compte);
            log.info("✅ Solde CompteManquant mis à jour: {} → {} pour compte {}", ancienSolde, nouveauSolde, compteId);
            return;
        }

        Optional<CompteAttente> compteAttente = compteAttenteRepository.findById(compteId);
        if (compteAttente.isPresent()) {
            CompteAttente compte = compteAttente.get();
            Double ancienSolde = compte.getSolde();
            compte.setSolde(nouveauSolde);
            compteAttenteRepository.save(compte);
            log.info("✅ Solde CompteAttente mis à jour: {} → {} pour compte {}", ancienSolde, nouveauSolde, compteId);
            return;
        }

        Optional<CompteRemuneration> compteRemuneration = compteRemunerationRepository.findById(compteId);
        if (compteRemuneration.isPresent()) {
            CompteRemuneration compte = compteRemuneration.get();
            Double ancienSolde = compte.getSolde();
            compte.setSolde(nouveauSolde);
            compteRemunerationRepository.save(compte);
            log.info("✅ Solde CompteRemuneration mis à jour: {} → {} pour compte {}", ancienSolde, nouveauSolde, compteId);
            return;
        }

        // Fallback: Utiliser le repository général
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + compteId));

        Double ancienSolde = compte.getSolde();
        compte.setSolde(nouveauSolde);
        compteRepository.save(compte);
        log.info("✅ Solde mis à jour (fallback): {} → {} pour compte {}", ancienSolde, nouveauSolde, compteId);
    }

    @Override
    public double getSolde(Long compteId) {
        // Essayer de trouver le compte dans tous les repositories
        Optional<CompteServiceEntity> compteService = compteServiceRepository.findById(compteId);
        if (compteService.isPresent()) {
            return compteService.get().getSolde();
        }

        Optional<CompteManquant> compteManquant = compteManquantRepository.findById(compteId);
        if (compteManquant.isPresent()) {
            return compteManquant.get().getSolde();
        }

        Optional<CompteAttente> compteAttente = compteAttenteRepository.findById(compteId);
        if (compteAttente.isPresent()) {
            return compteAttente.get().getSolde();
        }

        Optional<CompteRemuneration> compteRemuneration = compteRemunerationRepository.findById(compteId);
        if (compteRemuneration.isPresent()) {
            return compteRemuneration.get().getSolde();
        }

        // Fallback: Utiliser le repository général
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + compteId));
        return compte.getSolde();
    }

}