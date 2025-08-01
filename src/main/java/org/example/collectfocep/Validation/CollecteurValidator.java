package org.example.collectfocep.Validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CollecteurValidator implements Validator {

    private final CollecteurRepository collecteurRepository;

    // ================================
    // CONSTANTES DE VALIDATION CORRIGÉES
    // ================================

    private static final int LONGUEUR_MIN_NOM = 2;
    private static final int LONGUEUR_MAX_NOM = 50;
    private static final int LONGUEUR_CNI = 13;

    // ✅ CORRECTION: Utiliser BigDecimal pour les montants
    private static final BigDecimal MONTANT_MAX_RETRAIT_MIN = BigDecimal.valueOf(1000.0);
    private static final BigDecimal MONTANT_MAX_RETRAIT_MAX = BigDecimal.valueOf(1000000.0);

    private static final String REGEX_EMAIL = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String REGEX_TELEPHONE = "^[0-9]{9}$";

    @Override
    public boolean supports(Class<?> clazz) {
        return Collecteur.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Collecteur collecteur = (Collecteur) target;

        // ================================
        // VALIDATION DU MONTANT MAXIMUM DE RETRAIT - CORRIGÉE
        // ================================

        if (collecteur.getMontantMaxRetrait() == null) {
            errors.rejectValue("montantMaxRetrait",
                    "montant.required",
                    "Le montant maximum de retrait est obligatoire");
        } else {
            if (collecteur.getMontantMaxRetrait().compareTo(BigDecimal.ZERO) <= 0) {
                errors.rejectValue("montantMaxRetrait",
                        "montant.invalid",
                        "Le montant maximum de retrait doit être positif");
            } else if (collecteur.getMontantMaxRetrait().compareTo(MONTANT_MAX_RETRAIT_MIN) < 0) {
                errors.rejectValue("montantMaxRetrait",
                        "montant.trop.petit",
                        String.format("Le montant maximum de retrait doit être supérieur à %s",
                                MONTANT_MAX_RETRAIT_MIN.toString()));
            } else if (collecteur.getMontantMaxRetrait().compareTo(MONTANT_MAX_RETRAIT_MAX) > 0) {
                errors.rejectValue("montantMaxRetrait",
                        "montant.trop.grand",
                        String.format("Le montant maximum de retrait doit être inférieur à %s",
                                MONTANT_MAX_RETRAIT_MAX.toString()));
            }
        }

        // ================================
        // VALIDATION DE L'ANCIENNETÉ - INCHANGÉE
        // ================================

        if (collecteur.getAncienneteEnMois() != null && collecteur.getAncienneteEnMois() < 0) {
            errors.rejectValue("ancienneteEnMois",
                    "anciennete.invalid",
                    "L'ancienneté ne peut pas être négative");
        }

        // ================================
        // VALIDATION DU NOM - INCHANGÉE
        // ================================

        if (collecteur.getNom() == null || collecteur.getNom().trim().isEmpty()) {
            errors.rejectValue("nom",
                    "nom.required",
                    "Le nom est obligatoire");
        } else if (collecteur.getNom().length() < LONGUEUR_MIN_NOM) {
            errors.rejectValue("nom",
                    "nom.trop.court",
                    String.format("Le nom doit contenir au moins %d caractères", LONGUEUR_MIN_NOM));
        } else if (collecteur.getNom().length() > LONGUEUR_MAX_NOM) {
            errors.rejectValue("nom",
                    "nom.trop.long",
                    String.format("Le nom ne peut pas dépasser %d caractères", LONGUEUR_MAX_NOM));
        }

        // ================================
        // VALIDATION DU PRÉNOM - INCHANGÉE
        // ================================

        if (collecteur.getPrenom() == null || collecteur.getPrenom().trim().isEmpty()) {
            errors.rejectValue("prenom",
                    "prenom.required",
                    "Le prénom est obligatoire");
        } else if (collecteur.getPrenom().length() < LONGUEUR_MIN_NOM) {
            errors.rejectValue("prenom",
                    "prenom.trop.court",
                    String.format("Le prénom doit contenir au moins %d caractères", LONGUEUR_MIN_NOM));
        } else if (collecteur.getPrenom().length() > LONGUEUR_MAX_NOM) {
            errors.rejectValue("prenom",
                    "prenom.trop.long",
                    String.format("Le prénom ne peut pas dépasser %d caractères", LONGUEUR_MAX_NOM));
        }

        // ================================
        // VALIDATION DU NUMÉRO CNI - INCHANGÉE
        // ================================

        if (collecteur.getNumeroCni() == null || collecteur.getNumeroCni().trim().isEmpty()) {
            errors.rejectValue("numeroCni",
                    "numeroCni.required",
                    "Le numéro de CNI est obligatoire");
        } else if (!collecteur.getNumeroCni().matches("^[0-9]{" + LONGUEUR_CNI + "}$")) {
            errors.rejectValue("numeroCni",
                    "numeroCni.invalid",
                    String.format("Le numéro de CNI doit contenir exactement %d chiffres", LONGUEUR_CNI));
        }

        // ================================
        // VALIDATION DE L'EMAIL - INCHANGÉE
        // ================================

        if (collecteur.getAdresseMail() == null || collecteur.getAdresseMail().trim().isEmpty()) {
            errors.rejectValue("adresseMail",
                    "email.required",
                    "L'adresse email est obligatoire");
        } else if (!collecteur.getAdresseMail().matches(REGEX_EMAIL)) {
            errors.rejectValue("adresseMail",
                    "email.invalid",
                    "L'adresse email n'est pas valide");
        } else if (collecteur.getId() == null &&
                collecteurRepository.existsByAdresseMail(collecteur.getAdresseMail())) {
            errors.rejectValue("adresseMail",
                    "email.duplique",
                    "Cette adresse email est déjà utilisée");
        }

        // ================================
        // VALIDATION DU TÉLÉPHONE - INCHANGÉE
        // ================================

        if (collecteur.getTelephone() == null || collecteur.getTelephone().trim().isEmpty()) {
            errors.rejectValue("telephone",
                    "telephone.required",
                    "Le numéro de téléphone est obligatoire");
        } else if (!collecteur.getTelephone().matches(REGEX_TELEPHONE)) {
            errors.rejectValue("telephone",
                    "telephone.invalid",
                    "Le numéro de téléphone doit contenir 9 chiffres");
        }

        // ================================
        // VALIDATION DE L'AGENCE - INCHANGÉE
        // ================================

        if (collecteur.getAgence() == null) {
            errors.rejectValue("agence",
                    "agence.required",
                    "L'agence est obligatoire");
        }

        // ================================
        // VALIDATION DU MOT DE PASSE - INCHANGÉE
        // ================================

        if (collecteur.getId() == null &&
                (collecteur.getPassword() == null || collecteur.getPassword().trim().isEmpty())) {
            errors.rejectValue("password",
                    "password.required",
                    "Le mot de passe est obligatoire à la création");
        }

        // ================================
        // VALIDATION DU RÔLE - INCHANGÉE
        // ================================

        if (collecteur.getRole() == null || collecteur.getRole().trim().isEmpty()) {
            errors.rejectValue("role",
                    "role.required",
                    "Le rôle est obligatoire");
        }
    }

    /**
     * Valide un collecteur et lance des exceptions en cas d'erreurs
     * Cette méthode est utilisée par le service pour valider un collecteur
     * avant sauvegarde
     *
     * @param collecteur Le collecteur à valider
     * @throws ValidationException Si le collecteur n'est pas valide
     * @throws DuplicateResourceException Si des ressources sont dupliquées
     */
    public void validateCollecteur(Collecteur collecteur) {
        if (collecteur == null) {
            throw new ValidationException("Le collecteur ne peut pas être null");
        }

        // Utiliser la méthode validate existante et convertir les erreurs en exceptions
        Errors errors = new BeanPropertyBindingResult(collecteur, "collecteur");
        validate(collecteur, errors);

        if (errors.hasErrors()) {
            FieldError firstError = errors.getFieldErrors().get(0);
            String message = firstError.getDefaultMessage();

            // Gérer les erreurs de duplication spécifiquement
            if (firstError.getCode() != null && firstError.getCode().contains("duplique")) {
                throw new DuplicateResourceException(message);
            }

            throw new ValidationException(message);
        }
    }

    // ================================
    // MÉTHODES UTILITAIRES POUR BigDecimal
    // ================================

    /**
     * Valide un montant BigDecimal dans une plage donnée
     */
    public boolean isValidAmount(BigDecimal amount, BigDecimal min, BigDecimal max) {
        if (amount == null) return false;
        return amount.compareTo(min) >= 0 && amount.compareTo(max) <= 0;
    }

    /**
     * Formate un montant BigDecimal pour les messages d'erreur
     */
    public String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f FCFA", amount.doubleValue());
    }
}