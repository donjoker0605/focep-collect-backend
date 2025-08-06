package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionAggregateResult {

    // Statistiques générales
    private Long totalCollecteurs;
    private Long collecteursTraites;
    private Integer totalClients;
    private BigDecimal totalCommissions;
    private BigDecimal totalRemuneration;
    private BigDecimal totalTVA;

    // Période traitée
    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Détails par collecteur
    private List<CommissionProcessingResult> details;

    // Métadonnées
    private String calculePar;
    private java.time.LocalDateTime dateCalcul;
    private Long agenceId;

    // Statistiques dérivées
    public BigDecimal getMoyenneCommissionParCollecteur() {
        if (totalCollecteurs == null || totalCollecteurs == 0 || totalCommissions == null) {
            return BigDecimal.ZERO;
        }
        return totalCommissions.divide(BigDecimal.valueOf(totalCollecteurs),
                2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getMoyenneCommissionParClient() {
        if (totalClients == null || totalClients == 0 || totalCommissions == null) {
            return BigDecimal.ZERO;
        }
        return totalCommissions.divide(BigDecimal.valueOf(totalClients),
                2, java.math.RoundingMode.HALF_UP);
    }

    public double getTauxReussite() {
        if (totalCollecteurs == null || totalCollecteurs == 0) {
            return 0.0;
        }
        return (collecteursTraites.doubleValue() / totalCollecteurs.doubleValue()) * 100.0;
    }
}

