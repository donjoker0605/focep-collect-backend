package org.example.collectfocep.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionTierDTO {
    private Long id;

    @NotNull(message = "Le montant minimum est requis")
    @Min(value = 0, message = "Le montant minimum doit être positif")
    private Double montantMin;

    @NotNull(message = "Le montant maximum est requis")
    @Min(value = 1, message = "Le montant maximum doit être positif")
    private Double montantMax;

    @NotNull(message = "Le taux est requis")
    @Min(value = 0, message = "Le taux doit être positif")
    @Max(value = 100, message = "Le taux ne peut pas dépasser 100%")
    private Double taux;
}