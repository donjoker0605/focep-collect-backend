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
    private Integer nombreVersementsAvecManquant;
    private Integer nombreVersementsAvecExcedent;

    // Méthodes utilitaires
    public String getStatutFinancier() {
        if (hasManquant && totalManquant < -10000) {
            return "CRITIQUE";
        } else if (hasManquant) {
            return "ATTENTION";
        } else if (hasAttente) {
            return "EXCEDENT";
        } else {
            return "EQUILIBRE";
        }
    }

    public String getMessageStatut() {
        switch (getStatutFinancier()) {
            case "CRITIQUE":
                return "Dette importante - Contact agence recommandé";
            case "ATTENTION":
                return "Dette détectée - Surveiller les versements";
            case "EXCEDENT":
                return "Crédit disponible";
            case "EQUILIBRE":
                return "Situation financière équilibrée";
            default:
                return "Statut indéterminé";
        }
    }
}