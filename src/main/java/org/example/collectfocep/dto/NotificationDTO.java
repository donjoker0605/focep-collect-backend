package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {

    private Long id;
    private String titre;
    private String message;
    private String type;
    private Boolean lu;
    private String destinataire;
    private Map<String, Object> metadata;
    private String actionUrl;
    private String actionLabel;
    private LocalDateTime dateCreation;

    // Méthodes utilitaires
    public boolean isUnread() {
        return !Boolean.TRUE.equals(lu);
    }

    public boolean hasAction() {
        return actionUrl != null && !actionUrl.trim().isEmpty();
    }
}