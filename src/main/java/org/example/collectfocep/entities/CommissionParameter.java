package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private CommissionType type;

    private double valeur;
    private String codeProduit;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "is_active")
    private boolean active;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "collecteur_id")
    private Collecteur collecteur;

    @ManyToOne
    @JoinColumn(name = "agence_id")
    private Agence agence;

    @OneToMany(mappedBy = "commissionParameter", cascade = CascadeType.ALL)
    private List<CommissionTier> tiers;
    
    /**
     * Méthode de compatibilité pour récupérer la valeur personnalisée
     * (alias pour getValeur())
     */
    public java.math.BigDecimal getValeurPersonnalisee() {
        return java.math.BigDecimal.valueOf(this.valeur);
    }
}