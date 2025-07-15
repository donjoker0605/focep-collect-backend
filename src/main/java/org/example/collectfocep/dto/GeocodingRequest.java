package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class GeocodingRequest {
    @NotNull(message = "La latitude est requise")
    @Min(value = -90, message = "Latitude invalide")
    @Max(value = 90, message = "Latitude invalide")
    private Double latitude;

    @NotNull(message = "La longitude est requise")
    @Min(value = -180, message = "Longitude invalide")
    @Max(value = 180, message = "Longitude invalide")
    private Double longitude;
}
