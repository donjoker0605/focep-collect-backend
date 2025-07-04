package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityEvent {
    private String type;
    private Long collecteurId;
    private Long agenceId;
    private Long entityId;
    private Double montant;
    private Double solde;
    private LocalDateTime timestamp;
}