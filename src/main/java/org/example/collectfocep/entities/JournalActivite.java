package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_activite", indexes = {
        @Index(name = "idx_user_id_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_agence_timestamp", columnList = "agence_id, timestamp"),
        @Index(name = "idx_action_timestamp", columnList = "action, timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType; // COLLECTEUR, ADMIN, SUPER_ADMIN

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // CREATE_CLIENT, MODIFY_CLIENT, LOGIN, LOGOUT, TRANSACTION

    @Column(name = "entity_type", length = 50)
    private String entityType; // CLIENT, MOUVEMENT, COMMISSION, COLLECTEUR

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON des données modifiées

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "agence_id")
    private Long agenceId; // Pour filtrage par agence

    @Column(name = "success", nullable = false)
    private Boolean success = true;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs; // Temps d'exécution en millisecondes

    @PrePersist
    private void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (success == null) {
            success = true;
        }
    }
}