package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.CommissionTier;
import org.example.collectfocep.entities.CommissionType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * Requête de simulation de commission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSimulationRequest {
    @NotNull
    @Positive
    private BigDecimal montant;

    @NotNull
    private CommissionType type;

    private BigDecimal valeur; // Pour FIXED et PERCENTAGE

    private List<CommissionTier> tiers; // Pour TIER
}