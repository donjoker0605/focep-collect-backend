package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transferts_compte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransfertCompte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_collecteur_id")
    private Long sourceCollecteurId;

    @Column(name = "target_collecteur_id")
    private Long targetCollecteurId;

    @Column(name = "date_transfert")
    private LocalDateTime dateTransfert;

    @Column(name = "nombre_comptes")
    private int nombreComptes;

    @Column(name = "montant_total")
    private double montantTotal;

    @Column(name = "montant_commissions")
    private double montantCommissions;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "is_inter_agence")
    private boolean interAgence;

    @Column(name = "statut")
    private String statut;

    @OneToMany(mappedBy = "transfert", cascade = CascadeType.ALL)
    private List<Mouvement> mouvements;

    // Méthodes personnalisées pour compatibilité avec le code existant
    public boolean getIsInterAgence() {
        return this.interAgence;
    }

    public void setIsInterAgence(boolean interAgence) {
        this.interAgence = interAgence;
    }
}