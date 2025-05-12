package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.CommissionCalculationException;
import org.example.collectfocep.repositories.CommissionParameterRepository;
import org.example.collectfocep.repositories.CommissionRepository;
import org.example.collectfocep.services.CommissionValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionService {

    @Value("${commission.tva.rate:0.1925}")
    private double TVA_RATE;

    @Value("${commission.fixed.amount:1000}")
    private double FIXED_COMMISSION_AMOUNT;

    @Value("${commission.percentage.rate:0.02}")
    private double DEFAULT_PERCENTAGE_RATE;

    private final CommissionParameterRepository commissionParameterRepository;
    private final CommissionValidationService validationService;
    private final CommissionRepository commissionRepository;

    @Transactional(readOnly = true)
    public CommissionResult calculateCommission(double montant, CommissionType type, Double valeurPersonnalisee) {
        return switch(type) {
            case FIXED -> calculateFixedCommission(montant, valeurPersonnalisee);
            case PERCENTAGE -> calculatePercentageCommission(montant, valeurPersonnalisee);
            case TIER -> calculateTierCommission(montant);
        };
    }

    @Transactional(readOnly = true)
    public double calculerCommission(Mouvement mouvement) {
        try {
            // Déterminer le client concerné
            Compte compteDestination = mouvement.getCompteDestination();

            // Vérifier si le compte de destination est un compte client
            if (!(compteDestination instanceof CompteClient)) {
                // Pas un compte client, pas de commission
                return 0.0;
            }

            // Récupérer le client de façon sûre
            final Client client = ((CompteClient) compteDestination).getClient();
            if (client == null) {
                // Pas de client associé, pas de commission
                return 0.0;
            }

            // Rechercher les paramètres de commission pour ce client
            CommissionParameter params = commissionParameterRepository.findByClient(client)
                    .orElseGet(() -> commissionParameterRepository.findByCollecteur(client.getCollecteur())
                            .orElseGet(() -> commissionParameterRepository.findByAgence(client.getAgence())
                                    .orElseThrow(() -> new CommissionCalculationException("Aucun paramètre de commission trouvé"))));

            // Calculer selon le type de commission
            switch (params.getType()) {
                case FIXED:
                    return params.getValeur();
                case PERCENTAGE:
                    return mouvement.getMontant() * (params.getValeur() / 100);
                case TIER:
                    return calculateCommissionByTier(params.getTiers(), mouvement.getMontant());
                default:
                    return 0.0;
            }
        } catch (Exception e) {
            log.error("Erreur lors du calcul de commission", e);
            return 0.0;
        }
    }

    private CommissionResult calculateFixedCommission(double montant, Double valeurPersonnalisee) {
        log.debug("Calcul de commission fixe pour le montant: {}", montant);
        double commission = valeurPersonnalisee != null ? valeurPersonnalisee : FIXED_COMMISSION_AMOUNT;
        double tva = commission * TVA_RATE;
        double montantNet = commission - tva;

        return CommissionResult.builder()
                .montantCommission(commission)
                .montantTVA(tva)
                .montantNet(montantNet)
                .typeCalcul("FIXED")
                .dateCalcul(LocalDateTime.now())
                .build();
    }

    private CommissionResult calculatePercentageCommission(double montant, Double valeurPersonnalisee) {
        log.debug("Calcul de commission en pourcentage pour le montant: {}", montant);
        double taux = valeurPersonnalisee != null ? valeurPersonnalisee / 100 : DEFAULT_PERCENTAGE_RATE;
        double commission = montant * taux;
        double tva = commission * TVA_RATE;
        double montantNet = commission - tva;

        return CommissionResult.builder()
                .montantCommission(commission)
                .montantTVA(tva)
                .montantNet(montantNet)
                .typeCalcul("PERCENTAGE")
                .dateCalcul(LocalDateTime.now())
                .build();
    }

    private CommissionResult calculateTierCommission(double montant) {
        log.debug("Calcul de commission par palier pour le montant: {}", montant);
        double rate;

        // Exemple de paliers
        if (montant <= 100000) {
            rate = 0.03;
        } else if (montant <= 500000) {
            rate = 0.02;
        } else {
            rate = 0.01;
        }

        double commission = montant * rate;
        double tva = commission * TVA_RATE;
        double montantNet = commission - tva;

        return CommissionResult.builder()
                .montantCommission(commission)
                .montantTVA(tva)
                .montantNet(montantNet)
                .typeCalcul("TIER")
                .dateCalcul(LocalDateTime.now())
                .build();
    }

    private double calculateCommissionByTier(List<CommissionTier> tiers, double montant) {
        return tiers.stream()
                .filter(tier -> montant >= tier.getMontantMin() && montant <= tier.getMontantMax())
                .findFirst()
                .map(tier -> montant * (tier.getTaux() / 100))
                .orElseThrow(() -> new CommissionCalculationException("Aucun palier trouvé pour le montant"));
    }

    // Méthode pour récupérer les commissions par agence
    @Transactional(readOnly = true)
    public List<Commission> findByAgenceId(Long agenceId) {
        log.debug("Récupération des commissions pour l'agence: {}", agenceId);
        // Vous devrez implémenter la requête dans le repository correspondant
        // ou utiliser une jointure avec les clients/collecteurs
        return commissionRepository.findByAgenceId(agenceId);
    }

    // Méthode pour récupérer les commissions par collecteur
    @Transactional(readOnly = true)
    public List<Commission> findByCollecteurId(Long collecteurId) {
        log.debug("Récupération des commissions pour le collecteur: {}", collecteurId);
        return commissionRepository.findByCollecteurId(collecteurId);
    }

    // Méthode pour sauvegarder un paramètre de commission
    @Transactional
    public CommissionParameter saveCommissionParameter(CommissionParameter parameter) {
        log.info("Sauvegarde d'un paramètre de commission: {}", parameter.getType());

        // Validation du paramètre
        validationService.validateCommissionParameters(parameter);

        // Sauvegarde du paramètre
        return commissionParameterRepository.save(parameter);
    }
}