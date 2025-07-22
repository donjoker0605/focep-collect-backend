package org.example.collectfocep.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Entity pour les paliers de commission (type TIER)
 * Permet de définir des taux différents selon des tranches de montant
 */
@Entity
@Table(name = "commission_tier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "montant_min", nullable = false)
    @NotNull(message = "Le montant minimum est requis")
    @Min(value = 0, message = "Le montant minimum doit être positif")
    private Double montantMin;

    @Column(name = "montant_max", nullable = false)
    @NotNull(message = "Le montant maximum est requis")
    @Min(value = 1, message = "Le montant maximum doit être positif")
    private Double montantMax;

    @Column(name = "taux", nullable = false)
    @NotNull(message = "Le taux est requis")
    @DecimalMin(value = "0.0", message = "Le taux doit être positif")
    @DecimalMax(value = "100.0", message = "Le taux ne peut pas dépasser 100%")
    private Double taux;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_parameter_id", nullable = false)
    private CommissionParameter commissionParameter;

    @Version
    private Long version;

    // Validation métier
    @PrePersist
    @PreUpdate
    private void validate() {
        if (montantMin != null && montantMax != null && montantMin >= montantMax) {
            throw new IllegalArgumentException(
                    String.format("Le montant minimum (%.2f) doit être inférieur au montant maximum (%.2f)",
                            montantMin, montantMax));
        }
    }

    // Méthodes utilitaires
    public boolean isApplicableFor(Double montant) {
        if (montant == null) return false;
        return montant >= montantMin && montant <= montantMax;
    }

    public boolean isApplicableFor(java.math.BigDecimal montant) {
        if (montant == null) return false;
        return isApplicableFor(montant.doubleValue());
    }

    public String getRangeDescription() {
        return String.format("%.2f - %.2f FCFA (%.2f%%)", montantMin, montantMax, taux);
    }

    public boolean overlappsWith(CommissionTier other) {
        if (other == null) return false;

        return (this.montantMin < other.montantMax && this.montantMax > other.montantMin);
    }

    public java.math.BigDecimal calculateCommission(java.math.BigDecimal montant) {
        if (!isApplicableFor(montant)) {
            return java.math.BigDecimal.ZERO;
        }

        return montant.multiply(java.math.BigDecimal.valueOf(taux))
                .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommissionTier that = (CommissionTier) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("CommissionTier{id=%d, range=[%.2f-%.2f], taux=%.2f%%}",
                id, montantMin, montantMax, taux);
    }
}