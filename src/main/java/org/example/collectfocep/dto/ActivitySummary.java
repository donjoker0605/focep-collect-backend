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
    private Long collecteurId;
    private String action;
    private LocalDateTime timestamp;
    private String details;

    // Champs additionnels optionnels
    private String collecteurNom;
    private String actionDescription;
    private Long entityId;
    private String entityType;
}
