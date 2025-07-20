package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "commission_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CommissionTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "montant_min", nullable = false)
    private Double montantMin; // Changé de double vers Double pour cohérence

    // 🔥 CORRECTION: Permettre que montantMax soit null pour les paliers "illimités"
    @Column(name = "montant_max", nullable = true)
    private Double montantMax; // Changé de double vers Double et nullable = true

    @Column(nullable = false)
    private Double taux; // Changé de double vers Double pour cohérence

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_parameter_id")
    private CommissionParameter commissionParameter;

    // Méthodes utilitaires

    /**
     * Vérifie si ce palier est applicable pour un montant donné
     */
    public boolean isApplicableFor(Double montant) {
        if (montant == null) return false;

        boolean minOk = montant >= montantMin;
        boolean maxOk = montantMax == null || montant <= montantMax;

        return minOk && maxOk;
    }

    /**
     * Vérifie si ce palier chevauche avec un autre
     */
    public boolean overlapsWith(CommissionTier other) {
        if (other == null) return false;

        // Si l'un des paliers n'a pas de max, on doit vérifier différemment
        if (this.montantMax == null && other.montantMax == null) {
            // Deux paliers illimités ne peuvent coexister
            return true;
        }

        if (this.montantMax == null) {
            // Ce palier est illimité, il chevauche si l'autre commence avant son minimum
            return other.montantMin < this.montantMin ||
                    (other.montantMax != null && other.montantMax > this.montantMin);
        }

        if (other.montantMax == null) {
            // L'autre palier est illimité
            return this.montantMin < other.montantMin || this.montantMax > other.montantMin;
        }

        // Cas standard: aucun des deux n'est illimité
        return this.montantMin < other.montantMax && this.montantMax > other.montantMin;
    }

    /**
     * Représentation textuelle du palier
     */
    public String getRangeDescription() {
        if (montantMax == null) {
            return String.format("%.0f FCFA et plus", montantMin);
        }
        return String.format("%.0f - %.0f FCFA", montantMin, montantMax);
    }

    /**
     * Validation de la cohérence du palier
     */
    public boolean isValid() {
        if (montantMin == null || montantMin < 0) return false;
        if (taux == null || taux < 0 || taux > 100) return false;
        if (montantMax != null && montantMax <= montantMin) return false;

        return true;
    }
}