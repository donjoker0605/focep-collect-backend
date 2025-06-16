package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionParametersDTO {

    private Long id;

    @NotNull(message = "Le taux de commission est obligatoire")
    @Positive(message = "Le taux doit être positif")
    private Double tauxCommission;

    private String typeCalcul; // POURCENTAGE, FIXE
    private String periodicite; // JOURNALIERE, HEBDOMADAIRE, MENSUELLE

    // Seuils
    private Double montantMinimum;
    private Double montantMaximum;

    // Niveau d'application
    private String niveau; // AGENCE, COLLECTEUR, CLIENT
    private Long agenceId;
    private Long collecteurId;
    private Long clientId;

    // Conditions
    private List<String> conditions;

    // Métadonnées
    private Boolean actif;
    private String creePar;
    private String modifiePar;
}