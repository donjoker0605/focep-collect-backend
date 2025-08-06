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

    
    /**
     * Méthode de compatibilité pour récupérer la valeur personnalisée
     * (alias pour getValeur())
     */
    public java.math.BigDecimal getValeurPersonnalisee() {
        return java.math.BigDecimal.valueOf(this.valeur);

    }
}