package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientLocationDTO {

    private Long clientId;
    private String nomComplet;

    // Coordonnées GPS
    private Double latitude;  // Utiliser Double pour compatibilité JSON
    private Double longitude; // Utiliser Double pour compatibilité JSON

    // Métadonnées
    private Boolean coordonneesSaisieManuelle;
    private String adresseComplete;
    private LocalDateTime dateMajCoordonnees;
    private String source; // "GPS" ou "MANUAL"

    // Informations supplémentaires
    private Double accuracy; // Précision en mètres
    private String ville;
    private String quartier;

    // Informations du collecteur (pour sécurité)
    private Long collecteurId;
    private Long agenceId;

    // Méthodes utilitaires
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public boolean isManualEntry() {
        return coordonneesSaisieManuelle != null && coordonneesSaisieManuelle;
    }

    public String getLocationSummary() {
        if (!hasLocation()) {
            return "Pas de localisation";
        }

        String source = isManualEntry() ? "Saisie manuelle" : "GPS";
        String accuracy = this.accuracy != null ? String.format(" (±%.0fm)", this.accuracy) : "";

        return String.format("%.6f, %.6f - %s%s", latitude, longitude, source, accuracy);
    }
}