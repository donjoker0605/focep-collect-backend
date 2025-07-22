package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "collecteur_id")
    private Collecteur collecteur;

    // BigDecimal au lieu de double
    @Column(name = "montant", precision = 15, scale = 2, nullable = false)
    private BigDecimal montant;

    // BigDecimal au lieu de double
    @Column(name = "tva", precision = 15, scale = 2, nullable = false)
    private BigDecimal tva;

    @Column(name = "type", length = 20)
    private String type;

    // BigDecimal pour cohérence
    @Column(name = "valeur", precision = 15, scale = 4)
    private BigDecimal valeur;

    @Column(name = "date_calcul")
    private LocalDateTime dateCalcul;

    @Column(name = "date_fin_validite")
    private LocalDateTime dateFinValidite;

    @OneToOne
    @JoinColumn(name = "compte_id")
    private Compte compte;

    @ManyToOne
    @JoinColumn(name = "commission_parameter_id")
    private CommissionParameter commissionParameter;

    @ManyToOne
    @JoinColumn(name = "rapport_id")
    private RapportCommission rapport;

    // Méthodes utilitaires pour calculs
    public BigDecimal getMontantTotal() {
        return montant != null && tva != null ? montant.add(tva) : BigDecimal.ZERO;
    }

    public BigDecimal getMontantNet() {
        return montant != null && tva != null ? montant.subtract(tva) : BigDecimal.ZERO;
    }

    // Méthodes de compatibilité pour transition douce
    @Deprecated(forRemoval = true)
    public double getMontantAsDouble() {
        return montant != null ? montant.doubleValue() : 0.0;
    }

    @Deprecated(forRemoval = true)
    public double getTvaAsDouble() {
        return tva != null ? tva.doubleValue() : 0.0;
    }

    // personnalisé pour faciliter création
    public static class CommissionBuilder {
        public CommissionBuilder montantFromDouble(double montant) {
            this.montant = BigDecimal.valueOf(montant);
            return this;
        }

        public CommissionBuilder tvaFromDouble(double tva) {
            this.tva = BigDecimal.valueOf(tva);
            return this;
        }
    }

    @PrePersist
    @PreUpdate
    private void validateAmounts() {
        if (montant != null && montant.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant de commission ne peut pas être négatif");
        }
        if (tva != null && tva.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La TVA ne peut pas être négative");
        }
        if (dateCalcul == null) {
            dateCalcul = LocalDateTime.now();
        }
    }
}