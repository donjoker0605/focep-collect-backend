package org.example.collectfocep.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================
    // RELATIONS
    // =====================================

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "collecteur_id")
    private Long collecteurId;

    @Column(name = "agence_id")
    private Long agenceId;

    @Column(name = "entity_id")
    private Long entityId; // ID de l'entité concernée (Client, Mouvement, etc.)

    // Relations optionnelles pour récupération d'infos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"notifications", "agence"})
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"notifications", "clients", "agence"})
    private Collecteur collecteur;

    // =====================================
    // CONTENU NOTIFICATION
    // =====================================

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    // =====================================
    // MÉTADONNÉES
    // =====================================

    @Column(name = "data", columnDefinition = "JSON")
    private String data; // Données additionnelles au format JSON

    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @Column(name = "lu", nullable = false)
    @Builder.Default
    private Boolean lu = false;

    // =====================================
    // GROUPING ET PERFORMANCE
    // =====================================

    @Column(name = "grouped_count")
    @Builder.Default
    private Integer groupedCount = 1;

    // =====================================
    // EMAIL
    // =====================================

    @Column(name = "email_sent")
    @Builder.Default
    private Boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    // =====================================
    // MÉTHODES UTILITAIRES
    // =====================================

    public boolean isRead() {
        return Boolean.TRUE.equals(this.lu);
    }

    public boolean isCritical() {
        return Priority.CRITIQUE.equals(this.priority);
    }

    public boolean isEmailPending() {
        return !Boolean.TRUE.equals(this.emailSent) && this.isCritical();
    }

    public void markAsRead() {
        this.lu = true;
        this.dateLecture = LocalDateTime.now();
    }

    public void markEmailSent() {
        this.emailSent = true;
        this.emailSentAt = LocalDateTime.now();
    }

    public void incrementGroupedCount() {
        this.groupedCount = (this.groupedCount == null ? 0 : this.groupedCount) + 1;
    }

    public long getMinutesSinceCreation() {
        return java.time.Duration.between(this.dateCreation, LocalDateTime.now()).toMinutes();
    }
}