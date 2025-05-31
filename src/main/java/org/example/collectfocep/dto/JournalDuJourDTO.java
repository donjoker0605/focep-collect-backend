package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalDuJourDTO {
    private Long journalId;
    private Long collecteurId;
    private LocalDate date;
    private String statut;
    private Boolean estCloture;
    private String reference;
    private Integer nombreOperations;
    private List<MouvementJournalDTO> operations;

    // Méthodes utilitaires
    public boolean isActif() {
        return !Boolean.TRUE.equals(estCloture);
    }

    public String getStatutText() {
        return Boolean.TRUE.equals(estCloture) ? "CLÔTURÉ" : "OUVERT";
    }
}