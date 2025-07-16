package org.example.collectfocep.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour les requêtes de validation de données client
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientValidationRequest {

    @NotNull(message = "L'ID du collecteur est requis")
    private Long collecteurId;

    @NotBlank(message = "Le numéro de compte est requis")
    private String accountNumber;

    /**
     * Nom du client pour validation croisée (optionnel)
     */
    private String clientName;
}