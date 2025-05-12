package org.example.collectfocep.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CompteRepository;
import org.example.collectfocep.services.impl.MouvementService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
public class CommissionRepartitionService {
    @Value("${commission.nouveauCollecteur.duree:3}")
    private int DUREE_NOUVEAU_COLLECTEUR; // durée en mois

    private final CompteService compteService;
    private final MouvementService mouvementService;
    private final CommissionCalculationService commissionCalculationService;
    private final MouvementRepository mouvementRepository;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final CompteRepository compteRepository;

    @Autowired
    public CommissionRepartitionService(
            CompteService compteService,
            MouvementService mouvementService,
            CommissionCalculationService commissionCalculationService,
            MouvementRepository mouvementRepository,
            CollecteurRepository collecteurRepository,
            CompteRepository compteRepository,
            ClientRepository clientRepository) {
        this.compteService = compteService;
        this.mouvementService = mouvementService;
        this.commissionCalculationService = commissionCalculationService;
        this.mouvementRepository = mouvementRepository;
        this.collecteurRepository = collecteurRepository;
        this.compteRepository = compteRepository;
        this.clientRepository = clientRepository;
    }

    // Cette méthode effectue d'abord le prélèvement des commissions sur les clients
    public void preleverCommissions(Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin) {
        List<Client> clients = collecteur.getClients();
        double totalCommissionsPrelevees = 0;
        double totalTVAPrelevee = 0;

        for (Client client : clients) {
            // Calculer le montant collecté pour ce client sur la période
            double montantCollecte = getMontantCollecte(client, dateDebut, dateFin);

            if (montantCollecte > 0) {
                // Prélever la commission et la TVA directement sur le compte client
                CommissionResult result = commissionCalculationService
                        .calculateCommissionForClient(client, dateDebut, dateFin, montantCollecte);

                totalCommissionsPrelevees += result.getMontantCommission();
                totalTVAPrelevee += result.getMontantTVA();

                log.info("Commission prélevée pour client {}: commission={}, TVA={}",
                        client.getId(), result.getMontantCommission(), result.getMontantTVA());
            }
        }

        log.info("Total des commissions prélevées pour collecteur {}: commissions={}, TVA={}",
                collecteur.getId(), totalCommissionsPrelevees, totalTVAPrelevee);
    }

    // Cette méthode répartit les commissions déjà prélevées
    public void repartirCommissions(Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin) {
        log.info("Début de la répartition des commissions pour collecteur {} du {} au {}",
                collecteur.getId(), dateDebut, dateFin);

        // 1. Récupération des comptes nécessaires
        CompteCollecteur compteServiceCollecteur = compteService.findServiceAccount(collecteur);
        CompteCollecteur compteAttente = compteService.findWaitingAccount(collecteur);
        CompteCollecteur compteRemunerationCollecteur = compteService.findSalaryAccount(collecteur);
        CompteLiaison compteLiaison = compteService.findLiaisonAccount(collecteur.getAgence());

        // 2. Calcul des totaux des commissions déjà prélevées
        CommissionTotals totals = calculerTotaux(collecteur, dateDebut, dateFin);

        log.info("Commissions totales pour collecteur {}: {}",
                collecteur.getId(), totals.getTotalCommissions());

        // 3. Traitement selon le cas (nouveau collecteur ou expérimenté)
        if (collecteur.getAncienneteEnMois() <= DUREE_NOUVEAU_COLLECTEUR) {
            traiterNouveauCollecteur(collecteur, compteAttente, compteRemunerationCollecteur, totals);
        } else {
            traiterCollecteurExperimente(collecteur, compteAttente, compteRemunerationCollecteur, totals);
        }

        // 4. Finalize les mouvements
        enregistrerMouvements(collecteur, compteAttente, compteLiaison, totals);

        log.info("Répartition des commissions terminée pour collecteur {}", collecteur.getId());
    }

    private CommissionTotals calculerTotaux(Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin) {
        CommissionTotals totals = new CommissionTotals();

        // Calculer la somme des commissions déjà prélevées qui sont dans le compte d'attente
        // Utiliser directement le solde du compte d'attente ou une requête de somme sur les mouvements
        Compte compteAttente = compteService.findWaitingAccount(collecteur);

        // On peut également utiliser une requête pour calculer le total des commissions
        double totalCommissions = mouvementRepository.sumCommissionsByCollecteurAndPeriod(
                collecteur.getId(),
                dateDebut.atStartOfDay(),
                dateFin.plusDays(1).atStartOfDay().minusSeconds(1)
        );

        // Calculer la TVA déjà prélevée (qui est dans le compte taxe)
        double totalTVA = mouvementRepository.sumTVAByCollecteurAndPeriod(
                collecteur.getId(),
                dateDebut.atStartOfDay(),
                dateFin.plusDays(1).atStartOfDay().minusSeconds(1)
        );

        totals.setTotalCommissions(totalCommissions);
        totals.setTotalTVAClient(totalTVA);

        // Calcul de la rémunération selon l'ancienneté du collecteur
        totals.setMontantRemuneration(calculerRemuneration(collecteur, totalCommissions));

        return totals;
    }


    // Méthode pour traiter la répartition basée sur un résultat de calcul
    public void processRepartition(CommissionResult result) {
        try {
            // Récupérer le collecteur
            Collecteur collecteur = collecteurRepository.findById(result.getCollecteurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // Déterminer les dates de début et fin (peut être extrait du résultat ou calculé)
            LocalDate startDate = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);

            // Effectuer la répartition
            repartirCommissions(collecteur, startDate, endDate);

        } catch (Exception e) {
            log.error("Erreur lors de la répartition des commissions: {}", e.getMessage(), e);
            throw new BusinessException("Erreur de répartition", "REPARTITION_ERROR", e.getMessage());
        }
    }

