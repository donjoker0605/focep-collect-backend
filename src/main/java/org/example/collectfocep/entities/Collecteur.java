package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "COLLECTEURS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Collecteur extends Utilisateur {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agence")
    private Agence agence;

    @Column(nullable = false)
    private int ancienneteEnMois;

    @Column(nullable = false)
    private Double montantMaxRetrait;

    @Column(name = "date_modification_montant")
    private LocalDateTime dateModificationMontantMax;

    @Column(name = "modifie_par")
    private String modifiePar;

    @ManyToOne
    @JoinColumn(name = "rapport_id")
    private RapportCommission rapport;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "collecteur")
    @Builder.Default
    private List<Client> clients = new ArrayList<>();

    @OneToMany(mappedBy = "collecteur", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CompteCollecteur> comptes = new ArrayList<>();

    // Méthodes pour ajouter/supprimer des comptes
    public void addCompte(CompteCollecteur compte) {
        comptes.add(compte);
        compte.setCollecteur(this);
    }

    public void removeCompte(CompteCollecteur compte) {
        comptes.remove(compte);
        compte.setCollecteur(null);
    }
}