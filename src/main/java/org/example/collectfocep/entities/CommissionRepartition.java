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
public class CommissionRepartition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "collecteur_id")
    private Collecteur collecteur;

    private double montantTotalCommission;
    private double montantTVAClient;
    private double partCollecteur;
    private double partEMF;
    private double tvaSurPartEMF;
    private LocalDateTime dateRepartition;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "repartition_id")
    private List<Mouvement> mouvements;
}