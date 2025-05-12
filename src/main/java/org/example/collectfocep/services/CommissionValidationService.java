package org.example.collectfocep.services;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionTier;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class CommissionValidationService {

    public void validateCommissionParameters(CommissionParameter parameter) {
        // Vérifier le type de commission
        if (parameter.getType() == null) {
            throw new ValidationException("Le type de commission est requis");
        }

        switch (parameter.getType()) {
            case FIXED:
                validateFixedCommission(parameter);
                break;
            case PERCENTAGE:
                validatePercentageCommission(parameter);
                break;
            case TIER:
                validateTierCommission(parameter);
                break;
            default:
                throw new ValidationException("Type de commission non supporté");
        }
    }

    private void validateFixedCommission(CommissionParameter parameter) {
        if (parameter.getValeur() <= 0) {
            throw new ValidationException("Le montant fixe doit être supérieur à zéro");
        }
    }

    private void validatePercentageCommission(CommissionParameter parameter) {
        if (parameter.getValeur() < 0 || parameter.getValeur() > 100) {
            throw new ValidationException("Le pourcentage doit être compris entre 0 et 100");
        }
    }

    private void validateTierCommission(CommissionParameter parameter) {
        List<CommissionTier> tiers = parameter.getTiers();
        if (tiers == null || tiers.isEmpty()) {
            throw new ValidationException("Les paliers sont requis pour ce type de commission");
        }

        // Vérifier que les paliers ne se chevauchent pas et sont continus
        tiers.sort(Comparator.comparing(CommissionTier::getMontantMin));

        for (int i = 0; i < tiers.size(); i++) {
            CommissionTier tier = tiers.get(i);

            // Vérifier les bornes
            if (tier.getMontantMin() >= tier.getMontantMax()) {
                throw new ValidationException("Le montant minimum doit être inférieur au montant maximum");
            }

            // Vérifier que le taux est positif
            if (tier.getTaux() < 0 || tier.getTaux() > 100) {
                throw new ValidationException("Le taux doit être compris entre 0 et 100");
            }

            // Vérifier la continuité et l'absence de chevauchement
            if (i > 0) {
                CommissionTier previous = tiers.get(i-1);
                if (previous.getMontantMax() >= tier.getMontantMin()) {
                    throw new ValidationException("Les paliers se chevauchent");
                }

                if (previous.getMontantMax() + 1 != tier.getMontantMin()) {
                    throw new ValidationException("Il y a une discontinuité entre les paliers");
                }
            }
        }
    }
}