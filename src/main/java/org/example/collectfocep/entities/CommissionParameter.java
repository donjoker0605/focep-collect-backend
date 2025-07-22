package org.example.collectfocep.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "commission_parameter")
public class CommissionParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @NotNull(message = "Le type de commission est requis")
    private CommissionType type;

    @Column(name = "valeur", precision = 15, scale = 4)
    @DecimalMin(value = "0.0", message = "La valeur ne peut pas être négative")
    private BigDecimal valeur;

    @Column(name = "code_produit", length = 50)
    private String codeProduit;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Version
    private Long version;

    // Relations hiérarchiques (Client > Collecteur > Agence)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id")
    private Collecteur collecteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private Agence agence;

    @OneToMany(mappedBy = "commissionParameter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CommissionTier> tiers;

    // MÉTHODES DE VALIDATION MÉTIER

    /**
     * Valide la cohérence du paramètre selon son type
     */
    @PrePersist
    @PreUpdate
    private void validateParameter() {
        validateScope();
        validateValueByType();
        validateDates();
    }

    /**
     * Validation scope correcte avec Boolean wrapper
     */
    private void validateScope() {
        int scopeCount = 0;
        if (client != null) scopeCount++;
        if (collecteur != null) scopeCount++;
        if (agence != null) scopeCount++;

        if (scopeCount == 0) {
            throw new IllegalArgumentException("Au moins une relation (client, collecteur, ou agence) doit être définie");
        }
        if (scopeCount > 1) {
            throw new IllegalArgumentException("Un seul scope doit être défini (client OU collecteur OU agence)");
        }
    }

    /**
     * Validation avec BigDecimal
     */
    private void validateValueByType() {
        if (type == null) {
            throw new IllegalArgumentException("Le type de commission est requis");
        }

        switch (type) {
            case FIXED -> {
                if (valeur == null || valeur.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Une valeur positive est requise pour le type FIXED");
                }
            }
            case PERCENTAGE -> {
                if (valeur == null || valeur.compareTo(BigDecimal.ZERO) <= 0 ||
                        valeur.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("Le pourcentage doit être entre 0 et 100");
                }
            }
            case TIER -> {
                if (tiers == null || tiers.isEmpty()) {
                    throw new IllegalArgumentException("Au moins un palier est requis pour le type TIER");
                }
                validateTiers();
            }
        }
    }

    /**
     * Valide les dates de validité
     */
    private void validateDates() {
        if (validFrom != null && validTo != null && validFrom.isAfter(validTo)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin");
        }

        if (validFrom == null) {
            validFrom = LocalDate.now();
        }
    }

    /**
     * Valide les paliers pour le type TIER
     */
    private void validateTiers() {
        if (tiers == null || tiers.isEmpty()) return;

        // Trier par montant minimum
        tiers.sort((t1, t2) -> Double.compare(t1.getMontantMin(), t2.getMontantMin()));

        // Vérifier continuité et non-chevauchement
        for (int i = 0; i < tiers.size(); i++) {
            CommissionTier current = tiers.get(i);

            // Validation palier individuel
            if (current.getMontantMin() >= current.getMontantMax()) {
                throw new IllegalArgumentException(
                        String.format("Palier invalide: min (%.2f) doit être < max (%.2f)",
                                current.getMontantMin(), current.getMontantMax()));
            }

            // Vérification chevauchement avec le suivant
            if (i < tiers.size() - 1) {
                CommissionTier next = tiers.get(i + 1);
                if (current.getMontantMax() > next.getMontantMin()) {
                    throw new IllegalArgumentException(
                            String.format("Chevauchement entre paliers [%.2f-%.2f] et [%.2f-%.2f]",
                                    current.getMontantMin(), current.getMontantMax(),
                                    next.getMontantMin(), next.getMontantMax()));
                }
            }
        }
    }

    // MÉTHODES UTILITAIRES

    /**
     * Détermine le scope du paramètre
     */
    public String getScope() {
        if (client != null) return "CLIENT";
        if (collecteur != null) return "COLLECTEUR";
        if (agence != null) return "AGENCE";
        return "UNDEFINED";
    }

    /**
     * Vérifie si le paramètre est actuellement valide
     */
    public boolean isCurrentlyValid() {
        if (!Boolean.TRUE.equals(active)) return false;

        LocalDate today = LocalDate.now();
        if (validFrom != null && today.isBefore(validFrom)) return false;
        if (validTo != null && today.isAfter(validTo)) return false;

        return true;
    }

    /**
     * Obtient l'entité associée selon le scope
     */
    public Object getScopeEntity() {
        if (client != null) return client;
        if (collecteur != null) return collecteur;
        if (agence != null) return agence;
        return null;
    }

    /**
     * Obtient l'ID de l'entité associée
     */
    public Long getScopeEntityId() {
        if (client != null) return client.getId();
        if (collecteur != null) return collecteur.getId();
        if (agence != null) return agence.getId();
        return null;
    }

    /**
     * Trouve le palier applicable pour un montant donné (type TIER)
     */
    public CommissionTier findApplicableTier(BigDecimal montant) {
        if (type != CommissionType.TIER || tiers == null) {
            return null;
        }

        return tiers.stream()
                .filter(tier -> montant.compareTo(BigDecimal.valueOf(tier.getMontantMin())) >= 0 &&
                        montant.compareTo(BigDecimal.valueOf(tier.getMontantMax())) <= 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * Méthodes de compatibilité pour transition douce
     */
    @Deprecated(forRemoval = true)
    public double getValeurAsDouble() {
        return valeur != null ? valeur.doubleValue() : 0.0;
    }

    @Deprecated(forRemoval = true)
    public void setValeurFromDouble(double valeur) {
        this.valeur = BigDecimal.valueOf(valeur);
    }

    /**
     * Builder personnalisé pour faciliter création
     */
    public static class CommissionParameterBuilder {
        public CommissionParameterBuilder valeurFromDouble(double valeur) {
            this.valeur = BigDecimal.valueOf(valeur);
            return this;
        }

        public CommissionParameterBuilder valeurFromString(String valeur) {
            this.valeur = new BigDecimal(valeur);
            return this;
        }
    }

    // FACTORY METHODS pour création rapide

    public static CommissionParameter createFixedCommission(BigDecimal montantFixe) {
        return CommissionParameter.builder()
                .type(CommissionType.FIXED)
                .valeur(montantFixe)
                .active(true)
                .build();
    }

    public static CommissionParameter createPercentageCommission(BigDecimal pourcentage) {
        return CommissionParameter.builder()
                .type(CommissionType.PERCENTAGE)
                .valeur(pourcentage)
                .active(true)
                .build();
    }

    public static CommissionParameter createTierCommission(List<CommissionTier> tiers) {
        return CommissionParameter.builder()
                .type(CommissionType.TIER)
                .tiers(tiers)
                .active(true)
                .build();
    }
}