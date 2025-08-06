package org.example.collectfocep.services;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionTier;
import org.example.collectfocep.entities.CommissionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service de calcul pur des commissions selon les spécifications FOCEP
 * - Montant fixe
 * - Pourcentage  
 * - Paliers (taux appliqué sur l'intégralité du montant)
 */
@Service
@Slf4j
public class CommissionCalculatorService {

    /**
     * Calcule la commission "x" pour un client selon ses paramètres
     */
    public BigDecimal calculateCommission(BigDecimal montantTotal, CommissionParameter parameter) {
        log.debug("Calcul commission - Montant: {}, Type: {}", montantTotal, parameter.getType());
        
        if (montantTotal == null || montantTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Montant invalide: {}", montantTotal);
            return BigDecimal.ZERO;
        }

        return switch (parameter.getType()) {
            case FIXED -> calculateFixedCommission(parameter.getValeurPersonnalisee());
            case PERCENTAGE -> calculatePercentageCommission(montantTotal, parameter.getValeurPersonnalisee());
            case TIER -> calculateTierCommission(montantTotal, parameter.getTiers());
        };
    }

    /**
     * Calcul commission par montant fixe
     * Commission = montant fixe défini (indépendant du montant collecté)
     */
    private BigDecimal calculateFixedCommission(BigDecimal montantFixe) {
        log.debug("Commission fixe: {}", montantFixe);
        
        if (montantFixe == null || montantFixe.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Montant fixe invalide: {}, utilisation de 0", montantFixe);
            return BigDecimal.ZERO;
        }
        
        return montantFixe.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcul commission par pourcentage
     * Commission = montantTotal * pourcentage / 100
     */
    private BigDecimal calculatePercentageCommission(BigDecimal montantTotal, BigDecimal pourcentage) {
        log.debug("Commission pourcentage - Montant: {}, Taux: {}%", montantTotal, pourcentage);
        
        if (pourcentage == null || pourcentage.compareTo(BigDecimal.ZERO) < 0 
                || pourcentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            log.warn("Pourcentage invalide: {}%, utilisation de 0%", pourcentage);
            return BigDecimal.ZERO;
        }
        
        return montantTotal
                .multiply(pourcentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcul commission par paliers d'intervalle continu
     * Le montant détermine le palier applicable, et ce taux s'applique à l'intégralité
     * 
     * Exemple spec FOCEP :
     * - 0-100K : 5% → si client collecte 450K = 4% sur la totalité = 18K
     * - 100K-500K : 4%
     * - 500K+ : 3%
     */
    private BigDecimal calculateTierCommission(BigDecimal montantTotal, List<CommissionTier> tiers) {
        log.debug("Commission paliers - Montant: {}, Nb paliers: {}", 
                montantTotal, tiers != null ? tiers.size() : 0);
        
        if (tiers == null || tiers.isEmpty()) {
            log.warn("Aucun palier défini, commission = 0");
            return BigDecimal.ZERO;
        }
        
        // Tri des paliers par montant minimum pour garantir l'ordre
        tiers.sort((t1, t2) -> Double.compare(t1.getMontantMin(), t2.getMontantMin()));
        
        for (CommissionTier tier : tiers) {
            BigDecimal min = BigDecimal.valueOf(tier.getMontantMin());
            BigDecimal max = BigDecimal.valueOf(tier.getMontantMax());
            
            // Vérification que le montant est dans cet intervalle
            if (montantTotal.compareTo(min) >= 0 && 
                (tier.getMontantMax() == Double.MAX_VALUE || montantTotal.compareTo(max) <= 0)) {
                
                BigDecimal taux = BigDecimal.valueOf(tier.getTaux());
                BigDecimal commission = montantTotal
                        .multiply(taux)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                
                log.debug("Palier trouvé [{}-{}], taux: {}%, commission: {}", 
                        min, max.equals(BigDecimal.valueOf(Double.MAX_VALUE)) ? "∞" : max, 
                        taux, commission);
                return commission;
            }
        }
        
        log.warn("Aucun palier trouvé pour montant: {}", montantTotal);
        return BigDecimal.ZERO;
    }

    /**
     * Calcule la TVA sur une commission selon le taux FOCEP (19,25%)
     */
    public BigDecimal calculateTVA(BigDecimal commission) {
        if (commission == null || commission.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal tauxTVA = BigDecimal.valueOf(0.1925); // 19,25%
        return commission
                .multiply(tauxTVA)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcul du solde net client après commission et TVA
     */
    public BigDecimal calculateSoldeNet(BigDecimal soldeActuel, BigDecimal commission, BigDecimal tva) {
        if (soldeActuel == null) soldeActuel = BigDecimal.ZERO;
        if (commission == null) commission = BigDecimal.ZERO;
        if (tva == null) tva = BigDecimal.ZERO;
        
        return soldeActuel
                .subtract(commission)
                .subtract(tva)
                .setScale(2, RoundingMode.HALF_UP);
    }
}