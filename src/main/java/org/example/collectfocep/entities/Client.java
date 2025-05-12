package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String numeroCni;

    private String ville;
    private String quartier;
    private String telephone;
    private String photoPath;

    @Column(nullable = false)
    private boolean valide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_collecteur")
    private Collecteur collecteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agence")
    private Agence agence;

    // Getters/setters pour les relations par ID
    public Long getAgenceId() {
        return agence != null ? agence.getId() : null;
    }

    public void setAgenceId(Long agenceId) {
        if (agence == null) {
            agence = new Agence();
        }
        agence.setId(agenceId);
    }

    public Long getCollecteurId() {
        return collecteur != null ? collecteur.getId() : null;
    }

    public void setCollecteurId(Long collecteurId) {
        if (collecteur == null) {
            collecteur = new Collecteur();
        }
        collecteur.setId(collecteurId);
    }
}