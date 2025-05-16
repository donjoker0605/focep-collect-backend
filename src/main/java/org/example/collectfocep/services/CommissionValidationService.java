package org.example.collectfocep.services;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionTier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class CommissionValidationService {

    public ValidationResult validateCommissionParameters(CommissionParameter parameter) {
        log.debug("Validation du paramètre de commission: {}", parameter.getId());

        var result = ValidationResult.success();

        // Validation du type
        if (parameter.getType() == null) {
            return ValidationResult.error("Le type de commission est requis");
        }

        // Validation du scope
        result = result.and(validateScope(parameter));
        if (!result.isValid()) return result;

        // Validation selon le type
        switch (parameter.getType()) {
            case FIXED -> result = result.and(validateFixedCommission(parameter));
            case PERCENTAGE -> result = result.and(validatePercentageCommission(parameter));
            case TIER -> result = result.and(validateTierCommission(parameter));
            default -> result = ValidationResult.error("Type de commission non supporté: " + parameter.getType());
        }

        // Validation des dates
        result = result.and(validateDates(parameter));

        log.debug("Résultat validation: {}", result.isValid() ? "SUCCESS" : "FAILED");
        return result;
    }

    private ValidationResult validateScope(CommissionParameter parameter) {
        int scopeCount = 0;
        if (parameter.getClient() != null) scopeCount++;
        if (parameter.getCollecteur() != null) scopeCount++;
        if (parameter.getAgence() != null) scopeCount++;

        if (scopeCount == 0) {
            return ValidationResult.error("Un scope doit être défini (client, collecteur, ou agence)");
        }

        if (scopeCount > 1) {
            return ValidationResult.error("Un seul scope peut être défini à la fois");
        }

        return ValidationResult.success();
    }

    private ValidationResult validateFixedCommission(CommissionParameter parameter) {
        // Utilisation de Double wrapper au lieu de double primitif
        Double valeur = parameter.getValeur();
        if (valeur == null || valeur <= 0) {
            return ValidationResult.error("Le montant fixe doit être supérieur à zéro");
        }

        // Un montant fixe ne doit pas être trop élevé (limite de sécurité)
        if (valeur > 1000000) {
            return ValidationResult.warning("Montant fixe très élevé: " + valeur);
        }

        return ValidationResult.success();
    }

    private ValidationResult validatePercentageCommission(CommissionParameter parameter) {
        // Utilisation de Double wrapper au lieu de double primitif
        Double valeur = parameter.getValeur();
        if (valeur == null || valeur < 0 || valeur > 100) {
            return ValidationResult.error("Le pourcentage doit être compris entre 0 et 100");
        }

        // Avertissement pour des taux très élevés
        if (valeur > 20) {
            return ValidationResult.warning("Taux de commission très élevé: " + valeur + "%");
        }

        return ValidationResult.success();
    }

    private ValidationResult validateTierCommission(CommissionParameter parameter) {
        List<CommissionTier> tiers = parameter.getTiers();

        if (tiers == null || tiers.isEmpty()) {
            return ValidationResult.error("Les paliers sont requis pour ce type de commission");
        }

        // Tri par montant minimum
        tiers.sort(Comparator.comparing(CommissionTier::getMontantMin));

        // Validation de chaque palier
        for (int i = 0; i < tiers.size(); i++) {
            CommissionTier tier = tiers.get(i);

            // Validation des bornes (les champs sont double primitifs, pas de vérification null)
            if (tier.getMontantMin() >= tier.getMontantMax()) {
                return ValidationResult.error(
                        String.format("Palier %d invalide [%.2f-%.2f]: minimum doit être inférieur au maximum",
                                i+1, tier.getMontantMin(), tier.getMontantMax())
                );
            }

            // Validation du taux (double primitif)
            if (tier.getTaux() < 0 || tier.getTaux() > 100) {
                return ValidationResult.error(
                        String.format("Palier %d: le taux doit être compris entre 0 et 100 (actuel: %.2f)",
                                i+1, tier.getTaux())
                );
            }

            // Validation de la continuité
            if (i > 0) {
                CommissionTier previous = tiers.get(i-1);

                // Vérification du chevauchement (stricte)
                if (previous.getMontantMax() > tier.getMontantMin()) {
                    return ValidationResult.error(
                            String.format("Chevauchement détecté entre paliers %d et %d: [%.2f-%.2f] et [%.2f-%.2f]",
                                    i, i+1,
                                    previous.getMontantMin(), previous.getMontantMax(),
                                    tier.getMontantMin(), tier.getMontantMax())
                    );
                }

                // Avertissement pour les gaps (optionnel selon votre métier)
                if (previous.getMontantMax() < tier.getMontantMin() - 1) {
                    return ValidationResult.warning(
                            String.format("Gap détecté entre paliers %d et %d: [%.2f-%.2f] -> [%.2f-%.2f]",
                                    i, i+1,
                                    previous.getMontantMin(), previous.getMontantMax(),
                                    tier.getMontantMin(), tier.getMontantMax())
                    );
                }
            }

            // Le premier palier doit commencer à 0
            if (i == 0 && tier.getMontantMin() > 0) {
                return ValidationResult.warning("Le premier palier ne commence pas à 0: " + tier.getMontantMin());
            }
        }

        return ValidationResult.success();
    }

    private ValidationResult validateDates(CommissionParameter parameter) {
        LocalDate debut = parameter.getValidFrom();
        LocalDate fin = parameter.getValidTo();

        if (debut != null && fin != null && debut.isAfter(fin)) {
            return ValidationResult.error("La date de début doit être antérieure à la date de fin");
        }

        // Vérification que la période n'est pas dans le passé
        if (fin != null && fin.isBefore(LocalDate.now())) {
            return ValidationResult.warning("La période de validité est dans le passé");
        }

        return ValidationResult.success();
    }

    // Classe interne pour les résultats de validation
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>(), new ArrayList<>());
        }

        public static ValidationResult error(String message) {
            List<String> errors = new ArrayList<>();
            errors.add(message);
            return new ValidationResult(false, errors, new ArrayList<>());
        }

        public static ValidationResult warning(String message) {
            List<String> warnings = new ArrayList<>();
            warnings.add(message);
            return new ValidationResult(true, new ArrayList<>(), warnings);
        }

        public ValidationResult and(ValidationResult other) {
            List<String> allErrors = new ArrayList<>(this.errors);
            allErrors.addAll(other.errors);

            List<String> allWarnings = new ArrayList<>(this.warnings);
            allWarnings.addAll(other.warnings);

            boolean combinedValid = this.valid && other.valid;

            return new ValidationResult(combinedValid, allErrors, allWarnings);
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }

        public void throwIfInvalid() {
            if (!valid) {
                throw new ValidationException(String.join("; ", errors));
            }
        }
    }
}