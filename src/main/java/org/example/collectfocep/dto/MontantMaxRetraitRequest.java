package org.example.collectfocep.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MontantMaxRetraitRequest {
    @NotNull(message = "Le nouveau montant est obligatoire")
    @Min(value = 1000, message = "Le montant minimum doit être de 1000")
    private Double nouveauMontant;

    @NotBlank(message = "La justification est obligatoire")
    private String justification;
}
