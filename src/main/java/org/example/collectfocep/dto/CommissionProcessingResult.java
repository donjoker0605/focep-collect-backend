package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Résultat final du traitement des commissions
 * Contient un résumé de toute l'opération
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionProcessingResult {
    private Long collecteurId;
    private boolean success;
    private String errorMessage;
    private List<String> warnings;

    // Résultats financiers
    private BigDecimal totalCommissions;
    private BigDecimal remunerationCollecteur;
    private BigDecimal partEMF;
    private BigDecimal totalTVA;

    // Statistiques
    private int nombreClients;
    private int nombreCalculs;

    // Métadonnées
    private LocalDateTime processedAt;
    private long processingTimeMs;

    // Détails des calculs (optionnel pour les rapports détaillés)
    private List<CommissionCalculation> calculations;

    public static CommissionProcessingResult success(Long collecteurId,
                                                     List<CommissionCalculation> calculations,
                                                     double totalCommissions,
                                                     BigDecimal remunerationCollecteur,
                                                     double partEMF,
                                                     double totalTVA) {
        return CommissionProcessingResult.builder()
                .collecteurId(collecteurId)
                .success(true)
                .calculations(calculations)
                .totalCommissions(BigDecimal.valueOf(totalCommissions))
                .remunerationCollecteur(remunerationCollecteur)
                .partEMF(BigDecimal.valueOf(partEMF))
                .totalTVA(BigDecimal.valueOf(totalTVA))
                .nombreClients(calculations.size())
                .nombreCalculs(calculations.size())
                .processedAt(LocalDateTime.now())
                .build();
    }

    public static CommissionProcessingResult failure(Long collecteurId, String errorMessage) {
        return CommissionProcessingResult.builder()
                .collecteurId(collecteurId)
                .success(false)
                .errorMessage(errorMessage)
                .processedAt(LocalDateTime.now())
                .build();
    }

    public BigDecimal getMontantTotalDistribue() {
        if (remunerationCollecteur == null) return BigDecimal.ZERO;

        BigDecimal total = remunerationCollecteur;
        if (partEMF != null) total = total.add(partEMF);
        if (totalTVA != null) total = total.add(totalTVA);

        return total;
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new java.util.ArrayList<>();
        }
        warnings.add(warning);
    }
}