package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 📊 Traçabilité des collectes quotidiennes AVANT remise à zéro
 *
 * CORRECTION: Utilisation de BigDecimal avec precision/scale
 * OU suppression des attributs precision/scale avec Double
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

    // === UTILISATION DE BIGDECIMAL POUR COHÉRENCE AVEC LA BASE ===
    @Column(name = "solde_compte_service_avant_cloture", precision = 15, scale = 2)
    private BigDecimal soldeCompteServiceAvantCloture;

    @Column(name = "solde_compte_manquant_avant_cloture", precision = 15, scale = 2)
    private BigDecimal soldeCompteManquantAvantCloture;

    @Column(name = "total_epargne_jour", precision = 15, scale = 2)
    private BigDecimal totalEpargneJour;

    @Column(name = "total_retraits_jour", precision = 15, scale = 2)
    private BigDecimal totalRetraitsJour;

    @Column(name = "nombre_operations_jour")
    private Integer nombreOperationsJour;

    @Column(name = "nombre_clients_servis")
    private Integer nombreClientsServis;

    @Column(name = "cloture_effectuee")
    @Builder.Default
    private Boolean clotureEffectuee = false;

    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "cree_par")
    private String creePar;

    // === MÉTHODES UTILITAIRES MISES À JOUR ===

    /**
     * Calcule le solde net de la journée (Option 1: BigDecimal)
     */
    public BigDecimal getSoldeNetJour() {
        BigDecimal epargne = totalEpargneJour != null ? totalEpargneJour : BigDecimal.ZERO;
        BigDecimal retraits = totalRetraitsJour != null ? totalRetraitsJour : BigDecimal.ZERO;
        return epargne.subtract(retraits);
    }

    /**
     * Factory method pour créer depuis le journal
     */
    public static TraceabiliteCollecteQuotidienne creerDepuisJournal(
            Journal journal,
            BigDecimal soldeCompteService,
            BigDecimal soldeCompteManquant,
            BigDecimal totalEpargne,
            BigDecimal totalRetraits,
            Integer nombreOperations,
            Integer nombreClients,
            String creePar) {

        return TraceabiliteCollecteQuotidienne.builder()
                .journal(journal)
                .collecteur(journal.getCollecteur())
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
     * Marque la traçabilité comme clôturée
     */
    public void marquerCommeClôturee() {
        this.clotureEffectuee = true;
    }

    /**
     * Vérifie si la traçabilité est cohérente
     */
    public boolean isCoherente() {
        return soldeCompteServiceAvantCloture != null
                && totalEpargneJour != null
                && totalRetraitsJour != null
                && nombreOperationsJour != null
                && nombreOperationsJour >= 0;
    }
}