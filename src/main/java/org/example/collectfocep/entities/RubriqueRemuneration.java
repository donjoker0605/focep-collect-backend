package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Rubrique de rémunération selon spécification FOCEP
 * Une rubrique définit comment calculer Vi pour un ou plusieurs collecteurs
 */
@Entity
@Table(name = "rubrique_remuneration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class  RubriqueRemuneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeRubrique type;

    /**
     * Valeur de la rubrique :
     * - Si type = CONSTANT : montant fixe
     * - Si type = PERCENTAGE : pourcentage à appliquer sur S
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valeur;

    /**
     * Date à partir de laquelle la rubrique commence à être appliquée
     */
    @Column(name = "date_application", nullable = false)
    private LocalDate dateApplication;

    /**
     * Délai en jours de validité de la rubrique
     * null = indéfini (toujours valide)
     */
    @Column(name = "delai_jours")
    private Integer delaiJours;

    /**
     * IDs des collecteurs concernés par cette rubrique
     * Stocké en JSON pour simplicité
     */
    @ElementCollection
    @CollectionTable(name = "rubrique_collecteurs", joinColumns = @JoinColumn(name = "rubrique_id"))
    @Column(name = "collecteur_id")
    private List<Long> collecteurIds;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    public enum TypeRubrique {
        CONSTANT,    // Montant fixe
        PERCENTAGE   // Pourcentage de S
    }

    /**
     * Calcule Vi (valeur de la rubrique) en fonction de S (somme des commissions collecteur)
     */
    public BigDecimal calculateVi(BigDecimal S) {
        if (!isCurrentlyValid()) {
            return BigDecimal.ZERO;
        }

        return switch (type) {
            case CONSTANT -> valeur;
            case PERCENTAGE -> S.multiply(valeur).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        };
    }

    /**
     * Vérifie si la rubrique est actuellement valide (dans ses délais)
     */
    public boolean isCurrentlyValid() {
        if (!active) {
            return false;
        }

        LocalDate now = LocalDate.now();
        
        // Vérifier date d'application
        if (now.isBefore(dateApplication)) {
            return false;
        }

        // Vérifier délai si défini
        if (delaiJours != null) {
            LocalDate dateExpiration = dateApplication.plusDays(delaiJours);
            return !now.isAfter(dateExpiration);
        }

        return true; // Pas de délai = toujours valide
    }

    /**
     * Vérifie si cette rubrique s'applique à un collecteur donné
     */
    public boolean appliesToCollecteur(Long collecteurId) {
        return collecteurIds != null && collecteurIds.contains(collecteurId);
    }
}