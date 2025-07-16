package org.example.collectfocep.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse de validation de données client
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientValidationDTO {

    /**
     * Indique si un client a été trouvé
     */
    private Boolean clientFound;

    /**
     * Informations du client trouvé
     */
    private Long clientId;
    private String clientName;
    private String accountNumber;

    /**
     * Validation du téléphone
     */
    private Boolean hasValidPhone;
    private String phoneWarning;

    /**
     * Messages
     */
    private String errorMessage;
    private String successMessage;

    /**
     * Données additionnelles pour l'interface
     */
    private String displayName;
    private String numeroCni;

    /**
     * Vérifier s'il y a des avertissements
     */
    public Boolean hasWarnings() {
        return phoneWarning != null && !phoneWarning.trim().isEmpty();
    }

    /**
     * Vérifier s'il y a des erreurs
     */
    public Boolean hasErrors() {
        return !clientFound || (errorMessage != null && !errorMessage.trim().isEmpty());
    }
}