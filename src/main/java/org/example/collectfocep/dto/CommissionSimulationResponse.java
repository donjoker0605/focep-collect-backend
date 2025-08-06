package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSimulationResponse {

    private BigDecimal montantBase;
    private BigDecimal commissionCalculee;
    private BigDecimal tvaApplicable;
    private BigDecimal montantNet;
    private BigDecimal montantTotal;

    private String typeCalcul;
    private String parametresUtilises;
    private String scope; // CLIENT, COLLECTEUR, AGENCE

    // Pour TIER - détail des paliers
    private String palierApplique;
    private List<CommissionTierDetail> paliersDisponibles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommissionTierDetail {
        private BigDecimal montantMin;
        private BigDecimal montantMax;
        private BigDecimal taux;
        private boolean applicable;
    }

    public static CommissionSimulationResponse from(CommissionResult result, BigDecimal montantBase) {
        return CommissionSimulationResponse.builder()
                .montantBase(montantBase)
                .commissionCalculee(result.getMontantCommission())
                .tvaApplicable(result.getMontantTVA())
                .montantNet(result.getMontantNet())
                .montantTotal(result.getCommissionTotal())
                .typeCalcul(result.getTypeCalcul())
                .build();
    }
}