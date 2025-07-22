package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "commissions", indexes = {
        @Index(name = "idx_commission_client", columnList = "client_id"),
        @Index(name = "idx_commission_collecteur", columnList = "collecteur_id"),
        @Index(name = "idx_commission_date_calcul", columnList = "date_calcul"),
        @Index(name = "idx_commission_type", columnList = "type")
})
public class Commission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id", nullable = false)
    private Collecteur collecteur;

    @Column(name = "montant", precision = 15, scale = 2, nullable = false)
    private BigDecimal montant;

    @Column(name = "tva", precision = 15, scale = 2, nullable = false)
    private BigDecimal tva;

    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "valeur", precision = 15, scale = 4)
    private BigDecimal valeur;

    @Column(name = "date_calcul", nullable = false)
    @Builder.Default
    private LocalDateTime dateCalcul = LocalDateTime.now();

    @Column(name = "date_fin_validite")
    private LocalDateTime dateFinValidite;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id")
    private Compte compte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_parameter_id")
    private CommissionParameter commissionParameter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rapport_id")
    private RapportCommission rapport;

    public BigDecimal getMontantTotal() {
        if (montant == null && tva == null) return BigDecimal.ZERO;
        if (montant == null) return tva;
        if (tva == null) return montant;
        return montant.add(tva);
    }

    public BigDecimal getMontantNet() {
        if (montant == null && tva == null) return BigDecimal.ZERO;
        if (montant == null) return tva.negate();
        if (tva == null) return montant;
        return montant.subtract(tva);
    }

    public boolean isExpired() {
        return dateFinValidite != null && dateFinValidite.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return !isExpired() && montant != null && montant.compareTo(BigDecimal.ZERO) >= 0;
    }

    @Deprecated(forRemoval = true)
    public double getMontantAsDouble() {
        return montant != null ? montant.doubleValue() : 0.0;
    }

    @Deprecated(forRemoval = true)
    public double getTvaAsDouble() {
        return tva != null ? tva.doubleValue() : 0.0;
    }

    @Deprecated(forRemoval = true)
    public void setMontantFromDouble(double montant) {
        this.montant = BigDecimal.valueOf(montant);
    }

    @Deprecated(forRemoval = true)
    public void setTvaFromDouble(double tva) {
        this.tva = BigDecimal.valueOf(tva);
    }

    public static class CommissionBuilder {

        public CommissionBuilder montantFromDouble(double montant) {
            this.montant = BigDecimal.valueOf(montant);
            return this;
        }

        public CommissionBuilder tvaFromDouble(double tva) {
            this.tva = BigDecimal.valueOf(tva);
            return this;
        }

        public CommissionBuilder montantFromString(String montant) {
            if (montant != null && !montant.trim().isEmpty()) {
                this.montant = new BigDecimal(montant);
            }
            return this;
        }

        public CommissionBuilder tvaFromString(String tva) {
            if (tva != null && !tva.trim().isEmpty()) {
                this.tva = new BigDecimal(tva);
            }
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

    public static Commission createFixedCommission(Client client, Collecteur collecteur,
                                                   BigDecimal montantFixe, BigDecimal tva) {
        return Commission.builder()
                .client(client)
                .collecteur(collecteur)
                .montant(montantFixe)
                .tva(tva)
                .type("FIXED")
                .dateCalcul(LocalDateTime.now())
                .build();
    }

    public static Commission createPercentageCommission(Client client, Collecteur collecteur,
                                                        BigDecimal montantBase, BigDecimal pourcentage,
                                                        BigDecimal tva) {
        BigDecimal montantCommission = montantBase.multiply(pourcentage)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        return Commission.builder()
                .client(client)
                .collecteur(collecteur)
                .montant(montantCommission)
                .valeur(pourcentage)
                .tva(tva)
                .type("PERCENTAGE")
                .dateCalcul(LocalDateTime.now())
                .build();
    }

    @Override
    public String toString() {
        return String.format("Commission{id=%d, client=%s, type=%s, montant=%s, tva=%s, dateCalcul=%s}",
                id,
                client != null ? client.getNomComplet() : "null",
                type,
                montant,
                tva,
                dateCalcul);
    }
}