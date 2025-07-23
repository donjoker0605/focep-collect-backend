package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les demandes de modification du montant maximum de retrait
 * CORRECTION: Utilise BigDecimal pour cohérence avec l'entité Collecteur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MontantMaxRetraitRequest {

    @NotNull(message = "Le nouveau montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal nouveauMontant;

    @NotNull(message = "La justification est obligatoire")
    private String justification;

    // ================================
    // MÉTHODES DE COMPATIBILITÉ (TRANSITION)
    // ================================

    /**
     * Méthode de compatibilité pour retourner le montant en Double
     * À utiliser uniquement si nécessaire pour la transition
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public Double getNouveauMontantAsDouble() {
        return nouveauMontant != null ? nouveauMontant.doubleValue() : null;
    }

    /**
     * Méthode de compatibilité pour définir le montant depuis Double
     * À utiliser uniquement pour la transition
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void setNouveauMontantFromDouble(Double montant) {
        this.nouveauMontant = montant != null ? BigDecimal.valueOf(montant) : null;
    }

    // ================================
    // MÉTHODES UTILITAIRES
    // ================================

    /**
     * Valide que le montant est dans une plage acceptable
     */
    public boolean isValidAmount() {
        if (nouveauMontant == null) return false;

        // Montant doit être entre 1,000 et 10,000,000 FCFA
        BigDecimal minAmount = BigDecimal.valueOf(1000);
        BigDecimal maxAmount = BigDecimal.valueOf(10_000_000);

        return nouveauMontant.compareTo(minAmount) >= 0 &&
                nouveauMontant.compareTo(maxAmount) <= 0;
    }

    /**
     * Retourne le montant formaté pour affichage
     */
    public String getFormattedAmount() {
        if (nouveauMontant == null) return "0";
        return String.format("%,.0f FCFA", nouveauMontant.doubleValue());
    }
}