// dto/TransactionValidationRequest.java
package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO pour les requêtes de validation de transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionValidationRequest {

    @NotNull(message = "L'ID du client est requis")
    private Long clientId;

    @NotNull(message = "L'ID du collecteur est requis")
    private Long collecteurId;

    @NotNull(message = "Le montant est requis")
    @Positive(message = "Le montant doit être positif")
    private Double montant;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;
}