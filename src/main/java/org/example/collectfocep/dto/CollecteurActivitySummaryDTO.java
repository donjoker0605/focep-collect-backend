package org.example.collectfocep.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 📊 DTO pour le résumé d'activité d'un collecteur
 *
 * USAGE :
 * - Dashboard de supervision admin
 * - Vue d'ensemble des performances collecteurs
 * - Détection rapide des problèmes
 *
 * DONNÉES INCLUSES :
 * - Informations de base du collecteur
 * - Métriques d'activité sur une période
 * - Statut et indicateurs visuels
 * - Score de performance calculé
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollecteurActivitySummaryDTO {

    // =====================================
    // INFORMATIONS COLLECTEUR
    // =====================================

    /**
     * ID unique du collecteur
     */
    private Long collecteurId;

    /**
     * Nom complet du collecteur (nom + prénom)
     */
    private String collecteurNom;

    /**
     * Nom de l'agence du collecteur
     */
    private String agenceNom;

    /**
     * ID de l'agence (pour filtrage/navigation)
     */
    private Long agenceId;

    // =====================================
    // MÉTRIQUES D'ACTIVITÉ
    // =====================================

    /**
     * Nombre total d'activités sur la période analysée
     */
    @Builder.Default
    private Integer totalActivites = 0;

    /**
     * Nombre de jours avec au moins une activité
     */
    @Builder.Default
    private Integer joursActifs = 0;

    /**
     * Nombre d'activités critiques détectées
     */
    @Builder.Default
    private Integer activitesCritiques = 0;

    /**
     * Activités par jour (moyenne calculée)
     */
    public Double getActivitesParJour() {
        if (joursActifs == null || joursActifs == 0) return 0.0;
        return (double) totalActivites / joursActifs;
    }

    /**
     * Pourcentage d'activités critiques
     */
    public Double getPourcentageCritiques() {
        if (totalActivites == null || totalActivites == 0) return 0.0;
        return (double) activitesCritiques * 100 / totalActivites;
    }

    // =====================================
    // STATUT ET INDICATEURS
    // =====================================

    /**
     * Statut du collecteur : ACTIF, ATTENTION, INACTIF, ERREUR
     */
    private String statut;

    /**
     * Couleur associée au statut (format hex) pour l'UI
     * - ACTIF: #4CAF50 (vert)
     * - ATTENTION: #FF9800 (orange)
     * - INACTIF: #F44336 (rouge)
     * - ERREUR: #9E9E9E (gris)
     */
    private String couleurStatut;

    /**
     * Icône emoji associée au statut pour l'UI
     * - ACTIF: ✅
     * - ATTENTION: ⚠️
     * - INACTIF: 🔴
     * - ERREUR: ❓
     */
    private String iconeStatut;

    /**
     * Score d'activité calculé (0-100)
     * Basé sur : volume d'activité, régularité, nombre d'anomalies
     */
    @Builder.Default
    private Integer scoreActivite = 0;

    // =====================================
    // DONNÉES TEMPORELLES
    // =====================================

    /**
     * Horodatage de la dernière activité enregistrée
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime derniereActivite;

    /**
     * Temps écoulé depuis la dernière activité (format lisible)
     */
    public String getTempsDepuisDerniereActivite() {
        if (derniereActivite == null) return "Aucune activité";

        LocalDateTime maintenant = LocalDateTime.now();
        java.time.Duration duree = java.time.Duration.between(derniereActivite, maintenant);

        long jours = duree.toDays();
        long heures = duree.toHours() % 24;
        long minutes = duree.toMinutes() % 60;

        if (jours > 0) {
            return jours + " jour" + (jours > 1 ? "s" : "");
        } else if (heures > 0) {
            return heures + " heure" + (heures > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return "À l'instant";
        }
    }

    /**
     * Dernière activité formatée pour l'affichage
     */
    public String getDerniereActiviteFormatee() {
        if (derniereActivite == null) return "N/A";
        return derniereActivite.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
    }

    // =====================================
    // MÉTHODES UTILITAIRES UI
    // =====================================

    /**
     * Indique si le collecteur nécessite une attention particulière
     */
    public Boolean getNecessiteAttention() {
        return "ATTENTION".equals(statut) || "INACTIF".equals(statut) ||
                (activitesCritiques != null && activitesCritiques > 0);
    }

    /**
     * Niveau de priorité pour le tri (1 = plus prioritaire)
     * INACTIF = 1, ATTENTION = 2, ACTIF = 3, ERREUR = 4
     */
    public Integer getNiveauPriorite() {
        switch (statut != null ? statut : "ERREUR") {
            case "INACTIF": return 1;
            case "ATTENTION": return 2;
            case "ACTIF": return 3;
            default: return 4;
        }
    }

    /**
     * Message descriptif du statut pour l'UI
     */
    public String getMessageStatut() {
        switch (statut != null ? statut : "ERREUR") {
            case "ACTIF":
                return "Collecteur actif et performant";
            case "ATTENTION":
                return "Attention requise - Activités suspectes détectées";
            case "INACTIF":
                return "Collecteur inactif depuis plus de 24h";
            case "ERREUR":
                return "Erreur lors de l'analyse des données";
            default:
                return "Statut inconnu";
        }
    }

    /**
     * Classe CSS suggérée pour l'affichage du statut
     */
    public String getClasseCssStatut() {
        switch (statut != null ? statut : "ERREUR") {
            case "ACTIF": return "status-success";
            case "ATTENTION": return "status-warning";
            case "INACTIF": return "status-danger";
            default: return "status-secondary";
        }
    }

    // =====================================
    // MÉTHODES DE CALCUL AVANCÉES
    // =====================================

    /**
     * Score visuel de performance (étoiles de 1 à 5)
     */
    public Integer getEtoilesPerformance() {
        if (scoreActivite == null) return 1;

        if (scoreActivite >= 90) return 5;
        if (scoreActivite >= 75) return 4;
        if (scoreActivite >= 60) return 3;
        if (scoreActivite >= 40) return 2;
        return 1;
    }

    /**
     * Emoji de performance basé sur le score
     */
    public String getEmojiPerformance() {
        Integer etoiles = getEtoilesPerformance();
        switch (etoiles) {
            case 5: return "🌟";
            case 4: return "⭐";
            case 3: return "✨";
            case 2: return "🔸";
            default: return "🔹";
        }
    }

    /**
     * Indicateur de tendance d'activité
     */
    private String tendanceActivite; // CROISSANTE, STABLE, DÉCROISSANTE

    public String getIconeTendance() {
        switch (tendanceActivite != null ? tendanceActivite : "STABLE") {
            case "CROISSANTE": return "📈";
            case "DÉCROISSANTE": return "📉";
            default: return "➡️";
        }
    }

    // =====================================
    // DONNÉES ÉTENDUES (optionnelles)
    // =====================================

    /**
     * Répartition des types d'activités (pour graphiques)
     */
    private Map<String, Integer> repartitionActivites;

    /**
     * Heures d'activité préférées du collecteur
     */
    private Map<Integer, Integer> heuresActivites;

    /**
     * Nombre de clients uniques touchés sur la période
     */
    private Integer clientsUniques;

    /**
     * Montant total des transactions gérées (si applicable)
     */
    private Double montantTransactions;

    // =====================================
    // MÉTHODES BUILDER PERSONNALISÉES
    // =====================================

    /**
     * Builder avec statut automatique basé sur les métriques
     */
    public static CollecteurActivitySummaryDTO.CollecteurActivitySummaryDTOBuilder withAutoStatus() {
        return CollecteurActivitySummaryDTO.builder();
    }

    /**
     * Applique automatiquement le statut, la couleur et l'icône basés sur les métriques
     */
    public void appliquerStatutAutomatique() {
        if (totalActivites == null || totalActivites == 0) {
            this.statut = "INACTIF";
            this.couleurStatut = "#F44336";
            this.iconeStatut = "🔴";
        } else if (activitesCritiques != null && activitesCritiques > 0 &&
                getPourcentageCritiques() > 10) {
            this.statut = "ATTENTION";
            this.couleurStatut = "#FF9800";
            this.iconeStatut = "⚠️";
        } else {
            this.statut = "ACTIF";
            this.couleurStatut = "#4CAF50";
            this.iconeStatut = "✅";
        }
    }

    // =====================================
    // MÉTHODES UTILITAIRES DE COMPARAISON
    // =====================================

    /**
     * Compare avec un autre résumé par score de performance
     */
    public int compareByPerformance(CollecteurActivitySummaryDTO other) {
        return Integer.compare(
                other.getScoreActivite() != null ? other.getScoreActivite() : 0,
                this.scoreActivite != null ? this.scoreActivite : 0
        );
    }

    /**
     * Compare par priorité (collecteurs nécessitant une attention en premier)
     */
    public int compareByPriority(CollecteurActivitySummaryDTO other) {
        return Integer.compare(this.getNiveauPriorite(), other.getNiveauPriorite());
    }

    /**
     * Compare par dernière activité (plus récent en premier)
     */
    public int compareByLastActivity(CollecteurActivitySummaryDTO other) {
        if (this.derniereActivite == null && other.derniereActivite == null) return 0;
        if (this.derniereActivite == null) return 1;
        if (other.derniereActivite == null) return -1;
        return other.derniereActivite.compareTo(this.derniereActivite);
    }

    // =====================================
    // VALIDATION ET COHÉRENCE
    // =====================================

    /**
     * Valide la cohérence des données du résumé
     */
    public boolean isValid() {
        return collecteurId != null &&
                collecteurNom != null && !collecteurNom.trim().isEmpty() &&
                agenceNom != null && !agenceNom.trim().isEmpty() &&
                totalActivites != null && totalActivites >= 0 &&
                joursActifs != null && joursActifs >= 0 &&
                activitesCritiques != null && activitesCritiques >= 0 &&
                scoreActivite != null && scoreActivite >= 0 && scoreActivite <= 100;
    }

    /**
     * Corrige automatiquement les valeurs incohérentes
     */
    public void corriger() {
        if (totalActivites == null || totalActivites < 0) totalActivites = 0;
        if (joursActifs == null || joursActifs < 0) joursActifs = 0;
        if (activitesCritiques == null || activitesCritiques < 0) activitesCritiques = 0;
        if (scoreActivite == null || scoreActivite < 0) scoreActivite = 0;
        if (scoreActivite > 100) scoreActivite = 100;

        // Cohérence : pas plus d'activités critiques que d'activités totales
        if (activitesCritiques > totalActivites) {
            activitesCritiques = totalActivites;
        }
    }

    @Override
    public String toString() {
        return String.format("CollecteurSummary{id=%d, nom='%s', agence='%s', activités=%d, statut='%s', score=%d}",
                collecteurId, collecteurNom, agenceNom, totalActivites, statut, scoreActivite);
    }
}