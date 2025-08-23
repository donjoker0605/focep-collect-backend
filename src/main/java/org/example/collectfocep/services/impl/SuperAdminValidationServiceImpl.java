package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.ValidationException;
import org.example.collectfocep.exceptions.SecurityException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.SuperAdminValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 🔒 Implémentation du service de validation SuperAdmin
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminValidationServiceImpl implements SuperAdminValidationService {

    private final AgenceRepository agenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AdminRepository adminRepository;
    private final CollecteurRepository collecteurRepository;
    private final ParametreCommissionRepository parametreCommissionRepository;

    // Patterns de validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+237|237)?[0-9]{9}$"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$"
    );

    // Limites application
    private static final int MAX_AGENCES = 50;
    private static final int MAX_COLLECTEURS_PAR_AGENCE = 100;
    private static final int MAX_ADMINS_PAR_AGENCE = 5;

    // ================================
    // VALIDATION AGENCES
    // ================================

    @Override
    public void validateAgenceCreationData(AgenceDTO agenceDTO) {
        log.debug("🔍 Validation création agence: {}", agenceDTO.getNomAgence());
        
        // Validation données obligatoires
        validateNotEmpty(agenceDTO.getNomAgence(), "Nom agence");
        
        // Validation longueur
        if (agenceDTO.getNomAgence().length() > 100) {
            throw new ValidationException("Le nom de l'agence ne peut pas dépasser 100 caractères");
        }
        
        // Validation unicité nom
        if (agenceRepository.existsByNomAgence(agenceDTO.getNomAgence().trim())) {
            throw new ValidationException("Une agence avec ce nom existe déjà: " + agenceDTO.getNomAgence());
        }
        
        // Validation code agence si fourni
        if (agenceDTO.getCodeAgence() != null && !agenceDTO.getCodeAgence().trim().isEmpty()) {
            if (agenceDTO.getCodeAgence().length() > 10) {
                throw new ValidationException("Le code agence ne peut pas dépasser 10 caractères");
            }
            
            if (agenceRepository.existsByCodeAgence(agenceDTO.getCodeAgence().trim())) {
                throw new ValidationException("Une agence avec ce code existe déjà: " + agenceDTO.getCodeAgence());
            }
        }
        
        // Validation téléphone si fourni
        if (agenceDTO.getTelephone() != null && !agenceDTO.getTelephone().trim().isEmpty()) {
            validatePhoneNumber(agenceDTO.getTelephone());
        }
        
        // Validation limites application
        validateApplicationLimits();
        
        log.debug("✅ Validation création agence réussie");
    }

    @Override
    public void validateAgenceUpdateData(Long agenceId, AgenceDTO agenceDTO) {
        log.debug("🔍 Validation mise à jour agence: {}", agenceId);
        
        // Validation ID
        validateId(agenceId, "Agence");
        validateEntityExists(agenceId, "Agence");
        
        // Validation données obligatoires
        validateNotEmpty(agenceDTO.getNomAgence(), "Nom agence");
        
        // Validation longueur
        if (agenceDTO.getNomAgence().length() > 100) {
            throw new ValidationException("Le nom de l'agence ne peut pas dépasser 100 caractères");
        }
        
        // Validation unicité nom (exclure agence courante)
        agenceRepository.findByNomAgence(agenceDTO.getNomAgence().trim())
                .ifPresent(existingAgence -> {
                    if (!existingAgence.getId().equals(agenceId)) {
                        throw new ValidationException("Une autre agence avec ce nom existe déjà: " + agenceDTO.getNomAgence());
                    }
                });
        
        // Validation code agence si fourni
        if (agenceDTO.getCodeAgence() != null && !agenceDTO.getCodeAgence().trim().isEmpty()) {
            if (agenceDTO.getCodeAgence().length() > 10) {
                throw new ValidationException("Le code agence ne peut pas dépasser 10 caractères");
            }
            
            agenceRepository.findByCodeAgence(agenceDTO.getCodeAgence().trim())
                    .ifPresent(existingAgence -> {
                        if (!existingAgence.getId().equals(agenceId)) {
                            throw new ValidationException("Une autre agence avec ce code existe déjà: " + agenceDTO.getCodeAgence());
                        }
                    });
        }
        
        // Validation téléphone si fourni
        if (agenceDTO.getTelephone() != null && !agenceDTO.getTelephone().trim().isEmpty()) {
            validatePhoneNumber(agenceDTO.getTelephone());
        }
        
        log.debug("✅ Validation mise à jour agence réussie");
    }

    @Override
    public void validateAgenceDeletion(Long agenceId) {
        log.debug("🔍 Validation suppression agence: {}", agenceId);
        
        validateId(agenceId, "Agence");
        validateEntityExists(agenceId, "Agence");
        
        // Vérifier qu'il n'y a pas de collecteurs
        Long collecteursCount = collecteurRepository.countByAgenceId(agenceId);
        if (collecteursCount > 0) {
            throw new ValidationException("Impossible de supprimer une agence ayant des collecteurs. " +
                    "Nombre de collecteurs: " + collecteursCount);
        }
        
        // Vérifier qu'il n'y a pas d'admins
        Long adminsCount = adminRepository.countByAgenceId(agenceId);
        if (adminsCount > 0) {
            throw new ValidationException("Impossible de supprimer une agence ayant des admins. " +
                    "Nombre d'admins: " + adminsCount);
        }
        
        log.debug("✅ Validation suppression agence réussie");
    }

    // ================================
    // VALIDATION ADMINS
    // ================================

    @Override
    public void validateAdminCreationData(SuperAdminDTO adminDTO) {
        log.debug("🔍 Validation création admin: {}", adminDTO.getEmail());
        
        // Validation données obligatoires
        validateNotEmpty(adminDTO.getNom(), "Nom");
        validateNotEmpty(adminDTO.getPrenom(), "Prénom");
        validateNotEmpty(adminDTO.getEmail(), "Email");
        validateNotEmpty(adminDTO.getPassword(), "Mot de passe");
        validateNotEmpty(adminDTO.getNumeroCni(), "Numéro CNI");
        validateNotEmpty(adminDTO.getTelephone(), "Téléphone");
        
        // Validation formats
        validateEmail(adminDTO.getEmail());
        validatePhoneNumber(adminDTO.getTelephone());
        validatePasswordStrength(adminDTO.getPassword());
        
        // Validation longueurs
        if (adminDTO.getNom().length() > 100) {
            throw new ValidationException("Le nom ne peut pas dépasser 100 caractères");
        }
        if (adminDTO.getPrenom().length() > 100) {
            throw new ValidationException("Le prénom ne peut pas dépasser 100 caractères");
        }
        if (adminDTO.getNumeroCni().length() > 20) {
            throw new ValidationException("Le numéro CNI ne peut pas dépasser 20 caractères");
        }
        
        // Validation unicité
        if (utilisateurRepository.existsByAdresseMail(adminDTO.getEmail())) {
            throw new ValidationException("Un utilisateur avec cet email existe déjà: " + adminDTO.getEmail());
        }
        
        if (utilisateurRepository.existsByNumeroCni(adminDTO.getNumeroCni())) {
            throw new ValidationException("Un utilisateur avec ce numéro CNI existe déjà: " + adminDTO.getNumeroCni());
        }
        
        log.debug("✅ Validation création admin réussie");
    }

    @Override
    public void validateAdminUpdateData(Long adminId, SuperAdminDTO adminDTO) {
        log.debug("🔍 Validation mise à jour admin: {}", adminId);
        
        validateId(adminId, "Admin");
        validateEntityExists(adminId, "Admin");
        
        // Validation données obligatoires
        validateNotEmpty(adminDTO.getNom(), "Nom");
        validateNotEmpty(adminDTO.getPrenom(), "Prénom");
        validateNotEmpty(adminDTO.getEmail(), "Email");
        validateNotEmpty(adminDTO.getTelephone(), "Téléphone");
        
        // Validation formats
        validateEmail(adminDTO.getEmail());
        validatePhoneNumber(adminDTO.getTelephone());
        
        // Validation longueurs
        if (adminDTO.getNom().length() > 100) {
            throw new ValidationException("Le nom ne peut pas dépasser 100 caractères");
        }
        if (adminDTO.getPrenom().length() > 100) {
            throw new ValidationException("Le prénom ne peut pas dépasser 100 caractères");
        }
        
        // Validation unicité email (exclure utilisateur courant)
        utilisateurRepository.findByAdresseMail(adminDTO.getEmail())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(adminId)) {
                        throw new ValidationException("Un autre utilisateur avec cet email existe déjà: " + adminDTO.getEmail());
                    }
                });
        
        log.debug("✅ Validation mise à jour admin réussie");
    }

    @Override
    public void validatePasswordResetRequest(Long userId, PasswordResetRequest request) {
        log.debug("🔍 Validation reset password: {}", userId);
        
        validateId(userId, "Utilisateur");
        validateEntityExists(userId, "Utilisateur");
        
        validateNotEmpty(request.getNewPassword(), "Nouveau mot de passe");
        validatePasswordStrength(request.getNewPassword());
        
        log.debug("✅ Validation reset password réussie");
    }

    // ================================
    // VALIDATION COLLECTEURS
    // ================================

    @Override
    public void validateCollecteurCreationData(CollecteurCreateDTO collecteurDTO) {
        log.debug("🔍 Validation création collecteur: {}", collecteurDTO.getAdresseMail());
        
        // Validation données obligatoires
        validateNotEmpty(collecteurDTO.getNom(), "Nom");
        validateNotEmpty(collecteurDTO.getPrenom(), "Prénom");
        validateNotEmpty(collecteurDTO.getAdresseMail(), "Email");
        validateNotEmpty(collecteurDTO.getTelephone(), "Téléphone");
        validateId(collecteurDTO.getAgenceId(), "Agence");
        
        // Validation formats
        validateEmail(collecteurDTO.getAdresseMail());
        validatePhoneNumber(collecteurDTO.getTelephone());
        
        // Validation longueurs
        if (collecteurDTO.getNom().length() > 100) {
            throw new ValidationException("Le nom ne peut pas dépasser 100 caractères");
        }
        if (collecteurDTO.getPrenom().length() > 100) {
            throw new ValidationException("Le prénom ne peut pas dépasser 100 caractères");
        }
        
        // Validation unicité
        if (utilisateurRepository.existsByAdresseMail(collecteurDTO.getAdresseMail())) {
            throw new ValidationException("Un utilisateur avec cet email existe déjà: " + collecteurDTO.getAdresseMail());
        }
        
        // Validation agence
        validateEntityExists(collecteurDTO.getAgenceId(), "Agence");
        validateAgenceCapacity(collecteurDTO.getAgenceId());
        
        log.debug("✅ Validation création collecteur réussie");
    }

    @Override
    public void validateCollecteurUpdateData(Long collecteurId, CollecteurUpdateDTO collecteurDTO) {
        log.debug("🔍 Validation mise à jour collecteur: {}", collecteurId);
        
        validateId(collecteurId, "Collecteur");
        validateEntityExists(collecteurId, "Collecteur");
        
        // Validation données obligatoires
        validateNotEmpty(collecteurDTO.getNom(), "Nom");
        validateNotEmpty(collecteurDTO.getPrenom(), "Prénom");
        validateNotEmpty(collecteurDTO.getAdresseMail(), "Email");
        validateNotEmpty(collecteurDTO.getTelephone(), "Téléphone");
        
        // Validation formats
        validateEmail(collecteurDTO.getAdresseMail());
        validatePhoneNumber(collecteurDTO.getTelephone());
        
        // Validation longueurs
        if (collecteurDTO.getNom().length() > 100) {
            throw new ValidationException("Le nom ne peut pas dépasser 100 caractères");
        }
        if (collecteurDTO.getPrenom().length() > 100) {
            throw new ValidationException("Le prénom ne peut pas dépasser 100 caractères");
        }
        
        // Validation unicité email (exclure collecteur courant)
        utilisateurRepository.findByAdresseMail(collecteurDTO.getAdresseMail())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(collecteurId)) {
                        throw new ValidationException("Un autre utilisateur avec cet email existe déjà: " + collecteurDTO.getAdresseMail());
                    }
                });
        
        log.debug("✅ Validation mise à jour collecteur réussie");
    }

    // ================================
    // VALIDATION PARAMÈTRES COMMISSION
    // ================================

    @Override
    public void validateCommissionParameters(ParametreCommissionDTO parametreDTO) {
        log.debug("🔍 Validation paramètre commission: {}", parametreDTO.getTypeCommission());
        
        // Validation données obligatoires
        validateNotEmpty(parametreDTO.getTypeCommission(), "Type commission");
        
        if (parametreDTO.getValeur() == null) {
            throw new ValidationException("La valeur de commission est obligatoire");
        }
        
        // Validation valeurs
        if (parametreDTO.getValeur().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("La valeur de commission ne peut pas être négative");
        }
        
        if (parametreDTO.getValeur().compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException("La valeur de commission ne peut pas dépasser 100%");
        }
        
        // Validation seuils
        if (parametreDTO.getSeuilMin() != null && parametreDTO.getSeuilMin().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Le seuil minimum ne peut pas être négatif");
        }
        
        if (parametreDTO.getSeuilMax() != null && parametreDTO.getSeuilMax().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Le seuil maximum ne peut pas être négatif");
        }
        
        if (parametreDTO.getSeuilMin() != null && parametreDTO.getSeuilMax() != null) {
            if (parametreDTO.getSeuilMin().compareTo(parametreDTO.getSeuilMax()) > 0) {
                throw new ValidationException("Le seuil minimum ne peut pas être supérieur au seuil maximum");
            }
        }
        
        log.debug("✅ Validation paramètre commission réussie");
    }

    @Override
    public void validateCommissionParametersList(List<ParametreCommissionDTO> parametres) {
        log.debug("🔍 Validation liste paramètres commission: {} éléments", parametres.size());
        
        if (parametres.isEmpty()) {
            throw new ValidationException("La liste de paramètres ne peut pas être vide");
        }
        
        for (ParametreCommissionDTO parametre : parametres) {
            validateCommissionParameters(parametre);
        }
        
        log.debug("✅ Validation liste paramètres commission réussie");
    }

    // ================================
    // VALIDATION GÉNÉRALE
    // ================================

    @Override
    public void validateId(Long id, String entityName) {
        if (id == null || id <= 0) {
            throw new ValidationException("L'ID " + entityName + " est invalide: " + id);
        }
    }

    @Override
    public void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Le champ '" + fieldName + "' est obligatoire");
        }
    }

    @Override
    public void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Format d'email invalide: " + email);
        }
    }

    @Override
    public void validatePhoneNumber(String phoneNumber) {
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new ValidationException("Format de numéro de téléphone invalide: " + phoneNumber);
        }
    }

    @Override
    public void validateEntityExists(Long id, String entityName) {
        boolean exists = switch (entityName.toLowerCase()) {
            case "agence" -> agenceRepository.existsById(id);
            case "utilisateur", "admin", "collecteur" -> utilisateurRepository.existsById(id);
            default -> throw new ValidationException("Type d'entité non supporté: " + entityName);
        };
        
        if (!exists) {
            throw new ResourceNotFoundException(entityName + " non trouvé(e) avec l'ID: " + id);
        }
    }

    // ================================
    // VALIDATION BUSINESS RULES
    // ================================

    @Override
    public void validateUserAgenceAssignment(Long userId, Long agenceId) {
        validateId(userId, "Utilisateur");
        validateId(agenceId, "Agence");
        validateEntityExists(userId, "Utilisateur");
        validateEntityExists(agenceId, "Agence");
        
        // Vérifier que l'agence est active
        agenceRepository.findById(agenceId).ifPresent(agence -> {
            if (!agence.isActive()) {
                throw new ValidationException("Impossible d'assigner un utilisateur à une agence inactive");
            }
        });
    }

    @Override
    public void validateAgenceCapacity(Long agenceId) {
        validateId(agenceId, "Agence");
        validateEntityExists(agenceId, "Agence");
        
        Long collecteursCount = collecteurRepository.countByAgenceId(agenceId);
        if (collecteursCount >= MAX_COLLECTEURS_PAR_AGENCE) {
            throw new ValidationException("L'agence a atteint sa capacité maximale de collecteurs (" + 
                    MAX_COLLECTEURS_PAR_AGENCE + ")");
        }
    }

    @Override
    public void validateApplicationLimits() {
        Long agencesCount = agenceRepository.count();
        if (agencesCount >= MAX_AGENCES) {
            throw new ValidationException("Limite maximale d'agences atteinte (" + MAX_AGENCES + ")");
        }
    }

    // ================================
    // VALIDATION SÉCURITÉ
    // ================================

    @Override
    public void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new ValidationException("Le mot de passe doit contenir au moins 8 caractères");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("Le mot de passe doit contenir au moins une majuscule, " +
                    "une minuscule et un chiffre");
        }
    }

    @Override
    public void validateUserPermissions(String operation, Object... params) {
        // Cette méthode pourrait être étendue pour valider les permissions spécifiques
        log.debug("🔒 Validation permissions pour opération: {}", operation);
        // Pour l'instant, on assume que toutes les opérations SuperAdmin sont autorisées
        // car l'annotation @PreAuthorize("hasRole('SUPER_ADMIN')") est déjà en place
    }
}