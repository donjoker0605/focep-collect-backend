package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
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

    private double montant;
    private double tva;
    private String type;
    private double valeur;

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
}