    private double getMontantCollecte(Client client, LocalDate dateDebut, LocalDate dateFin) {
        // Implémentation pour calculer le montant collecté pour un client sur une période
        return mouvementRepository.sumAmountByClientAndPeriod(
                client.getId(),
                dateDebut.atStartOfDay(),
                dateFin.plusDays(1).atStartOfDay().minusSeconds(1)
        );
    }

    private double calculerRemuneration(Collecteur collecteur, double totalCommissions) {
        // Si c'est un nouveau collecteur (moins de 3 mois)
        if (collecteur.getAncienneteEnMois() <= DUREE_NOUVEAU_COLLECTEUR) {
            return 40000.0; // Montant fixe pour nouveaux collecteurs
        } else {
            // 70% des commissions pour collecteurs expérimentés
            return totalCommissions * 0.70;
        }
    }



    @Data
    private class CommissionTotals {
        private double totalCommissions;
        private double totalTVAClient;
        private double montantRemuneration;
        private double montantTVAEMF;
        private double montantEMF;
    }


    private void traiterNouveauCollecteur(Collecteur collecteur,
                                          CompteCollecteur compteAttente,
                                          CompteCollecteur compteRemunerationCollecteur,
                                          CommissionTotals totals) {
        // Montant fixe pour nouveau collecteur
        double montantFixe = 40000.0; // FCFA

        // Création du mouvement de transfert
        Mouvement mouvement = new Mouvement();
        mouvement.setMontant(montantFixe);
        mouvement.setCompteSource(compteAttente);
        mouvement.setCompteDestination(compteRemunerationCollecteur);
        mouvement.setLibelle("Rémunération nouveau collecteur - " + collecteur.getNom());
        mouvement.setDateOperation(LocalDateTime.now());
        mouvement.setSens("DEBIT");

        mouvementService.effectuerMouvement(mouvement);
    }

    private void traiterCollecteurExperimente(Collecteur collecteur,
                                              CompteCollecteur compteAttente,
                                              CompteCollecteur compteRemunerationCollecteur,
                                              CommissionTotals totals) {
        // 70% pour le collecteur
        double partCollecteur = totals.getTotalCommissions() * 0.70;

        // Création du mouvement pour la part du collecteur
        Mouvement mouvementCollecteur = new Mouvement();
        mouvementCollecteur.setMontant(partCollecteur);
        mouvementCollecteur.setCompteSource(compteAttente);
        mouvementCollecteur.setCompteDestination(compteRemunerationCollecteur);
        mouvementCollecteur.setLibelle("Rémunération collecteur expérimenté - " + collecteur.getNom());
        mouvementCollecteur.setDateOperation(LocalDateTime.now());
        mouvementCollecteur.setSens("DEBIT");

        mouvementService.effectuerMouvement(mouvementCollecteur);

        // Traitement de la part EMF et TVA
        traiterPartEMF(collecteur, compteAttente, totals);
    }

    private void traiterPartEMF(Collecteur collecteur,
                                CompteCollecteur compteAttente,
                                CommissionTotals totals) {
        // 30% pour l'EMF
        double partEMF = totals.getTotalCommissions() * 0.30;
        double tvaEMF = partEMF * 0.1925; // 19.25% TVA

        Compte compteProduitEMF = compteService.findProduitAccount();
        Compte compteTVA = compteService.findTVAAccount();

        // Mouvement pour la part EMF
        Mouvement mouvementEMF = new Mouvement();
        mouvementEMF.setMontant(partEMF);
        mouvementEMF.setCompteSource(compteAttente);
        mouvementEMF.setCompteDestination(compteProduitEMF);
        mouvementEMF.setLibelle("Part EMF commission - Collecteur " + collecteur.getNom());
        mouvementEMF.setDateOperation(LocalDateTime.now());
        mouvementEMF.setSens("DEBIT");

        // Mouvement pour la TVA EMF
        Mouvement mouvementTVA = new Mouvement();
        mouvementTVA.setMontant(tvaEMF);
        mouvementTVA.setCompteSource(compteAttente);
        mouvementTVA.setCompteDestination(compteTVA);
        mouvementTVA.setLibelle("TVA sur part EMF - Collecteur " + collecteur.getNom());
        mouvementTVA.setDateOperation(LocalDateTime.now());
        mouvementTVA.setSens("DEBIT");

        mouvementService.effectuerMouvement(mouvementEMF);
        mouvementService.effectuerMouvement(mouvementTVA);
    }

    private void enregistrerMouvements(Collecteur collecteur,
                                       CompteCollecteur compteAttente,
                                       CompteLiaison compteLiaison,
                                       CommissionTotals totals) {
        // Création des mouvements de liaison
        Mouvement mouvementLiaison = new Mouvement();
        mouvementLiaison.setMontant(totals.getTotalCommissions());
        mouvementLiaison.setCompteSource(compteAttente);
        mouvementLiaison.setCompteDestination(compteLiaison);
        mouvementLiaison.setLibelle("Transfert commissions vers liaison - Collecteur " + collecteur.getNom());
        mouvementLiaison.setDateOperation(LocalDateTime.now());
        mouvementLiaison.setSens("DEBIT");

        mouvementService.effectuerMouvement(mouvementLiaison);
    }
}