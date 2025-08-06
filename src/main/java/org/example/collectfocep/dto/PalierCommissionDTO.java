package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les paliers de commission
 * Alias de CommissionTierDTO pour compatibilité
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PalierCommissionDTO {

    private Long id;

    @NotNull(message = "Le montant minimum est requis")
    @DecimalMin(value = "0.0", message = "Le montant minimum doit être positif")
    private Double montantMin;

    @NotNull(message = "Le montant maximum est requis")
    @DecimalMin(value = "1.0", message = "Le montant maximum doit être positif")
    private Double montantMax;

    @NotNull(message = "Le taux est requis")
    @DecimalMin(value = "0.0", message = "Le taux doit être positif")
    @DecimalMax(value = "100.0", message = "Le taux ne peut pas dépasser 100%")
    private Double taux;

    private String description;

    // Validation personnalisée
    public boolean isValid() {
        if (montantMin == null || montantMax == null || taux == null) {
            return false;
        }

        return montantMin >= 0 &&
                montantMax > montantMin &&
                taux >= 0 &&
                taux <= 100;
    }

    // Méthodes utilitaires
    public boolean isApplicableFor(Double montant) {
        if (montant == null) return false;
        return montant >= montantMin && montant <= montantMax;
    }

    public boolean isApplicableFor(BigDecimal montant) {
        if (montant == null) return false;
        return isApplicableFor(montant.doubleValue());
    }

    public String getRangeDescription() {
        return String.format("%.2f - %.2f FCFA (%.2f%%)", montantMin, montantMax, taux);
    }

    public boolean overlappsWith(PalierCommissionDTO other) {
        if (other == null) return false;

        return (this.montantMin < other.montantMax && this.montantMax > other.montantMin);
    }

    public BigDecimal calculateCommission(BigDecimal montant) {
        if (!isApplicableFor(montant)) {
            return BigDecimal.ZERO;
        }

        return montant.multiply(BigDecimal.valueOf(taux))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    // Conversion vers CommissionTierDTO
    public CommissionTierDTO toCommissionTierDTO() {
        return CommissionTierDTO.builder()
                .id(this.id)
                .montantMin(this.montantMin)
                .montantMax(this.montantMax)
                .taux(this.taux)
                .build();
    }

    // Factory method depuis CommissionTierDTO
    public static PalierCommissionDTO fromCommissionTierDTO(CommissionTierDTO tierDTO) {
        return PalierCommissionDTO.builder()
                .id(tierDTO.getId())
                .montantMin(tierDTO.getMontantMin())
                .montantMax(tierDTO.getMontantMax())
                .taux(tierDTO.getTaux())
                .build();
    }

    @Override
    public String toString() {
        return String.format("PalierCommission{range=[%.2f-%.2f], taux=%.2f%%}",
                montantMin, montantMax, taux);
    }
}