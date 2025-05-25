package org.example.collectfocep.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BalanceVerificationRequest {
    @NotNull(message = "L'ID du client est requis")
    private Long clientId;

    @NotNull(message = "Le montant est requis")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private Double montant;
}
