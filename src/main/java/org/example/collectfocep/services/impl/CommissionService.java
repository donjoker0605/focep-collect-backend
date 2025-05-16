package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.CommissionParameterRepository;
import org.example.collectfocep.repositories.CommissionRepository;
import org.example.collectfocep.services.CommissionValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionService {

    @Value("${commission.tva.rate:0.1925}")
    private double TVA_RATE;

    private final CommissionParameterRepository commissionParameterRepository;
    private final CommissionValidationService validationService;
    private final CommissionRepository commissionRepository;

    /**
     * AJOUT: Méthode pour calculer la commission d'un mouvement
     */
    @Transactional(readOnly = true)
    public double calculerCommission(Mouvement mouvement) {
        log.debug("Calcul commission pour mouvement: ID={}, Type={}, Montant={}",
                mouvement.getId(), mouvement.getSens(), mouvement.getMontant());

        // Logique simplifiée pour le moment - peut être enrichie
        double montant = mouvement.getMontant();

        // Selon le type d'opération
        switch (mouvement.getSens().toLowerCase()) {
            case "epargne":
                // 2% de commission pour épargne
                double commissionEpargne = montant * 0.02;
                log.debug("Commission épargne calculée: {}% de {} = {}", 2, montant, commissionEpargne);
                return commissionEpargne;

            case "retrait":
                // 1% de commission pour retrait
                double commissionRetrait = montant * 0.01;
                log.debug("Commission retrait calculée: {}% de {} = {}", 1, montant, commissionRetrait);
                return commissionRetrait;

            default:
                log.debug("Pas de commission pour opération de type: {}", mouvement.getSens());
                return 0.0;
        }
    }

    @Transactional(readOnly = true)
    public CommissionResult calculateCommission(BigDecimal montant, CommissionType type, BigDecimal valeurPersonnalisee, List<CommissionTier> tiers) {
        log.debug("Calcul de commission - Montant: {}, Type: {}, Valeur: {}", montant, type, valeurPersonnalisee);

        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            return CommissionResult.failure(null, null, "Montant invalide: " + montant);
        }

        BigDecimal commission = switch (type) {
            case FIXED -> calculateFixedCommission(montant, valeurPersonnalisee);
            case PERCENTAGE -> calculatePercentageCommission(montant, valeurPersonnalisee);
            case TIER -> calculateTierCommission(montant, tiers);
        };

        BigDecimal tva = commission.multiply(BigDecimal.valueOf(TVA_RATE))
                .setScale(2, RoundingMode.HALF_UP);

        return CommissionResult.success(commission, tva, type.name(), null, null);
    }

    private BigDecimal calculateFixedCommission(BigDecimal montant, BigDecimal valeurPersonnalisee) {
        log.debug("Calcul commission fixe: {}", valeurPersonnalisee);

        if (valeurPersonnalisee == null || valeurPersonnalisee.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Valeur fixe invalide: {}, utilisation de 0", valeurPersonnalisee);
            return BigDecimal.ZERO;
        }

        return valeurPersonnalisee.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentageCommission(BigDecimal montant, BigDecimal valeurPersonnalisee) {
        log.debug("Calcul commission pourcentage - Montant: {}, Taux: {}%", montant, valeurPersonnalisee);

        if (valeurPersonnalisee == null || valeurPersonnalisee.compareTo(BigDecimal.ZERO) < 0
                || valeurPersonnalisee.compareTo(BigDecimal.valueOf(100)) > 0) {
            log.warn("Pourcentage invalide: {}, utilisation de 0%", valeurPersonnalisee);
            return BigDecimal.ZERO;
        }

        return montant.multiply(valeurPersonnalisee)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTierCommission(BigDecimal montant, List<CommissionTier> tiers) {
        log.debug("Calcul commission par palier - Montant: {}, Nombre de paliers: {}",
                montant, tiers != null ? tiers.size() : 0);

        if (tiers == null || tiers.isEmpty()) {
            log.warn("Aucun palier défini, commission = 0");
            return BigDecimal.ZERO;
        }

        for (CommissionTier tier : tiers) {
            BigDecimal min = BigDecimal.valueOf(tier.getMontantMin());
            BigDecimal max = BigDecimal.valueOf(tier.getMontantMax());

            if (montant.compareTo(min) >= 0 && montant.compareTo(max) <= 0) {
                BigDecimal taux = BigDecimal.valueOf(tier.getTaux());
                BigDecimal commission = montant.multiply(taux)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                log.debug("Palier trouvé [{}-{}], taux: {}%, commission: {}",
                        min, max, taux, commission);
                return commission;
            }
        }

        log.warn("Aucun palier trouvé pour le montant: {}", montant);
        return BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<Commission> findByAgenceId(Long agenceId) {
        log.debug("Récupération commissions pour agence: {}", agenceId);
        return commissionRepository.findByAgenceId(agenceId);
    }

    @Transactional(readOnly = true)
    public List<Commission> findByCollecteurId(Long collecteurId) {
        log.debug("Récupération commissions pour collecteur: {}", collecteurId);
        return commissionRepository.findByCollecteurId(collecteurId);
    }

    @Transactional
    public CommissionParameter saveCommissionParameter(CommissionParameter parameter) {
        log.info("Sauvegarde paramètre commission: type={}, scope={}",
                parameter.getType(), getParameterScope(parameter));

        // Validation
        var validationResult = validationService.validateCommissionParameters(parameter);
        validationResult.throwIfInvalid();

        // Log des warnings
        if (!validationResult.getWarnings().isEmpty()) {
            log.warn("Avertissements lors de la validation: {}", validationResult.getWarnings());
        }

        // Sauvegarde
        var saved = commissionParameterRepository.save(parameter);
        log.info("Paramètre sauvegardé avec ID: {}", saved.getId());

        return saved;
    }

    private String getParameterScope(CommissionParameter parameter) {
        if (parameter.getClient() != null) return "CLIENT:" + parameter.getClient().getId();
        if (parameter.getCollecteur() != null) return "COLLECTEUR:" + parameter.getCollecteur().getId();
        if (parameter.getAgence() != null) return "AGENCE:" + parameter.getAgence().getId();
        return "UNDEFINED";
    }
}