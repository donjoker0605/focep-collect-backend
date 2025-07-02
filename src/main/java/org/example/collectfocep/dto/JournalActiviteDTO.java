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
public class JournalActiviteDTO {
    private Long id;
    private Long userId;
    private String userType;
    private String username;
    private String action;
    private String actionDisplayName; // Nom affiché à l'utilisateur
    private String entityType;
    private Long entityId;
    private String entityDisplayName; // Nom de l'entité pour affichage
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
    private Long agenceId;
    private Boolean success;
    private String errorMessage;
    private Long durationMs;

    // Champs calculés pour l'affichage
    private String timeAgo;
    private String actionIcon;
    private String actionColor;
    private String description;
}