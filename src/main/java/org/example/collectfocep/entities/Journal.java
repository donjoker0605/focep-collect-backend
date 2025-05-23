package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Journal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_collecteur", nullable = false)
    private Collecteur collecteur;

    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Mouvement> mouvements = new ArrayList<>();

    @Column(name = "est_cloture", nullable = false)
    @Builder.Default
    private boolean estCloture = false;

    // ✅ AJOUT DU CHAMP STATUT MANQUANT
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private String statut = "OUVERT"; // Valeur par défaut

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Version
    private Long version;

    // ✅ CORRECTION DU GETTER - maintenant le champ existe
    public String getStatut() {
        return this.statut;
    }

    // ✅ MÉTHODE UTILITAIRE POUR CALCULER LE STATUT BASÉ SUR estCloture
    public String getStatutCalcule() {
        return this.estCloture ? "CLOTURE" : "OUVERT";
    }

    public void addMouvement(Mouvement mouvement) {
        mouvements.add(mouvement);
        mouvement.setJournal(this);
    }

    public void removeMouvement(Mouvement mouvement) {
        mouvements.remove(mouvement);
        mouvement.setJournal(null);
    }

    // MÉTHODE POUR CLÔTURER LE JOURNAL
    public void cloturerJournal() {
        this.estCloture = true;
        this.statut = "CLOTURE";
        this.dateCloture = LocalDateTime.now();
    }

    // MÉTHODE POUR OUVRIR LE JOURNAL
    public void ouvrirJournal() {
        this.estCloture = false;
        this.statut = "OUVERT";
        this.dateCloture = null;
    }
}