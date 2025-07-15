package org.example.collectfocep.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les requêtes de mise à jour de localisation client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateRequest {

    @NotNull(message = "La latitude est requise")
    @DecimalMin(value = "-90.0", message = "La latitude doit être supérieure ou égale à -90")
    @DecimalMax(value = "90.0", message = "La latitude doit être inférieure ou égale à 90")
    private BigDecimal latitude;

    @NotNull(message = "La longitude est requise")
    @DecimalMin(value = "-180.0", message = "La longitude doit être supérieure ou égale à -180")
    @DecimalMax(value = "180.0", message = "La longitude doit être inférieure ou égale à 180")
    private BigDecimal longitude;

    @Builder.Default
    private Boolean saisieManuelle = false;

    private String adresseComplete;

    private Double accuracy; // Précision en mètres (optionnel)

    // Méthodes utilitaires
    public boolean isManualEntry() {
        return saisieManuelle != null && saisieManuelle;
    }

    public boolean hasAccuracy() {
        return accuracy != null && accuracy > 0;
    }

    public String getLocationSummary() {
        String source = isManualEntry() ? "Manuel" : "GPS";
        String accuracyInfo = hasAccuracy() ? String.format(" (±%.0fm)", accuracy) : "";
        return String.format("%.6f, %.6f - %s%s",
                latitude.doubleValue(),
                longitude.doubleValue(),
                source,
                accuracyInfo);
    }
}