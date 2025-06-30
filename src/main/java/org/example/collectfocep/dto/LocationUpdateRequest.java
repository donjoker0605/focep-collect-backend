package org.example.collectfocep.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateRequest {

    @NotNull(message = "La latitude est requise")
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private BigDecimal latitude;

    @NotNull(message = "La longitude est requise")
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private BigDecimal longitude;

    @Builder.Default
    private Boolean saisieManuelle = false;

    private String adresseComplete;
}