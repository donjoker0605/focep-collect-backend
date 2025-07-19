package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClotureCheckDTO {

    private Long collecteurId;
    private String collecteurNom;
    private LocalDate date;
    private boolean peutCloturer;
    private String raisonInterdiction;
    private boolean journalExiste;
    private boolean dejaClôture;
    private Double soldeCompteService;
    private Integer nombreOperations;

    public static ClotureCheckDTO fromPreview(ClotureJournalPreviewDTO preview) {
        return ClotureCheckDTO.builder()
                .collecteurId(preview.getCollecteurId())
                .collecteurNom(preview.getCollecteurNom())
                .date(preview.getDate())
                .journalExiste(preview.getJournalExiste() != null && preview.getJournalExiste())
                .dejaClôture(preview.getDejaClôture() != null && preview.getDejaClôture())
                .soldeCompteService(preview.getSoldeCompteService())
                .nombreOperations(preview.getNombreOperations())
                .peutCloturer(preview.getJournalExiste() != null && preview.getJournalExiste()
                        && (preview.getDejaClôture() == null || !preview.getDejaClôture()))
                .raisonInterdiction(getRaisonInterdiction(preview))
                .build();
    }

    private static String getRaisonInterdiction(ClotureJournalPreviewDTO preview) {
        if (preview.getJournalExiste() == null || !preview.getJournalExiste()) {
            return "Aucun journal trouvé pour cette date";
        }
        if (preview.getDejaClôture() != null && preview.getDejaClôture()) {
            return "Le journal est déjà clôturé";
        }
        return null;
    }
}