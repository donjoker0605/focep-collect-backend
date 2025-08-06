package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationCommissionRequest {
    @NotNull
    private Long clientId;

    @NotNull
    @Positive
    private BigDecimal montantEpargne;

    @NotNull
    private String typeCommission;

    @NotNull
    @Positive
    private BigDecimal valeurCommission;
}
