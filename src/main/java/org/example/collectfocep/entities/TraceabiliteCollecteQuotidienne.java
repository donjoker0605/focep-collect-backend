package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 📊 Traçabilité des collectes quotidiennes AVANT remise à zéro
 *
 * OBJECTIF: Conserver l'historique des montants collectés par jour par collecteur
 * même après la remise à zéro du compte service lors de la clôture
 */
@Entity
@Table(name = "tracabilite_collecte_quotidienne",
        uniqueConstraints = @UniqueConstraint(columnNames = {"collecteur_id", "date_collecte"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceabiliteCollecteQuotidienne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id", nullable = false)
    private Collecteur collecteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @Column(name = "date_collecte", nullable = false)
    private LocalDate dateCollecte;

    // === MONTANTS AVANT CLÔTURE ===
    @Column(name = "solde_compte_service_avant_cloture", precision = 15, scale = 2)
    private Double soldeCompteServiceAvantCloture;

    @Column(name = "solde_compte_manquant_avant_cloture", precision = 15, scale = 2)
    private Double soldeCompteManquantAvantCloture;

    // === DÉTAIL DES OPÉRATIONS DU JOUR ===
    @Column(name = "total_epargne_jour", precision = 15, scale = 2)
    private Double totalEpargneJour;

    @Column(name = "total_retraits_jour", precision = 15, scale = 2)
    private Double totalRetraitsJour;

    @Column(name = "nombre_operations_jour")
    private Integer nombreOperationsJour;

    @Column(name = "nombre_clients_servis")
    private Integer nombreClientsServis;

    // === INFORMATIONS CLÔTURE ===
    @Column(name = "cloture_effectuee")
    @Builder.Default
    private Boolean clotureEffectuee = false;

    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "cree_par")
    private String creePar;

    // === MÉTHODES UTILITAIRES ===

    /**
     * Calcule le solde net de la journée
     */
    public Double getSoldeNetJour() {
        double epargne = totalEpargneJour != null ? totalEpargneJour : 0.0;
        double retraits = totalRetraitsJour != null ? totalRetraitsJour : 0.0;
        return epargne - retraits;
    }

    /**
     * Vérifie la cohérence des données
     */
    public boolean isCoherent() {
        // Le solde compte service devrait être égal au solde net du jour
        // (sauf s'il y avait un solde antérieur)
        Double soldeNet = getSoldeNetJour();
        Double soldeCompte = soldeCompteServiceAvantCloture;

        if (soldeNet == 0 && soldeCompte == 0) return true;
        if (soldeNet == null || soldeCompte == null) return false;

        // Tolérance de 0.01 pour les erreurs d'arrondi
        return Math.abs(soldeNet - soldeCompte) < 0.01;
    }

    /**
     * Factory method pour créer une trace à partir d'un journal
     */
    public static TraceabiliteCollecteQuotidienne creerDepuisJournal(
            Journal journal,
            Double soldeCompteService,
            Double soldeCompteManquant,
            Double totalEpargne,
            Double totalRetraits,
            Integer nombreOperations,
            Integer nombreClients,
            String creePar) {

        return TraceabiliteCollecteQuotidienne.builder()
                .collecteur(journal.getCollecteur())
                .journal(journal)
                .dateCollecte(journal.getDateDebut())
                .soldeCompteServiceAvantCloture(soldeCompteService)
                .soldeCompteManquantAvantCloture(soldeCompteManquant)
                .totalEpargneJour(totalEpargne)
                .totalRetraitsJour(totalRetraits)
                .nombreOperationsJour(nombreOperations)
                .nombreClientsServis(nombreClients)
                .clotureEffectuee(false)
                .creePar(creePar)
                .build();
    }

    /**
     * Marque la trace comme clôturée
     */
    public void marquerCommeClôturee() {
        this.clotureEffectuee = true;
    }

    @Override
    public String toString() {
        return String.format("TraceabiliteCollecte[collecteur=%s, date=%s, soldeService=%.2f, epargne=%.2f, retraits=%.2f]",
                collecteur != null ? collecteur.getId() : "null",
                dateCollecte,
                soldeCompteServiceAvantCloture != null ? soldeCompteServiceAvantCloture : 0.0,
                totalEpargneJour != null ? totalEpargneJour : 0.0,
                totalRetraitsJour != null ? totalRetraitsJour : 0.0);
    }
}