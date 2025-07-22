package org.example.collectfocep.services;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionTier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        log.debug("Résultat validation: {}", result.isValid() ? "VALIDE" : "INVALIDE");
        return result;
    }

    private ValidationResult validateScope(CommissionParameter parameter) {
        int scopeCount = 0;
        if (parameter.getClient() != null) scopeCount++;
        if (parameter.getCollecteur() != null) scopeCount++;
        if (parameter.getAgence() != null) scopeCount++;

        if (scopeCount == 0) {
            return ValidationResult.error("Au moins une relation (client, collecteur, ou agence) doit être définie");
        }
        if (scopeCount > 1) {
            return ValidationResult.error("Un seul scope doit être défini");
        }
        return ValidationResult.success();
    }

    private ValidationResult validateFixedCommission(CommissionParameter parameter) {
        // Gérer BigDecimal au lieu de Double
        if (parameter.getValeur() == null || parameter.getValeur().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.error("Une valeur positive est requise pour le type FIXED");
        }

        // Validation plafond raisonnable
        if (parameter.getValeur().compareTo(BigDecimal.valueOf(1000000)) > 0) {
            return ValidationResult.warning("Montant fixe très élevé: " + parameter.getValeur());
        }

        return ValidationResult.success();
    }

    private ValidationResult validatePercentageCommission(CommissionParameter parameter) {
        // Gérer BigDecimal au lieu de Double
        if (parameter.getValeur() == null ||
                parameter.getValeur().compareTo(BigDecimal.ZERO) <= 0 ||
                parameter.getValeur().compareTo(BigDecimal.valueOf(100)) > 0) {
            return ValidationResult.error("Le pourcentage doit être entre 0 et 100");
        }

        // Warning pour pourcentages élevés
        if (parameter.getValeur().compareTo(BigDecimal.valueOf(20)) > 0) {
            return ValidationResult.warning("Pourcentage élevé: " + parameter.getValeur() + "%");
        }

        return ValidationResult.success();
    }

    private ValidationResult validateTierCommission(CommissionParameter parameter) {
        if (parameter.getTiers() == null || parameter.getTiers().isEmpty()) {
            return ValidationResult.error("Au moins un palier est requis pour le type TIER");
        }

        var result = ValidationResult.success();
        List<CommissionTier> sortedTiers = new ArrayList<>(parameter.getTiers());
        sortedTiers.sort(Comparator.comparing(CommissionTier::getMontantMin));

        // Validation de chaque palier
        for (CommissionTier tier : sortedTiers) {
            if (tier.getMontantMin() == null || tier.getMontantMax() == null || tier.getTaux() == null) {
                result = result.and(ValidationResult.error("Palier incomplet détecté"));
                continue;
            }

            if (tier.getMontantMin() >= tier.getMontantMax()) {
                result = result.and(ValidationResult.error(
                        String.format("Palier invalide: min (%.2f) >= max (%.2f)",
                                tier.getMontantMin(), tier.getMontantMax())));
            }

            if (tier.getTaux() < 0 || tier.getTaux() > 100) {
                result = result.and(ValidationResult.error(
                        String.format("Taux invalide: %.2f%% (doit être entre 0 et 100)", tier.getTaux())));
            }
        }

        // Validation des chevauchements
        for (int i = 0; i < sortedTiers.size() - 1; i++) {
            CommissionTier current = sortedTiers.get(i);
            CommissionTier next = sortedTiers.get(i + 1);

            if (current.getMontantMax() > next.getMontantMin()) {
                result = result.and(ValidationResult.error(
                        String.format("Chevauchement entre paliers [%.2f-%.2f] et [%.2f-%.2f]",
                                current.getMontantMin(), current.getMontantMax(),
                                next.getMontantMin(), next.getMontantMax())));
            }
        }

        return result;
    }

    private ValidationResult validateDates(CommissionParameter parameter) {
        if (parameter.getValidFrom() != null && parameter.getValidTo() != null) {
            if (parameter.getValidFrom().isAfter(parameter.getValidTo())) {
                return ValidationResult.error("La date de début doit être antérieure à la date de fin");
            }
        }

        // Warning pour paramètre expiré
        if (parameter.getValidTo() != null && parameter.getValidTo().isBefore(LocalDate.now())) {
            return ValidationResult.warning("Ce paramètre est expiré");
        }

        return ValidationResult.success();
    }

    // CLASSE INTERNE ValidationResult
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        }

        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>(), new ArrayList<>());
        }

        public static ValidationResult error(String error) {
            List<String> errors = new ArrayList<>();
            errors.add(error);
            return new ValidationResult(false, errors, new ArrayList<>());
        }

        public static ValidationResult warning(String warning) {
            List<String> warnings = new ArrayList<>();
            warnings.add(warning);
            return new ValidationResult(true, new ArrayList<>(), warnings);
        }

        public ValidationResult and(ValidationResult other) {
            boolean combinedValid = this.valid && other.valid;
            List<String> combinedErrors = new ArrayList<>(this.errors);
            combinedErrors.addAll(other.errors);
            List<String> combinedWarnings = new ArrayList<>(this.warnings);
            combinedWarnings.addAll(other.warnings);
            return new ValidationResult(combinedValid, combinedErrors, combinedWarnings);
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }

        public void throwIfInvalid() {
            if (!valid) {
                throw new ValidationException("Validation échouée: " + String.join(", ", errors));
            }
        }
    }
}