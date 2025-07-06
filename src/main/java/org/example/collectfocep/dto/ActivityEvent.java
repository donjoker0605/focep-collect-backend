package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEvent {
    private String type;
    private Long collecteurId;
    private Long agenceId;
    private Long entityId;
    private String details;
    private Double montant;
    LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;

    // Factory methods
    public static ActivityEvent transaction(Long collecteurId, Long entityId, Double montant, String details) {
        ActivityEvent event = new ActivityEvent();
        event.setType("TRANSACTION");
        event.setCollecteurId(collecteurId);
        event.setEntityId(entityId);
        event.setMontant(montant);
        event.setDetails(details);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static ActivityEvent soldeNegatif(Long collecteurId, Long agenceId, Double solde) {
        ActivityEvent event = new ActivityEvent();
        event.setType("SOLDE_NEGATIF");
        event.setCollecteurId(collecteurId);
        event.setAgenceId(agenceId);
        event.setMontant(solde); // ✅ Utilisé pour le solde
        event.setDetails("Solde collecteur négatif détecté");
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}