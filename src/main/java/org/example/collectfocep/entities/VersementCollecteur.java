package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "versements_collecteur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersementCollecteur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id", nullable = false)
    private Collecteur collecteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @Column(name = "date_versement", nullable = false)
    private LocalDate dateVersement;

    @Column(name = "montant_collecte", nullable = false)
    private Double montantCollecte;

    @Column(name = "montant_verse", nullable = false)
    private Double montantVerse;

    @Column(name = "excedent")
    private Double excedent;

    @Column(name = "manquant")
    private Double manquant;

    @Column(name = "numero_autorisation", unique = true)
    private String numeroAutorisation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutVersement statut;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "cree_par")
    private String creePar;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        if (numeroAutorisation == null) {
            numeroAutorisation = genererNumeroAutorisation();
        }
        calculerDifferences();
    }

    @PreUpdate
    protected void onUpdate() {
        calculerDifferences();
    }

    private void calculerDifferences() {
        if (montantCollecte != null && montantVerse != null) {
            double difference = montantVerse - montantCollecte;

            if (difference > 0) {
                excedent = difference;
                manquant = 0.0;
            } else if (difference < 0) {
                manquant = Math.abs(difference);
                excedent = 0.0;
            } else {
                excedent = 0.0;
                manquant = 0.0;
            }
        }
    }

    private String genererNumeroAutorisation() {
        return String.format("VERS-%d-%s-%06d",
                collecteur != null ? collecteur.getId() : 0,
                dateVersement != null ? dateVersement.toString().replace("-", "") : "00000000",
                System.currentTimeMillis() % 1000000);
    }

    public enum StatutVersement {
        EN_ATTENTE,
        VALIDE,
        ANNULE,
        REGULARISE
    }

    // Méthodes utilitaires
    public boolean hasExcedent() {
        return excedent != null && excedent > 0;
    }

    public boolean hasManquant() {
        return manquant != null && manquant > 0;
    }

    public boolean isEquilibre() {
        return !hasExcedent() && !hasManquant();
    }
}