package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.impl.MouvementService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Moteur de distribution des commissions
 * Gère la répartition entre collecteur, EMF et taxes
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CommissionDistributionEngine {

    private final MouvementService mouvementService;
    private final CompteRepository compteRepository;
    private final CollecteurRepository collecteurRepository;
    private final CompteAttenteRepository compteAttenteRepository;
    private final CompteRemunerationRepository compteRemunerationRepository;
    private final CompteChargeRepository compteChargeRepository;

    /**
     * Distribue les commissions calculées selon les règles métier
     */
    @Transactional
    public CommissionDistribution distribute(List<CommissionCalculation> calculations, CommissionContext context) {
        log.info("Distribution des commissions - {} calculs à traiter", calculations.size());

        // Récupération du collecteur
        Collecteur collecteur = collecteurRepository.findById(context.getCollecteurId())
                .orElseThrow(() -> new RuntimeException("Collecteur non trouvé: " + context.getCollecteurId()));

        // Calcul des totaux
        CommissionTotals totals = calculateTotals(calculations, context.getRules());

        // Calcul de la rémunération du collecteur
        BigDecimal remuneration = calculateCollecteurRemuneration(collecteur, totals, context.getRules());

        // Création de la distribution
        var distribution = CommissionDistribution.builder()
                .collecteurId(context.getCollecteurId())
                .calculations(calculations)
                .totalCommissions(totals.getTotalCommissions())
                .totalTVA(totals.getTotalTVAClient())
                .remunerationCollecteur(remuneration)
                .partEMF(totals.getMontantEMF())
                .tvaEMF(totals.getMontantTVAEMF())
                .build();

        // Création des mouvements comptables
        createAccountingMovements(distribution, collecteur, context.getRules());

        log.info("Distribution terminée - Rémunération: {}, Part EMF: {}",
                remuneration, totals.getMontantEMF());

        return distribution;
    }

    /**
     * Persiste les mouvements comptables
     */
    @Transactional
    public void persistMovements(CommissionDistribution distribution) {
        log.info("Persistance de {} mouvements comptables", distribution.getMovements().size());

        for (Mouvement mouvement : distribution.getMovements()) {
            mouvementService.effectuerMouvement(mouvement);
        }
    }

    /**
     * Met à jour les soldes des comptes
     */
    @Transactional
    public void updateAccountBalances(CommissionDistribution distribution) {
        log.info("Mise à jour des soldes des comptes");

        // Les soldes sont mis à jour automatiquement par MouvementService
        // Cette méthode peut contenir des validations supplémentaires si nécessaire

        // Validation post-distribution
        validateDistributionIntegrity(distribution);
    }

    private CommissionTotals calculateTotals(List<CommissionCalculation> calculations, CommissionRules rules) {
        BigDecimal totalCommissions = calculations.stream()
                .map(CommissionCalculation::getCommissionBase)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTVA = calculations.stream()
                .map(CommissionCalculation::getTva)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcul de la part EMF
        BigDecimal partEMF = totalCommissions.multiply(rules.getEmfRate());
        BigDecimal tvaEMF = partEMF.multiply(rules.getTvaRate())
                .setScale(2, RoundingMode.HALF_UP);

        return CommissionTotals.builder()
                .totalCommissions(totalCommissions.doubleValue())
                .totalTVAClient(totalTVA.doubleValue())
                .montantEMF(partEMF.doubleValue())
                .montantTVAEMF(tvaEMF.doubleValue())
                .build();
    }

    private BigDecimal calculateCollecteurRemuneration(Collecteur collecteur, CommissionTotals totals, CommissionRules rules) {
        BigDecimal totalCommissions = BigDecimal.valueOf(totals.getTotalCommissions());

        // Nouveau collecteur (moins de 3 mois)
        if (rules.isNouveauCollecteur(collecteur.getAncienneteEnMois())) {
            log.info("Collecteur nouveau - Rémunération fixe: {}", rules.getNouveauCollecteurMontant());
            return rules.getNouveauCollecteurMontant();
        }

        // Collecteur expérimenté (70% des commissions)
        BigDecimal remuneration = totalCommissions.multiply(rules.getCollecteurRate())
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Collecteur expérimenté - Rémunération (70%): {}", remuneration);
        return remuneration;
    }

    private void createAccountingMovements(CommissionDistribution distribution, Collecteur collecteur, CommissionRules rules) {
        List<Mouvement> mouvements = new ArrayList<>();

        // Récupération des comptes
        CompteAttente compteAttente = findOrCreateCompteAttente(collecteur);
        CompteRemuneration compteRemuneration = findOrCreateCompteRemuneration(collecteur);

        BigDecimal totalCommissions = BigDecimal.valueOf(distribution.getTotalCommissions());
        BigDecimal remunerationCollecteur = distribution.getRemunerationCollecteur();

        // Cas 1: Rémunération <= Commissions (cas normal)
        if (remunerationCollecteur.compareTo(totalCommissions) <= 0) {
            // Transfert de la rémunération
            mouvements.add(createMouvement(
                    compteAttente, compteRemuneration, remunerationCollecteur,
                    "Rémunération collecteur - " + collecteur.getNom()
            ));

            // Calcul et transfert de la part EMF
            BigDecimal resteCommissions = totalCommissions.subtract(remunerationCollecteur);
            if (resteCommissions.compareTo(BigDecimal.ZERO) > 0) {
                createEMFMovements(mouvements, compteAttente, resteCommissions, rules);
            }
        }
        // Cas 2: Rémunération > Commissions (déficit)
        else {
            // Transfert de toutes les commissions disponibles
            if (totalCommissions.compareTo(BigDecimal.ZERO) > 0) {
                mouvements.add(createMouvement(
                        compteAttente, compteRemuneration, totalCommissions,
                        "Transfert commissions disponibles"
                ));
            }

            // Complément depuis le compte de charge
            BigDecimal deficit = remunerationCollecteur.subtract(totalCommissions);
            CompteCharge compteCharge = findOrCreateCompteCharge(collecteur);

            mouvements.add(createMouvement(
                    compteCharge, compteRemuneration, deficit,
                    "Complément rémunération depuis compte charge"
            ));
        }

        distribution.setMovements(mouvements);
    }

    private void createEMFMovements(List<Mouvement> mouvements, CompteAttente compteAttente,
                                    BigDecimal resteCommissions, CommissionRules rules) {

        // Part EMF (30%)
        BigDecimal partEMF = resteCommissions.multiply(rules.getEmfRate())
                .setScale(2, RoundingMode.HALF_UP);

        // TVA sur part EMF
        BigDecimal tvaEMF = partEMF.multiply(rules.getTvaRate())
                .setScale(2, RoundingMode.HALF_UP);

        // Comptes système
        Compte compteProduitEMF = findCompteProduitEMF();
        Compte compteTaxe = findCompteTaxe();

        // Mouvement vers produit EMF (net de TVA)
        BigDecimal produitEMFNet = partEMF.subtract(tvaEMF);
        mouvements.add(createMouvement(
                compteAttente, compteProduitEMF, produitEMFNet,
                "Part EMF (net de TVA)"
        ));

        // Mouvement TVA
        mouvements.add(createMouvement(
                compteAttente, compteTaxe, tvaEMF,
                "TVA sur part EMF"
        ));
    }

    private Mouvement createMouvement(Compte source, Compte destination, BigDecimal montant, String libelle) {
        return Mouvement.builder()
                .compteSource(source)
                .compteDestination(destination)
                .montant(montant.doubleValue())
                .libelle(libelle)
                .sens("DEBIT")
                .dateOperation(LocalDateTime.now())
                .build();
    }

    private void validateDistributionIntegrity(CommissionDistribution distribution) {
        // Validation que la somme des distributions égale le total
        BigDecimal totalDistribue = distribution.getRemunerationCollecteur()
                .add(BigDecimal.valueOf(distribution.getPartEMF()))
                .add(BigDecimal.valueOf(distribution.getTvaEMF()));

        BigDecimal totalCommissions = BigDecimal.valueOf(distribution.getTotalCommissions());

        // Tolérance pour les arrondis
        BigDecimal tolerance = BigDecimal.valueOf(0.01);
        BigDecimal difference = totalCommissions.subtract(totalDistribue).abs();

        if (difference.compareTo(tolerance) > 0) {
            log.warn("Différence détectée dans la distribution: {} FCFA", difference);
        }
    }

    // Méthodes utilitaires pour récupérer/créer les comptes
    private CompteAttente findOrCreateCompteAttente(Collecteur collecteur) {
        return compteAttenteRepository.findFirstByCollecteur(collecteur)
                .orElseThrow(() -> new RuntimeException("Compte d'attente non trouvé pour collecteur: " + collecteur.getId()));
    }

    private CompteRemuneration findOrCreateCompteRemuneration(Collecteur collecteur) {
        return compteRemunerationRepository.findFirstByCollecteur(collecteur)
                .orElseThrow(() -> new RuntimeException("Compte rémunération non trouvé pour collecteur: " + collecteur.getId()));
    }

    private CompteCharge findOrCreateCompteCharge(Collecteur collecteur) {
        return compteChargeRepository.findFirstByCollecteur(collecteur)
                .orElseThrow(() -> new RuntimeException("Compte de charge non trouvé pour collecteur: " + collecteur.getId()));
    }

    private Compte findCompteProduitEMF() {
        return compteRepository.findByTypeCompte("PRODUIT_EMF")
                .orElseThrow(() -> new RuntimeException("Compte produit EMF non trouvé"));
    }

    private Compte findCompteTaxe() {
        return compteRepository.findByTypeCompte("TAXE")
                .orElseThrow(() -> new RuntimeException("Compte taxe non trouvé"));
    }
}