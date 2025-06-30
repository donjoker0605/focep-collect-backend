package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalActivite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_type", nullable = false)
    private String userType; // COLLECTEUR, ADMIN

    @Column(name = "action", nullable = false)
    private String action; // CREATE_CLIENT, MODIFY_CLIENT, LOGIN, LOGOUT, TRANSACTION

    @Column(name = "entity_type")
    private String entityType; // CLIENT, MOUVEMENT, COMMISSION

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "details", columnDefinition = "JSON")
    private String details; // JSON des données modifiées

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "agence_id")
    private Long agenceId;
}