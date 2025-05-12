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
    private double montantMin;

    @Column(name = "montant_max", nullable = false)
    private double montantMax;

    @Column(nullable = false)
    private double taux;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_parameter_id")
    private CommissionParameter commissionParameter;
}
