package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummary {
    // Champs de base
    private Long collecteurId;
    private String action;
    private LocalDateTime timestamp;
    private String details;

    // Champs additionnels existants
    private String collecteurNom;
    private String actionDescription;
    private Long entityId;
    private String entityType;

    // Alignés avec JournalActivite
    private Boolean success;           // Indique si l'action s'est bien passée
    private String errorMessage;       // Message d'erreur si success = false
    private Long durationMs;          // Durée d'exécution en millisecondes

    // Champs de contexte utilisateur
    private String username;          // Nom d'utilisateur qui a fait l'action
    private String userType;          // COLLECTEUR, ADMIN
    private String ipAddress;         // Adresse IP source
    private Long agenceId;           // ID de l'agence
}