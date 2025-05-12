package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemunerationCollecteur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "collecteur_id")
    private Collecteur collecteur;

    private double montantFixe;
    private double totalCommissions;
    private double montantRemuneration;
    private double montantTVA;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private boolean estPaye;

    @ManyToOne
    @JoinColumn(name = "compte_collecteur_id")
    private CompteCollecteur compteCollecteur;
}