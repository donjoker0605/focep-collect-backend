package org.example.collectfocep.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MontantMaxRetraitRequest {

    @NotNull(message = "Le nouveau montant est requis")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif")
    private Double nouveauMontant;

    @Size(max = 500, message = "La justification ne peut pas dépasser 500 caractères")
    private String justification;
}