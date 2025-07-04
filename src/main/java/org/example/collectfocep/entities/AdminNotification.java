package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notifications", indexes = {
        @Index(name = "idx_admin_unread", columnList = "admin_id, lu, date_creation"),
        @Index(name = "idx_collecteur_type", columnList = "collecteur_id, type, date_creation"),
        @Index(name = "idx_agence_date", columnList = "agence_id, date_creation"),
        @Index(name = "idx_priority_date", columnList = "priority, date_creation")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "collecteur_id", nullable = false)
    private Long collecteurId;

    @Column(name = "agence_id", nullable = false)
    private Long agenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "entity_id")
    private Long entityId; // ID de l'entité concernée (client, mouvement, etc.)

    @Column(name = "entity_type", length = 50)
    private String entityType; // Type d'entité (CLIENT, MOUVEMENT, etc.)

    @Column(name = "data", columnDefinition = "JSON")
    private String data; // Données contextuelles JSON

    @Column(name = "lu", nullable = false)
    private Boolean lu = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent = false;

    // NOUVEAU: Pour groupement des notifications
    @Column(name = "grouped_count", nullable = false)
    private Integer groupedCount = 1;

    @Column(name = "last_occurrence")
    private LocalDateTime lastOccurrence;

    // Méthodes utilitaires
    public boolean isUrgent() {
        return Priority.CRITIQUE.equals(this.priority);
    }

    public boolean isUnread() {
        return !Boolean.TRUE.equals(this.lu);
    }

    public void markAsRead() {
        this.lu = true;
        this.dateLecture = LocalDateTime.now();
    }

    public void markEmailSent() {
        this.emailSent = true;
    }
}