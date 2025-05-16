package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.Mouvement;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat de la distribution des commissions
 * Contient tous les détails de la répartition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionDistribution {
    private Long collecteurId;
    private List<CommissionCalculation> calculations;
    private double totalCommissions;
    private double totalTVA;
    private BigDecimal remunerationCollecteur;
    private double partEMF;
    private double tvaEMF;
    private List<Mouvement> movements;

    public BigDecimal getTotalCommissionsBD() {
        return BigDecimal.valueOf(totalCommissions);
    }

    public BigDecimal getTotalTVABD() {
        return BigDecimal.valueOf(totalTVA);
    }

    public BigDecimal getPartEMFBD() {
        return BigDecimal.valueOf(partEMF);
    }

    public BigDecimal getTvaEMFBD() {
        return BigDecimal.valueOf(tvaEMF);
    }

    public int getNombreClients() {
        return calculations != null ? calculations.size() : 0;
    }

    public BigDecimal getMontantTotalDistribue() {
        return remunerationCollecteur
                .add(getPartEMFBD())
                .add(getTvaEMFBD());
    }

    // Getters pour la compatibilité avec l'ancien code
    public List<CommissionCalculation> getCalculations() { return calculations; }
    public double getTotalCommissions() { return totalCommissions; }
    public BigDecimal getRemunerationCollecteur() { return remunerationCollecteur; }
    public double getPartEMF() { return partEMF; }
    public double getTvaEMF() { return tvaEMF; }
    public double getTotalTVA() { return totalTVA; }
    public List<Mouvement> getMovements() { return movements; }
}