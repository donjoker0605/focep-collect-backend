package org.example.collectfocep.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

import jakarta.persistence.*;

import java.sql.Types;
import java.time.LocalDateTime;

/**
 * 🔔 Entité pour les notifications destinées aux collecteurs
 *
 * TYPES DE NOTIFICATIONS :
 * - SYSTEM_ALERT : Alertes système critiques
 * - INFORMATION : Messages informatifs
 * - WARNING : Avertissements
 * - REMINDER : Rappels (clôture journal, etc.)
 * - ADMIN_MESSAGE : Messages des administrateurs
 *
 * PRIORITÉS :
 * - URGENT : Notification critique nécessitant une action immédiate
 * - HIGH : Priorité élevée
 * - NORMAL : Priorité normale
 * - LOW : Priorité faible
 *
 * CYCLE DE VIE :
 * 1. Création de la notification
 * 2. Envoi au collecteur (push/in-app)
 * 3. Lecture par le collecteur
 * 4. Expiration automatique après X jours
 * 5. Nettoyage automatique des notifications expirées
 */
@Entity
@Table(name = "collecteur_notifications", indexes = {
        @Index(name = "idx_collecteur_read", columnList = "collecteur_id, lu"),
        @Index(name = "idx_collecteur_priority", columnList = "collecteur_id, priorite"),
        @Index(name = "idx_date_creation", columnList = "date_creation"),
        @Index(name = "idx_date_expiration", columnList = "date_expiration")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollecteurNotification {

    // =====================================
    // IDENTIFIANT ET RÉFÉRENCE
    // =====================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID du collecteur destinataire
     */
    @Column(name = "collecteur_id", nullable = false)
    private Long collecteurId;

    /**
     * Référence optionnelle vers une entité liée (client, mouvement, etc.)
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Type d'entité référencée (CLIENT, MOUVEMENT, JOURNAL, etc.)
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    // =====================================
    // CLASSIFICATION
    // =====================================

    /**
     * Type de notification
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    /**
     * Priorité de la notification
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priorite", nullable = false, length = 20)
    private Priorite priorite;

    /**
     * Catégorie pour le filtrage/regroupement
     */
    @Column(name = "categorie", length = 50)
    private String categorie;

    // =====================================
    // CONTENU
    // =====================================

    /**
     * Titre de la notification (max 200 caractères)
     */
    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    /**
     * Message détaillé de la notification
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Données supplémentaires au format JSON
     * Exemple : {"montant": 50000, "clientNom": "Dupont", "action": "redirect"}
     */
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "donnees", columnDefinition = "JSON")
    private String donnees;

    /**
     * URL ou action à effectuer lors du clic (optionnel)
     */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /**
     * Icône à afficher avec la notification
     */
    @Column(name = "icone", length = 50)
    private String icone;

    /**
     * Couleur de la notification (format hex)
     */
    @Column(name = "couleur", length = 7)
    private String couleur;

    // =====================================
    // ÉTAT ET SUIVI
    // =====================================

    /**
     * Indique si la notification a été lue
     */
    @Column(name = "lu", nullable = false)
    @Builder.Default
    private Boolean lu = false;

    /**
     * Indique si la notification a été envoyée (push)
     */
    @Column(name = "envoye", nullable = false)
    @Builder.Default
    private Boolean envoye = false;

    /**
     * Nombre de tentatives d'envoi
     */
    @Column(name = "tentatives_envoi")
    @Builder.Default
    private Integer tentativesEnvoi = 0;

    /**
     * Erreur lors de l'envoi (si applicable)
     */
    @Column(name = "erreur_envoi", length = 500)
    private String erreurEnvoi;

    // =====================================
    // HORODATAGE
    // =====================================

    /**
     * Date et heure de création de la notification
     */
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation;

    /**
     * Date et heure de lecture par le collecteur
     */
    @Column(name = "date_lecture")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateLecture;

    /**
     * Date et heure d'envoi effectif
     */
    @Column(name = "date_envoi")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateEnvoi;

    /**
     * Date d'expiration de la notification
     * Après cette date, la notification peut être supprimée automatiquement
     */
    @Column(name = "date_expiration")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateExpiration;

    // =====================================
    // MÉTADONNÉES
    // =====================================

    /**
     * ID de l'administrateur qui a créé la notification (si applicable)
     */
    @Column(name = "admin_id")
    private Long adminId;

    /**
     * Nom de l'administrateur pour affichage
     */
    @Column(name = "admin_nom", length = 100)
    private String adminNom;

    /**
     * Indique si la notification peut être supprimée par l'utilisateur
     */
    @Column(name = "suppressible")
    @Builder.Default
    private Boolean suppressible = true;

    /**
     * Indique si la notification doit persister après lecture
     */
    @Column(name = "persistante")
    @Builder.Default
    private Boolean persistante = false;

    // =====================================
    // ÉNUMÉRATIONS
    // =====================================

    public enum NotificationType {
        SYSTEM_ALERT("Alerte système"),
        INFORMATION("Information"),
        WARNING("Avertissement"),
        REMINDER("Rappel"),
        ADMIN_MESSAGE("Message admin"),
        SOLDE_ALERT("Alerte solde"),
        JOURNAL_REMINDER("Rappel journal"),
        COMMISSION_INFO("Info commission"),
        CLIENT_UPDATE("Mise à jour client");

        private final String displayName;

        NotificationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Priorite {
        URGENT("Urgent", "#F44336", "🚨"),
        HIGH("Élevée", "#FF9800", "⚠️"),
        NORMAL("Normale", "#2196F3", "ℹ️"),
        LOW("Faible", "#9E9E9E", "💬");

        private final String displayName;
        private final String couleur;
        private final String icone;

        Priorite(String displayName, String couleur, String icone) {
            this.displayName = displayName;
            this.couleur = couleur;
            this.icone = icone;
        }

        public String getDisplayName() { return displayName; }
        public String getCouleur() { return couleur; }
        public String getIcone() { return icone; }
    }

    // =====================================
    // MÉTHODES UTILITAIRES
    // =====================================

    /**
     * Marque la notification comme lue
     */
    public void marquerCommeLue() {
        this.lu = true;
        this.dateLecture = LocalDateTime.now();
    }

    /**
     * Marque la notification comme envoyée
     */
    public void marquerCommeEnvoye() {
        this.envoye = true;
        this.dateEnvoi = LocalDateTime.now();
    }

    /**
     * Incrémente le compteur de tentatives d'envoi
     */
    public void incrementerTentativesEnvoi() {
        if (this.tentativesEnvoi == null) {
            this.tentativesEnvoi = 1;
        } else {
            this.tentativesEnvoi++;
        }
    }

    /**
     * Vérifie si la notification est expirée
     */
    public boolean isExpiree() {
        return dateExpiration != null && LocalDateTime.now().isAfter(dateExpiration);
    }

    /**
     * Vérifie si la notification est récente (moins de 24h)
     */
    public boolean isRecente() {
        return dateCreation != null &&
                dateCreation.isAfter(LocalDateTime.now().minusDays(1));
    }

    /**
     * Calcule l'âge de la notification en heures
     */
    public long getAgeEnHeures() {
        if (dateCreation == null) return 0;
        return java.time.Duration.between(dateCreation, LocalDateTime.now()).toHours();
    }

    /**
     * Retourne la couleur basée sur la priorité si pas définie explicitement
     */
    public String getCouleurEffective() {
        if (couleur != null && !couleur.trim().isEmpty()) {
            return couleur;
        }
        return priorite != null ? priorite.getCouleur() : Priorite.NORMAL.getCouleur();
    }

    /**
     * Retourne l'icône basée sur la priorité si pas définie explicitement
     */
    public String getIconeEffective() {
        if (icone != null && !icone.trim().isEmpty()) {
            return icone;
        }
        return priorite != null ? priorite.getIcone() : Priorite.NORMAL.getIcone();
    }

    /**
     * Détermine l'expiration automatique basée sur le type et la priorité
     */
    public void definirExpirationAutomatique() {
        if (dateExpiration != null) return; // Déjà définie

        LocalDateTime maintenant = LocalDateTime.now();

        // Durées d'expiration basées sur le type et la priorité
        switch (type) {
            case SYSTEM_ALERT:
                dateExpiration = maintenant.plusDays(priorite == Priorite.URGENT ? 3 : 7);
                break;
            case WARNING:
                dateExpiration = maintenant.plusDays(5);
                break;
            case REMINDER:
                dateExpiration = maintenant.plusDays(2);
                break;
            case ADMIN_MESSAGE:
                dateExpiration = maintenant.plusDays(30);
                break;
            case INFORMATION:
            default:
                dateExpiration = maintenant.plusDays(priorite == Priorite.URGENT ? 7 : 14);
                break;
        }
    }

    /**
     * Génère un résumé court pour les logs
     */
    public String getResume() {
        return String.format("[%s] %s - %s (Collecteur: %d)",
                priorite, type.getDisplayName(), titre, collecteurId);
    }

    public static CollecteurNotification createUrgent(Long collecteurId, String titre, String message) {
        return CollecteurNotification.builder()
                .collecteurId(collecteurId)
                .priorite(Priorite.URGENT)
                .type(NotificationType.SYSTEM_ALERT)
                .titre(titre)
                .message(message)
                .build();
    }

    public static CollecteurNotification createInfo(Long collecteurId, String titre, String message) {
        return CollecteurNotification.builder()
                .collecteurId(collecteurId)
                .priorite(Priorite.NORMAL)
                .type(NotificationType.INFORMATION)
                .titre(titre)
                .message(message)
                .build();
    }

    public static CollecteurNotification createRappel(Long collecteurId, String titre, String message) {
        return CollecteurNotification.builder()
                .collecteurId(collecteurId)
                .priorite(Priorite.HIGH)
                .type(NotificationType.REMINDER)
                .titre(titre)
                .message(message)
                .build();
    }

    // =====================================
    // CALLBACKS JPA
    // =====================================

    @PrePersist
    public void onPrePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }

        // Définir l'expiration automatique si pas définie
        definirExpirationAutomatique();

        // Valeurs par défaut
        if (lu == null) lu = false;
        if (envoye == null) envoye = false;
        if (suppressible == null) suppressible = true;
        if (persistante == null) persistante = false;
        if (tentativesEnvoi == null) tentativesEnvoi = 0;
    }

    @Override
    public String toString() {
        return String.format("CollecteurNotification{id=%d, collecteur=%d, type=%s, priorite=%s, titre='%s', lu=%s}",
                id, collecteurId, type, priorite, titre, lu);
    }

    public Long getCollecteurId() {
        return this.collecteurId;
    }

    public Boolean getSuppressible() {
        return this.suppressible;
    }

    public Long getId() {
        return this.id;
    }

    public void setErreurEnvoi(String erreurEnvoi) {
        this.erreurEnvoi = erreurEnvoi;
    }

    // =====================================
// MÉTHODES FACTORY BUILDER FLUIDES
// =====================================

    /**
     * Builder pour notification urgente
     */
    public static CollecteurNotificationBuilder urgent() {
        return CollecteurNotification.builder()
                .priorite(Priorite.URGENT)
                .type(NotificationType.SYSTEM_ALERT)
                .icone("🚨")
                .couleur("#F44336");
    }

    /**
     * Builder pour notification d'information
     */
    public static CollecteurNotificationBuilder info() {
        return CollecteurNotification.builder()
                .priorite(Priorite.NORMAL)
                .type(NotificationType.INFORMATION)
                .icone("ℹ️")
                .couleur("#2196F3");
    }

    /**
     * Builder pour notification de rappel
     */
    public static CollecteurNotificationBuilder rappel() {
        return CollecteurNotification.builder()
                .priorite(Priorite.HIGH)
                .type(NotificationType.REMINDER)
                .icone("⏰")
                .couleur("#FF9800");
    }

    /**
     * Builder pour message admin
     */
    public static CollecteurNotificationBuilder messageAdmin(Long adminId, String adminNom) {
        return CollecteurNotification.builder()
                .priorite(Priorite.NORMAL)
                .type(NotificationType.ADMIN_MESSAGE)
                .adminId(adminId)
                .adminNom(adminNom)
                .icone("📨")
                .couleur("#9C27B0")
                .persistante(true);
    }

    /**
     * Builder pour alerte système
     */
    public static CollecteurNotificationBuilder alerte() {
        return CollecteurNotification.builder()
                .priorite(Priorite.HIGH)
                .type(NotificationType.WARNING)
                .icone("⚠️")
                .couleur("#FF9800");
    }
}