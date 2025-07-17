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
    private boolean canClose;
    private String reason;
    private Double montantAVerser;
    private Integer nombreOperations;
    private Boolean journalExiste;
    private Boolean dejaClôture;

    public static ClotureCheckDTO fromPreview(ClotureJournalPreviewDTO preview) {
        boolean canClose = preview.getJournalExiste() && !preview.getDejaClôture();
        String reason = "";

        if (!preview.getJournalExiste()) {
            reason = "Aucun journal trouvé pour cette date";
        } else if (preview.getDejaClôture()) {
            reason = "Le journal est déjà clôturé";
        }

        return ClotureCheckDTO.builder()
                .canClose(canClose)
                .reason(reason)
                .montantAVerser(preview.getSoldeCompteService())
                .nombreOperations(preview.getNombreOperations())
                .journalExiste(preview.getJournalExiste())
                .dejaClôture(preview.getDejaClôture())
                .build();
    }
}
