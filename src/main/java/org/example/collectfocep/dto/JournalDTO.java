package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalDTO {
    private Long id;
    private String reference;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateCloture;
    private Boolean estCloture;
    private Long collecteurId;
    private String collecteurNom;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    public boolean isActif() {
        return !Boolean.TRUE.equals(estCloture);
    }

    public String getStatutText() {
        return Boolean.TRUE.equals(estCloture) ? "CLOTURÉ" : "OUVERT";
    }
}