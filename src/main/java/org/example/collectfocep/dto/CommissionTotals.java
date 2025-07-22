package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les totaux de commissions
 * Utilisé par CommissionDistributionEngine pour les calculs agrégés
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionTotals {

    // Totaux en double pour compatibilité existing code
    private double totalCommissions;
    private double totalTVAClient;
    private double montantEMF;
    private double montantTVAEMF;

    // Totaux en BigDecimal pour précision
    private BigDecimal totalCommissionsBD;
    private BigDecimal totalTVAClientBD;
    private BigDecimal montantEMFBD;
    private BigDecimal montantTVAEMFBD;

    // Métadonnées de calcul
    private int nombreClients;
    private int nombreCalculs;

    // Factory method pour création depuis BigDecimal
    public static CommissionTotals fromBigDecimal(
            BigDecimal totalCommissions,
            BigDecimal totalTVAClient,
            BigDecimal montantEMF,
            BigDecimal montantTVAEMF,
            int nombreClients) {

        return CommissionTotals.builder()
                .totalCommissions(totalCommissions.doubleValue())
                .totalTVAClient(totalTVAClient.doubleValue())
                .montantEMF(montantEMF.doubleValue())
                .montantTVAEMF(montantTVAEMF.doubleValue())
                .totalCommissionsBD(totalCommissions)
                .totalTVAClientBD(totalTVAClient)
                .montantEMFBD(montantEMF)
                .montantTVAEMFBD(montantTVAEMF)
                .nombreClients(nombreClients)
                .nombreCalculs(nombreClients) // Assumons 1 calcul par client
                .build();
    }

    // Factory method pour création depuis double (transition)
    public static CommissionTotals fromDouble(
            double totalCommissions,
            double totalTVAClient,
            double montantEMF,
            double montantTVAEMF,
            int nombreClients) {

        return CommissionTotals.builder()
                .totalCommissions(totalCommissions)
                .totalTVAClient(totalTVAClient)
                .montantEMF(montantEMF)
                .montantTVAEMF(montantTVAEMF)
                .totalCommissionsBD(BigDecimal.valueOf(totalCommissions))
                .totalTVAClientBD(BigDecimal.valueOf(totalTVAClient))
                .montantEMFBD(BigDecimal.valueOf(montantEMF))
                .montantTVAEMFBD(BigDecimal.valueOf(montantTVAEMF))
                .nombreClients(nombreClients)
                .nombreCalculs(nombreClients)
                .build();
    }

    // Méthodes utilitaires
    public BigDecimal getTotalDistribue() {
        BigDecimal total = getBigDecimalTotalCommissions();
        if (total == null) total = BigDecimal.ZERO;

        BigDecimal tva = getBigDecimalMontantTVAEMF();
        if (tva != null) total = total.add(tva);

        return total;
    }

    public boolean hasData() {
        return totalCommissions > 0 || nombreClients > 0;
    }

    // Getters BigDecimal avec fallback
    public BigDecimal getBigDecimalTotalCommissions() {
        return totalCommissionsBD != null ? totalCommissionsBD : BigDecimal.valueOf(totalCommissions);
    }

    public BigDecimal getBigDecimalTotalTVAClient() {
        return totalTVAClientBD != null ? totalTVAClientBD : BigDecimal.valueOf(totalTVAClient);
    }

    public BigDecimal getBigDecimalMontantEMF() {
        return montantEMFBD != null ? montantEMFBD : BigDecimal.valueOf(montantEMF);
    }

    public BigDecimal getBigDecimalMontantTVAEMF() {
        return montantTVAEMFBD != null ? montantTVAEMFBD : BigDecimal.valueOf(montantTVAEMF);
    }

    // Validation et cohérence
    public boolean isCoherent() {
        // Vérifier que BigDecimal et double sont cohérents (si les deux sont présents)
        if (totalCommissionsBD != null) {
            double diff = Math.abs(totalCommissions - totalCommissionsBD.doubleValue());
            if (diff > 0.01) return false; // Tolérance 1 centime
        }

        if (montantEMFBD != null) {
            double diff = Math.abs(montantEMF - montantEMFBD.doubleValue());
            if (diff > 0.01) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("CommissionTotals{clients=%d, total=%.2f, EMF=%.2f, TVA=%.2f}",
                nombreClients, totalCommissions, montantEMF, montantTVAEMF);
    }
}