package org.example.collectfocep.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.ParametreCommission;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateParametreCommissionRequest {

    @NotNull(message = "L'ID de l'agence est requis")
    private Long agenceId;

    @NotNull(message = "Le type d'opération est requis")
    private ParametreCommission.TypeOperation typeOperation;

    @DecimalMin(value = "0.0", message = "Le pourcentage de commission doit être positif")
    @DecimalMax(value = "100.0", message = "Le pourcentage de commission ne peut pas dépasser 100%")
    private BigDecimal pourcentageCommission;

    @DecimalMin(value = "0.0", message = "Le montant fixe doit être positif")
    private BigDecimal montantFixe;

    @DecimalMin(value = "0.0", message = "Le montant minimum doit être positif")
    private BigDecimal montantMinimum;

    @DecimalMin(value = "0.0", message = "Le montant maximum doit être positif")
    private BigDecimal montantMaximum;

    @Builder.Default
    private Boolean actif = true;
}