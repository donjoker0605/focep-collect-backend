package org.example.collectfocep.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ✅ CORRECTION: Extension de CommissionTier avec méthodes manquantes
 * Entité pour les paliers de commission (type TIER)
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

    // ✅ MÉTHODES MÉTIER EXISTANTES
    @PrePersist
    @PreUpdate
    private void validate() {
        if (montantMin != null && montantMax != null && montantMin >= montantMax) {
            throw new IllegalArgumentException(
                    String.format("Le montant minimum (%.2f) doit être inférieur au montant maximum (%.2f)",
                            montantMin, montantMax));
        }
    }

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

    // ✅ NOUVELLES MÉTHODES MANQUANTES POUR TON CODE

    /**
     * Valide si ce palier est cohérent
     * Utilisée dans ClientController
     */
    public boolean isValid() {
        if (montantMin == null || montantMax == null || taux == null) {
            return false;
        }

        if (montantMin < 0 || taux < 0 || taux > 100) {
            return false;
        }

        if (montantMax <= montantMin) {
            return false;
        }

        return true;
    }

    /**
     * Vérifie si ce palier chevauche avec un autre
     * Utilisée dans ClientController pour validation
     */
    public boolean overlapsWith(CommissionTier other) {
        if (other == null) return false;
        if (this.montantMin == null || this.montantMax == null) return false;
        if (other.montantMin == null || other.montantMax == null) return false;

        // Deux intervalles [a,b] et [c,d] se chevauchent si :
        // max(a,c) < min(b,d)
        double maxStart = Math.max(this.montantMin, other.montantMin);
        double minEnd = Math.min(this.montantMax, other.montantMax);

        return maxStart < minEnd;
    }

    /**
     * Vérifie si ce palier est adjacent à un autre (pour validation continuité)
     */
    public boolean isAdjacentTo(CommissionTier other) {
        if (other == null) return false;
        if (this.montantMax == null || other.montantMin == null) return false;

        // Tolérance pour les comparaisons de doubles
        double tolerance = 0.01;
        return Math.abs(this.montantMax - other.montantMin) <= tolerance;
    }

    /**
     * Compare ce palier avec un autre pour le tri
     */
    public int compareByRange(CommissionTier other) {
        if (other == null) return 1;
        if (this.montantMin == null && other.montantMin == null) return 0;
        if (this.montantMin == null) return -1;
        if (other.montantMin == null) return 1;

        return Double.compare(this.montantMin, other.montantMin);
    }

    /**
     * Calcule la commission pour un montant donné dans ce palier
     */
    public java.math.BigDecimal calculateCommissionFor(java.math.BigDecimal montant) {
        if (montant == null || !isApplicableFor(montant)) {
            return java.math.BigDecimal.ZERO;
        }

        return montant.multiply(java.math.BigDecimal.valueOf(taux))
                .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Version double pour compatibilité
     */
    public Double calculateCommissionFor(Double montant) {
        if (montant == null || !isApplicableFor(montant)) {
            return 0.0;
        }

        return (montant * taux) / 100.0;
    }

    /**
     * Représentation textuelle pour debugging
     */
    @Override
    public String toString() {
        return String.format("CommissionTier{id=%d, range=[%.2f-%.2f], taux=%.2f%%}",
                id, montantMin, montantMax, taux);
    }

    /**
     * Égalité basée sur la plage de montants
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommissionTier)) return false;

        CommissionTier that = (CommissionTier) o;
        return Double.compare(that.montantMin, montantMin) == 0 &&
                Double.compare(that.montantMax, montantMax) == 0 &&
                Double.compare(that.taux, taux) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(montantMin, montantMax, taux);
    }
}