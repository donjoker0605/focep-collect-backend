package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.example.collectfocep.entities.CommissionTier;
import org.example.collectfocep.entities.CommissionType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

/**
 * UTILISATION :
 * - Frontend envoie {"montant": 100000, "type": "PERCENTAGE", "valeur": 5}
 * - Backend calcule la commission et retourne CommissionResult
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSimulationRequest {

    /**
     * Montant de base pour le calcul de commission
     * Correspond au montant d'épargne collecté
     */
    @NotNull(message = "Le montant est requis")
    @Positive(message = "Le montant doit être positif")
    @DecimalMin(value = "1.0", message = "Le montant minimum est 1 FCFA")
    private BigDecimal montant;

    /**
     * Type de commission à calculer
     */
    @NotNull(message = "Le type de commission est requis")
    private CommissionType type;

    /**
     * Valeur pour les types FIXED et PERCENTAGE
     * - Pour FIXED : montant fixe en FCFA
     * - Pour PERCENTAGE : pourcentage (ex: 5 pour 5%)
     */
    @DecimalMin(value = "0.0", message = "La valeur ne peut pas être négative")
    private BigDecimal valeur;

    /**
     * Liste des paliers pour le type TIER
     * Utilisé uniquement si type = TIER
     */
    private List<CommissionTier> tiers;

    // =====================================
    // MÉTHODES DE VALIDATION PERSONNALISÉES
    // =====================================

    /**
     * Valide que les champs requis sont présents selon le type
     */
    public boolean isValidForType() {
        return switch (type) {
            case FIXED, PERCENTAGE -> valeur != null && valeur.compareTo(BigDecimal.ZERO) > 0;
            case TIER -> tiers != null && !tiers.isEmpty();
        };
    }

    /**
     * Retourne un message d'erreur si la validation échoue
     */
    public String getValidationError() {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            return "Montant invalide ou manquant";
        }

        if (type == null) {
            return "Type de commission requis";
        }

        return switch (type) {
            case FIXED -> {
                if (valeur == null || valeur.compareTo(BigDecimal.ZERO) <= 0) {
                    yield "Valeur fixe requise et doit être positive";
                }
                if (valeur.compareTo(BigDecimal.valueOf(1000000)) > 0) {
                    yield "Valeur fixe trop élevée (maximum 1,000,000 FCFA)";
                }
                yield null;
            }

            case PERCENTAGE -> {
                if (valeur == null || valeur.compareTo(BigDecimal.ZERO) <= 0) {
                    yield "Pourcentage requis et doit être positif";
                }
                if (valeur.compareTo(BigDecimal.valueOf(100)) > 0) {
                    yield "Pourcentage ne peut pas dépasser 100%";
                }
                yield null;
            }

            case TIER -> {
                if (tiers == null || tiers.isEmpty()) {
                    yield "Au moins un palier requis pour le type TIER";
                }
                // Validation des paliers
                for (CommissionTier tier : tiers) {
                    if (tier.getMontantMin() == null || tier.getMontantMax() == null || tier.getTaux() == null) {
                        yield "Palier incomplet détecté";
                    }
                    if (tier.getMontantMin() >= tier.getMontantMax()) {
                        yield "Montant minimum doit être inférieur au maximum dans les paliers";
                    }
                    if (tier.getTaux() < 0 || tier.getTaux() > 100) {
                        yield "Taux des paliers doit être entre 0 et 100%";
                    }
                }
                yield null;
            }
        };
    }

    // =====================================
    // MÉTHODES UTILITAIRES
    // =====================================

    /**
     * Retourne la valeur formatée pour l'affichage
     */
    public String getFormattedValeur() {
        if (valeur == null) return "N/A";

        return switch (type) {
            case FIXED -> String.format("%,.0f FCFA", valeur);
            case PERCENTAGE -> String.format("%.2f%%", valeur);
            case TIER -> String.format("%d palier(s)", tiers != null ? tiers.size() : 0);
        };
    }

    /**
     * Retourne une description lisible de la simulation
     */
    public String getDescription() {
        if (montant == null || type == null) {
            return "Simulation incomplète";
        }

        String montantStr = String.format("%,.0f FCFA", montant);

        return switch (type) {
            case FIXED -> String.format("Commission fixe de %s sur montant de %s",
                    getFormattedValeur(), montantStr);
            case PERCENTAGE -> String.format("Commission de %s sur montant de %s",
                    getFormattedValeur(), montantStr);
            case TIER -> String.format("Commission par paliers (%d paliers) sur montant de %s",
                    tiers != null ? tiers.size() : 0, montantStr);
        };
    }

    // =====================================
    // CONSTRUCTEURS DE CONVENANCE
    // =====================================

    /**
     * Constructeur pour commission fixe
     */
    public static CommissionSimulationRequest forFixed(BigDecimal montant, BigDecimal montantFixe) {
        return CommissionSimulationRequest.builder()
                .montant(montant)
                .type(CommissionType.FIXED)
                .valeur(montantFixe)
                .build();
    }

    /**
     * Constructeur pour commission pourcentage
     */
    public static CommissionSimulationRequest forPercentage(BigDecimal montant, BigDecimal pourcentage) {
        return CommissionSimulationRequest.builder()
                .montant(montant)
                .type(CommissionType.PERCENTAGE)
                .valeur(pourcentage)
                .build();
    }

    /**
     * Constructeur pour commission par paliers
     */
    public static CommissionSimulationRequest forTier(BigDecimal montant, List<CommissionTier> tiers) {
        return CommissionSimulationRequest.builder()
                .montant(montant)
                .type(CommissionType.TIER)
                .tiers(tiers)
                .build();
    }
}