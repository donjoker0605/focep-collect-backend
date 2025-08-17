package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 🔥 NOUVELLE ENTITÉ: Historique des calculs de commission
 * 
 * Objectif: Empêcher les doublons de calcul sur une même période
 * Exigence métier: Un collecteur ne peut avoir qu'un seul calcul par période
 */
@Entity
@Table(name = "historique_calcul_commission", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"collecteur_id", "date_debut", "date_fin"},
           name = "uk_collecteur_periode"
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
@EqualsAndHashCode
public class HistoriqueCalculCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Collecteur concerné par le calcul
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "agence", "journaux", "clients"})
    private Collecteur collecteur;

    /**
     * Période de calcul - Date début
     */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /**
     * Période de calcul - Date fin
     */
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    /**
     * Montant total des commissions calculées (S collecteur)
     */
    @Column(name = "montant_commission_total", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal montantCommissionTotal = BigDecimal.ZERO;

    /**
     * Montant total TVA calculée
     */
    @Column(name = "montant_tva_total", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal montantTvaTotal = BigDecimal.ZERO;

    /**
     * Nombre de clients ayant eu des commissions
     */
    @Column(name = "nombre_clients", nullable = false)
    @Builder.Default
    private Integer nombreClients = 0;

    /**
     * Statut du calcul
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutCalcul statut = StatutCalcul.CALCULE;

    /**
     * Détails du calcul (JSON stockant les commissions par client)
     */
    @Column(name = "details_calcul", columnDefinition = "TEXT")
    private String detailsCalcul;

    /**
     * Date et heure du calcul
     */
    @CreationTimestamp
    @Column(name = "date_calcul", nullable = false)
    private LocalDateTime dateCalcul;

    /**
     * Utilisateur ayant effectué le calcul
     */
    @Column(name = "calcule_par", length = 100)
    private String calculePar;

    /**
     * Agence du collecteur au moment du calcul
     */
    @Column(name = "agence_id", nullable = false)
    private Long agenceId;

    /**
     * Indique si ce calcul a déjà été utilisé pour une rémunération
     */
    @Column(name = "remunere", nullable = false)
    @Builder.Default
    private Boolean remunere = false;

    /**
     * Date de rémunération si applicable
     */
    @Column(name = "date_remuneration")
    private LocalDateTime dateRemuneration;

    /**
     * ID de la rémunération associée
     */
    @Column(name = "remuneration_id")
    private Long remunerationId;

    /**
     * Version pour optimistic locking
     */
    @Version
    private Long version;

    // =====================================
    // MÉTHODES UTILITAIRES
    // =====================================

    /**
     * Retourne une description de la période
     */
    public String getPeriodeDescription() {
        return String.format("%s → %s", dateDebut, dateFin);
    }

    /**
     * Vérifie si le calcul peut être utilisé pour rémunération
     */
    public boolean peutEtreRemunere() {
        return StatutCalcul.CALCULE.equals(statut) && !Boolean.TRUE.equals(remunere);
    }

    /**
     * Marque le calcul comme rémunéré
     */
    public void marquerCommeRemunere(Long remunerationId) {
        this.remunere = true;
        this.remunerationId = remunerationId;
        this.dateRemuneration = LocalDateTime.now();
    }

    /**
     * Calcule le total avec TVA
     */
    public BigDecimal getMontantTotalAvecTva() {
        return montantCommissionTotal.add(montantTvaTotal);
    }

    /**
     * Enum pour les statuts de calcul
     */
    public enum StatutCalcul {
        CALCULE("Calculé"),
        REMUNERE("Rémunéré"), 
        ANNULE("Annulé"),
        EN_COURS("En cours de calcul");

        private final String libelle;

        StatutCalcul(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }
}