package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionResult {
    private BigDecimal montantCommission;
    private BigDecimal montantTVA;
    private BigDecimal montantNet;
    private String typeCalcul;
    private LocalDateTime dateCalcul;
    private Long clientId;
    private Long collecteurId;
    private boolean success;
    private String errorMessage;
    private List<String> warnings;

    public static CommissionResult success(BigDecimal commission, BigDecimal tva, String typeCalcul, Long clientId, Long collecteurId) {
        return CommissionResult.builder()
                .montantCommission(commission)
                .montantTVA(tva)
                .montantNet(commission.subtract(tva))
                .typeCalcul(typeCalcul)
                .dateCalcul(LocalDateTime.now())
                .clientId(clientId)
                .collecteurId(collecteurId)
                .success(true)
                .build();
    }

    public static CommissionResult failure(Long clientId, Long collecteurId, String error) {
        return CommissionResult.builder()
                .clientId(clientId)
                .collecteurId(collecteurId)
                .success(false)
                .errorMessage(error)
                .dateCalcul(LocalDateTime.now())
                .build();
    }

    public BigDecimal getCommissionTotal() {
        if (montantCommission == null || montantTVA == null) {
            return BigDecimal.ZERO;
        }
        return montantCommission.add(montantTVA);
    }
}