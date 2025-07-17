// src/main/java/org/example/collectfocep/dto/ClotureCheckDTO.java
package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClotureCheckDTO {

    private Boolean canClose;
    private String reason;
    private Double montantAVerser;
    private Integer nombreOperations;
    private Boolean journalExiste;
    private Boolean dejaClôture;

    // Méthodes utilitaires
    public boolean isClotureAutorisee() {
        return Boolean.TRUE.equals(canClose);
    }

    public boolean hasBlockingReason() {
        return reason != null && !reason.trim().isEmpty();
    }

    public String getStatusMessage() {
        if (Boolean.TRUE.equals(canClose)) {
            return "Clôture autorisée";
        } else if (Boolean.FALSE.equals(journalExiste)) {
            return "Journal inexistant";
        } else if (Boolean.TRUE.equals(dejaClôture)) {
            return "Déjà clôturé";
        } else {
            return "Clôture non autorisée";
        }
    }
}