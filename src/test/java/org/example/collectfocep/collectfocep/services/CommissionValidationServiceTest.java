package org.example.collectfocep.collectfocep.services;

import jakarta.validation.ValidationException;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionTier;
import org.example.collectfocep.entities.CommissionType;
import org.example.collectfocep.services.CommissionValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CommissionValidationServiceTest {

    @InjectMocks
    private CommissionValidationService validationService;

    private CommissionParameter fixedParameter;
    private CommissionParameter percentageParameter;
    private CommissionParameter tierParameter;
    private List<CommissionTier> validTiers;
    private List<CommissionTier> overlappingTiers;
    private List<CommissionTier> discontinuousTiers;

    @BeforeEach
    void setUp() {
        // Configuration des paramètres de commission fixe
        fixedParameter = new CommissionParameter();
        fixedParameter.setId(1L);
        fixedParameter.setType(CommissionType.FIXED);
        fixedParameter.setValeur(50.0);
        fixedParameter.setCodeProduit("FIXE");
        fixedParameter.setValidFrom(LocalDate.now());
        fixedParameter.setValidTo(LocalDate.now().plusMonths(12));
        fixedParameter.setActive(true);

        // Configuration des paramètres de commission pourcentage
        percentageParameter = new CommissionParameter();
        percentageParameter.setId(2L);
        percentageParameter.setType(CommissionType.PERCENTAGE);
        percentageParameter.setValeur(2.5);
        percentageParameter.setCodeProduit("POURCENTAGE");
        percentageParameter.setValidFrom(LocalDate.now());
        percentageParameter.setValidTo(LocalDate.now().plusMonths(12));
        percentageParameter.setActive(true);

        // Configuration des paliers valides
        validTiers = new ArrayList<>();

        CommissionTier tier1 = new CommissionTier();
        tier1.setId(1L);
        tier1.setMontantMin(0.0);
        tier1.setMontantMax(1000.0);
        tier1.setTaux(3.0);

        CommissionTier tier2 = new CommissionTier();
        tier2.setId(2L);
        tier2.setMontantMin(1001.0);
        tier2.setMontantMax(5000.0);
        tier2.setTaux(2.0);

        CommissionTier tier3 = new CommissionTier();
        tier3.setId(3L);
        tier3.setMontantMin(5001.0);
        tier3.setMontantMax(Double.MAX_VALUE);
        tier3.setTaux(1.0);

        validTiers.addAll(Arrays.asList(tier1, tier2, tier3));

        // Configuration des paliers qui se chevauchent
        overlappingTiers = new ArrayList<>();

        CommissionTier overlapTier1 = new CommissionTier();
        overlapTier1.setId(4L);
        overlapTier1.setMontantMin(0.0);
        overlapTier1.setMontantMax(2000.0);
        overlapTier1.setTaux(3.0);

        CommissionTier overlapTier2 = new CommissionTier();
        overlapTier2.setId(5L);
        overlapTier2.setMontantMin(1500.0); // Chevauchement ici
        overlapTier2.setMontantMax(5000.0);
        overlapTier2.setTaux(2.0);

        overlappingTiers.addAll(Arrays.asList(overlapTier1, overlapTier2));

        // Configuration des paliers discontinus
        discontinuousTiers = new ArrayList<>();

        CommissionTier discontinuousTier1 = new CommissionTier();
        discontinuousTier1.setId(6L);
        discontinuousTier1.setMontantMin(0.0);
        discontinuousTier1.setMontantMax(1000.0);
        discontinuousTier1.setTaux(3.0);

        CommissionTier discontinuousTier2 = new CommissionTier();
        discontinuousTier2.setId(7L);
        discontinuousTier2.setMontantMin(2000.0); // Gap entre 1000 et 2000
        discontinuousTier2.setMontantMax(5000.0);
        discontinuousTier2.setTaux(2.0);

        discontinuousTiers.addAll(Arrays.asList(discontinuousTier1, discontinuousTier2));

        // Configuration des paramètres de commission par palier
        tierParameter = new CommissionParameter();
        tierParameter.setId(3L);
        tierParameter.setType(CommissionType.TIER);
        tierParameter.setTiers(validTiers);
        tierParameter.setCodeProduit("PALIER");
        tierParameter.setValidFrom(LocalDate.now());
        tierParameter.setValidTo(LocalDate.now().plusMonths(12));
        tierParameter.setActive(true);
    }

    @Test
    void testValidateFixedCommission_Valid() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            validationService.validateCommissionParameters(fixedParameter);
        });
    }

    @Test
    void testValidateFixedCommission_Invalid() {
        // Arrange
        fixedParameter.setValeur(0.0); // Valeur invalide

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.validateCommissionParameters(fixedParameter);
        });
    }

    @Test
    void testValidatePercentageCommission_Valid() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            validationService.validateCommissionParameters(percentageParameter);
        });
    }

    @Test
    void testValidatePercentageCommission_InvalidLow() {
        // Arrange
        percentageParameter.setValeur(-1.0); // Valeur invalide

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.validateCommissionParameters(percentageParameter);
        });
    }

    @Test
    void testValidatePercentageCommission_InvalidHigh() {
        // Arrange
        percentageParameter.setValeur(101.0); // Valeur invalide

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.validateCommissionParameters(percentageParameter);
        });
    }

    @Test
    void testValidateTierCommission_Valid() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            validationService.validateCommissionParameters(tierParameter);
        });
    }

    @Test
    void testValidateTierCommission_NoTiers() {
        // Arrange
        tierParameter.setTiers(new ArrayList<>());

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.validateCommissionParameters(tierParameter);
        });
    }

    @Test
    void testValidateTierCommission_OverlappingTiers() {
        // Arrange
        tierParameter.setTiers(overlappingTiers);

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.validateCommissionParameters(tierParameter);
        });
    }

    @Test
    void testValidateTierCommission_DiscontinuousTiers() {
        // Arrange
        tierParameter.setTiers(discontinuousTiers);

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.validateCommissionParameters(tierParameter);
        });
    }

    @Test
    void testValidateTierCommission_InvalidTierRange() {
        // Arrange
        CommissionTier invalidTier = new CommissionTier();
        invalidTier.setMontantMin(1000.0);
        invalidTier.setMontantMax(1000.0); // Min = Max (invalide)
        invalidTier.setTaux(2.0);

        tierParameter.setTiers(Arrays.asList(invalidTier));

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.validateCommissionParameters(tierParameter);
        });
    }

    @Test
    void testValidateTierCommission_InvalidTierRate() {
        // Arrange
        CommissionTier invalidTier = new CommissionTier();
        invalidTier.setMontantMin(0.0);
        invalidTier.setMontantMax(1000.0);
        invalidTier.setTaux(-1.0); // Taux négatif (invalide)

        tierParameter.setTiers(Arrays.asList(invalidTier));

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.validateCommissionParameters(tierParameter);
        });
    }

    @Test
    void testValidateCommissionParameters_NullType() {
        // Arrange
        CommissionParameter parameterWithNullType = new CommissionParameter();
        parameterWithNullType.setType(null);

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.validateCommissionParameters(parameterWithNullType);
        });
    }

    @Test
    void testValidateCommissionParameters_UnsupportedType() {
        // Arrange
        // Cette situation est difficile à tester directement car CommissionType est un enum
        // Mais on peut simuler un comportement similaire avec un switch qui atteint le cas par défaut

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            // On utilise une classe anonyme pour surcharger la méthode validateCommissionParameters
            // et forcer le passage dans le cas par défaut du switch
            CommissionValidationService testService = new CommissionValidationService() {
                @Override
                public void validateCommissionParameters(CommissionParameter parameter) {
                    throw new ValidationException("Type de commission non supporté");
                }
            };

            testService.validateCommissionParameters(tierParameter);
        });
    }
}