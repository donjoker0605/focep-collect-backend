package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO pour la pré-validation des transactions
 * Contient toutes les informations nécessaires pour valider une transaction avant exécution
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionPreValidationDTO {

    /**
     * Indique si la transaction peut être exécutée
     */
    private Boolean canProceed;

    /**
     * Informations du client
     */
    private Long clientId;
    private String clientName;
    private String numeroCompte;

    /**
     * Validation du téléphone
     */
    private Boolean hasValidPhone;
    private String phoneWarningMessage;

    /**
     * Validation du solde collecteur (pour retraits uniquement)
     */
    private Boolean soldeCollecteurSuffisant;
    private String soldeCollecteurMessage;

    /**
     * Messages d'erreur générique
     */
    private String errorMessage;
    private String successMessage;

    /**
     * Vérifier s'il y a des avertissements non bloquants
     */
    public Boolean hasWarnings() {
        return phoneWarningMessage != null;
    }

    /**
     * Vérifier s'il y a des erreurs bloquantes
     */
    public Boolean hasErrors() {
        return !canProceed || errorMessage != null;
    }
}