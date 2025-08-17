package org.example.collectfocep.services;

import org.example.collectfocep.dto.*;

/**
 * 🔒 Service de validation SuperAdmin pour prévenir les erreurs 400/500
 * 
 * Toutes les validations business et techniques pour garantir:
 * - Prévention erreurs 400 (Bad Request)
 * - Prévention erreurs 500 (Internal Server Error)
 * - Validation données avant traitement
 * - Cohérence business rules
 */
public interface SuperAdminValidationService {

    // ================================
    // VALIDATION AGENCES
    // ================================
    
    /**
     * Valide les données pour création d'agence
     * @throws ValidationException si données invalides
     */
    void validateAgenceCreationData(AgenceDTO agenceDTO);
    
    /**
     * Valide les données pour mise à jour d'agence
     * @throws ValidationException si données invalides
     */
    void validateAgenceUpdateData(Long agenceId, AgenceDTO agenceDTO);
    
    /**
     * Valide qu'une agence peut être supprimée
     * @throws ValidationException si suppression impossible
     */
    void validateAgenceDeletion(Long agenceId);

    // ================================
    // VALIDATION ADMINS
    // ================================
    
    /**
     * Valide les données pour création d'admin
     * @throws ValidationException si données invalides
     */
    void validateAdminCreationData(SuperAdminDTO adminDTO);
    
    /**
     * Valide les données pour mise à jour d'admin
     * @throws ValidationException si données invalides
     */
    void validateAdminUpdateData(Long adminId, SuperAdminDTO adminDTO);
    
    /**
     * Valide une demande de reset password
     * @throws ValidationException si demande invalide
     */
    void validatePasswordResetRequest(Long userId, PasswordResetRequest request);

    // ================================
    // VALIDATION COLLECTEURS
    // ================================
    
    /**
     * Valide les données pour création de collecteur
     * @throws ValidationException si données invalides
     */
    void validateCollecteurCreationData(CollecteurCreateDTO collecteurDTO);
    
    /**
     * Valide les données pour mise à jour de collecteur
     * @throws ValidationException si données invalides
     */
    void validateCollecteurUpdateData(Long collecteurId, CollecteurUpdateDTO collecteurDTO);

    // ================================
    // VALIDATION PARAMÈTRES COMMISSION
    // ================================
    
    /**
     * Valide les paramètres de commission
     * @throws ValidationException si paramètres invalides
     */
    void validateCommissionParameters(ParametreCommissionDTO parametreDTO);
    
    /**
     * Valide une liste de paramètres de commission
     * @throws ValidationException si un paramètre invalide
     */
    void validateCommissionParametersList(java.util.List<ParametreCommissionDTO> parametres);

    // ================================
    // VALIDATION GÉNÉRALE
    // ================================
    
    /**
     * Valide qu'un ID est valide (non null, > 0)
     * @throws ValidationException si ID invalide
     */
    void validateId(Long id, String entityName);
    
    /**
     * Valide qu'une chaîne n'est pas vide
     * @throws ValidationException si chaîne vide
     */
    void validateNotEmpty(String value, String fieldName);
    
    /**
     * Valide un email
     * @throws ValidationException si email invalide
     */
    void validateEmail(String email);
    
    /**
     * Valide un numéro de téléphone
     * @throws ValidationException si téléphone invalide
     */
    void validatePhoneNumber(String phoneNumber);
    
    /**
     * Valide qu'une entité existe en base
     * @throws ResourceNotFoundException si entité inexistante
     */
    void validateEntityExists(Long id, String entityName);

    // ================================
    // VALIDATION BUSINESS RULES
    // ================================
    
    /**
     * Valide qu'un utilisateur peut être assigné à une agence
     * @throws ValidationException si assignation impossible
     */
    void validateUserAgenceAssignment(Long userId, Long agenceId);
    
    /**
     * Valide qu'une agence peut accepter de nouveaux utilisateurs
     * @throws ValidationException si agence pleine ou inactive
     */
    void validateAgenceCapacity(Long agenceId);
    
    /**
     * Valide les limites de l'application (50 agences max, etc.)
     * @throws ValidationException si limites dépassées
     */
    void validateApplicationLimits();

    // ================================
    // VALIDATION SÉCURITÉ
    // ================================
    
    /**
     * Valide qu'un mot de passe respecte les critères
     * @throws ValidationException si mot de passe faible
     */
    void validatePasswordStrength(String password);
    
    /**
     * Valide qu'un utilisateur a les droits pour l'opération
     * @throws SecurityException si droits insuffisants
     */
    void validateUserPermissions(String operation, Object... params);
}