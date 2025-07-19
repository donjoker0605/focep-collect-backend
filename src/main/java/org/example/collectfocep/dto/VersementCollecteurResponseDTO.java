package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersementCollecteurResponseDTO {

    private Long id;
    private Long collecteurId;
    private String collecteurNom;
    private LocalDate date;
    private Double montantCollecte;
    private Double montantVerse;
    private Double excedent;
    private Double manquant;
    private String statut;
    private String commentaire;
    private LocalDateTime dateVersement;
    private String numeroAutorisation;
    private Long journalId;

    // Méthodes utilitaires
    public boolean hasExcedent() {
        return excedent != null && excedent > 0;
    }

    public boolean hasManquant() {
        return manquant != null && manquant > 0;
    }

    public boolean isEquilibre() {
        return !hasExcedent() && !hasManquant();
    }

    public String getCasVersement() {
        if (isEquilibre()) return "NORMAL";
        if (hasExcedent()) return "EXCEDENT";
        if (hasManquant()) return "MANQUANT";
        return "INDETERMINE";
    }
}