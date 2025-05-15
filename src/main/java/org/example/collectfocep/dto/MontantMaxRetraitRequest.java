package org.example.collectfocep.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MontantMaxRetraitRequest {
    @NotNull(message = "Le nouveau montant est obligatoire")
    @DecimalMin(value = "1000.0", message = "Le montant doit être positif")
    private Double nouveauMontant;

    @NotBlank(message = "La justification est obligatoire")
    private String justification;
}