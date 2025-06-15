package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDTO {

    @NotNull(message = "Le type de rapport est obligatoire")
    private String type; // collecteur, commission, agence

    private Long collecteurId;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    private String format; // json, pdf, excel

    // Sera assignée automatiquement
    private Long agenceId;

    private boolean includeDetails;
    private boolean includeGraphics;
}
