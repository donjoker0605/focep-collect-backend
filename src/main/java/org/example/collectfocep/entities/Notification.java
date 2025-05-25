package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    @Builder.Default // Résout le problème @Builder avec l'initialisation
    private Boolean lu = false;

    @Column(nullable = false)
    private String destinataire;

    @Column(columnDefinition = "JSON")
    private String metadata;

    private String actionUrl;
    private String actionLabel;

    @Column(nullable = false)
    @Builder.Default // Pour permettre l'initialisation automatique
    private LocalDateTime dateCreation = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    public enum NotificationType {
        INFO, WARNING, ERROR, SUCCESS
    }
}