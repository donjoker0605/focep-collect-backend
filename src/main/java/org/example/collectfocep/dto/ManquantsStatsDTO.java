package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManquantsStatsDTO {
    private Long collecteurId;
    private String collecteurNom;
    private Double totalManquant;
    private Double totalAttente;
    private Double soldeNet;
    private Boolean hasManquant;
    private Boolean hasAttente;

    // Méthodes utilitaires
    public Boolean isInDebt() {
        return totalManquant != null && totalManquant > 0;
    }

    public Boolean hasExcess() {
        return totalAttente != null && totalAttente > 0;
    }

    public String getStatusSummary() {
        if (isInDebt() && hasExcess()) {
            return "Situation mixte";
        } else if (isInDebt()) {
            return "Manquants détectés";
        } else if (hasExcess()) {
            return "Excédents en attente";
        } else {
            return "Situation équilibrée";
        }
    }
}