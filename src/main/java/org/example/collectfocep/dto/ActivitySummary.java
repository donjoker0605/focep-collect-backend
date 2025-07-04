package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivitySummary {
    private Long id;
    private String type;
    private String collecteurNom;
    private String description;
    private LocalDateTime timestamp;
    private String priority;
}
