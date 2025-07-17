package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersementCollecteurRequestDTO {

    @NotNull(message = "L'ID du collecteur est obligatoire")
    private Long collecteurId;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotNull(message = "Le montant versé est obligatoire")
    @Positive(message = "Le montant versé doit être positif")
    private Double montantVerse;

    private String commentaire;
}