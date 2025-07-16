package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les réponses de pré-validation des transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPreValidationDTO {

    // Validation générale
    private Boolean canProceed;
    private String errorMessage;

    // Informations client
    private Long clientId;
    private String clientName;
    private String numeroCompte;
    private String displayName;

    // Validation téléphone
    private Boolean hasValidPhone;
    private String phoneWarningMessage;

    // Validation solde collecteur (pour retraits)
    private Boolean soldeCollecteurSuffisant;
    private String soldeCollecteurMessage;
    private Double soldeCollecteurActuel;

    // Validation solde client (pour retraits)
    private Boolean soldeClientSuffisant;
    private String soldeClientMessage;
    private Double soldeClientActuel;

    // Informations supplémentaires
    private Double montantDemande;
    private String typeOperation;
    private String notes;
}