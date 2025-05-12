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
public class RapportCommission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "collecteur_id")
    private Collecteur collecteur;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    @OneToMany(mappedBy = "rapport", cascade = CascadeType.ALL)
    private List<Commission> commissions;

    private double totalCommissions;
    private double totalTVA;
    private double remunerationCollecteur;
    private double partEMF;
    private double tvaSurPartEMF;
    private boolean estValide;
}