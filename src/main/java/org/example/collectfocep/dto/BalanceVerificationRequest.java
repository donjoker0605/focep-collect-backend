package org.example.collectfocep.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Valid
public class BalanceVerificationRequest {
    @NotNull
    private Long clientId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private Double montant;
}
