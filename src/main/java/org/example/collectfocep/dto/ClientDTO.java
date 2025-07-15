package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.collectfocep.entities.CommissionType;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClientDTO {
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
    private String prenom;

    @NotBlank(message = "Le numéro CNI est obligatoire")
    @Size(min = 5, max = 20, message = "Le numéro CNI doit contenir entre 8 et 20 caractères")
    private String numeroCni;

    @NotBlank(message = "La ville est obligatoire")
    @Size(min = 2, max = 50, message = "La ville doit contenir entre 2 et 50 caractères")
    private String ville;

    @NotBlank(message = "Le quartier est obligatoire")
    @Size(min = 2, max = 50, message = "Le quartier doit contenir entre 2 et 50 caractères")
    private String quartier;

    @Pattern(regexp = "^(\\+237|237)?[\\s]?[679]\\d{8}$",
            message = "Le numéro de téléphone doit être au format camerounais (+237XXXXXXXXX)")
    private String telephone;

    private String photoPath;
    private boolean valide = true;

    @NotNull(message = "L'ID du collecteur est obligatoire")
    private Long collecteurId;

    @NotNull(message = "L'ID de l'agence est obligatoire")
    private Long agenceId;
    private String numeroCompte;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // CHAMPS GÉOLOCALISATION INTÉGRÉS
    @DecimalMin(value = "-90.0", message = "La latitude doit être supérieure ou égale à -90")
    @DecimalMax(value = "90.0", message = "La latitude doit être inférieure ou égale à 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "La longitude doit être supérieure ou égale à -180")
    @DecimalMax(value = "180.0", message = "La longitude doit être inférieure ou égale à 180")
    private Double longitude;

    private Boolean coordonneesSaisieManuelle = false;

    @Size(max = 500, message = "L'adresse complète ne peut pas dépasser 500 caractères")
    private String adresseComplete;

    private LocalDateTime dateMajCoordonnees;

    // 🔥 MÉTHODES UTILITAIRES

    /**
     * Vérifie si le client a une localisation définie
     */
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    /**
     * Vérifie si la localisation a été saisie manuellement
     */
    public boolean isManualLocation() {
        return coordonneesSaisieManuelle != null && coordonneesSaisieManuelle;
    }

    /**
     * Obtient le nom complet du client
     */
    public String getNomComplet() {
        return String.format("%s %s",
                prenom != null ? prenom : "",
                nom != null ? nom : "").trim();
    }

    /**
     * Obtient un résumé de la localisation
     */
    public String getLocationSummary() {
        if (!hasLocation()) {
            return "Pas de localisation";
        }

        String source = isManualLocation() ? "Saisie manuelle" : "GPS";
        return String.format("%.6f, %.6f (%s)", latitude, longitude, source);
    }

    /**
     * Obtient l'adresse complète ou construite
     */
    public String getFullAddress() {
        if (adresseComplete != null && !adresseComplete.trim().isEmpty()) {
            return adresseComplete;
        }

        if (ville != null && quartier != null) {
            return String.format("%s, %s", quartier, ville);
        }

        return ville != null ? ville : "Adresse non renseignée";
    }

    /**
     * Indique si une validation des coordonnées est nécessaire
     */
    public boolean needsCoordinateValidation() {
        if (!hasLocation()) {
            return false;
        }

        // Validation basique des coordonnées
        double lat = latitude;
        double lng = longitude;

        // Vérifier si dans les limites du Cameroun (avec tolérance)
        boolean inCameroon = lat >= 1.0 && lat <= 13.5 && lng >= 7.5 && lng <= 17.0;

        return !inCameroon;
    }

    /**
     * Obtient la source de localisation
     */
    public String getLocationSource() {
        if (!hasLocation()) {
            return "NONE";
        }
        return isManualLocation() ? "MANUAL" : "GPS";
    }

    /**
     * Validation personnalisée des coordonnées
     */
    public boolean areCoordinatesValid() {
        if (!hasLocation()) {
            return true; // Pas de coordonnées = valide
        }

        // Validation des limites globales
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return false;
        }

        // Validation contre les coordonnées nulles exactes
        if (Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001) {
            return false;
        }

        return true;
    }

    /**
     * Obtient un message d'avertissement sur la localisation
     */
    public String getLocationWarning() {
        if (!hasLocation()) {
            return null;
        }

        if (!areCoordinatesValid()) {
            return "Coordonnées GPS invalides";
        }

        if (needsCoordinateValidation()) {
            return "Coordonnées en dehors du Cameroun";
        }

        // Détecter l'émulateur Android (Mountain View, CA)
        if (Math.abs(latitude - 37.4219983) < 0.001 && Math.abs(longitude - (-122.084)) < 0.001) {
            return "Coordonnées d'émulateur détectées";
        }

        return null;
    }

    /**
     * Calcule la distance par rapport à une position donnée
     */
    public Double getDistanceFrom(Double targetLat, Double targetLng) {
        if (!hasLocation() || targetLat == null || targetLng == null) {
            return null;
        }

        // Formule de Haversine simplifiée
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(targetLat - latitude);
        double dLng = Math.toRadians(targetLng - longitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(targetLat)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Formate une distance en texte lisible
     */
    public String formatDistance(Double distanceKm) {
        if (distanceKm == null) {
            return "Distance inconnue";
        }

        if (distanceKm < 1.0) {
            return String.format("%.0f m", distanceKm * 1000);
        } else {
            return String.format("%.1f km", distanceKm);
        }
    }

    /**
     * Obtient une description de la fraîcheur des données de localisation
     */
    public String getLocationFreshness() {
        if (!hasLocation() || dateMajCoordonnees == null) {
            return "Jamais mise à jour";
        }

        LocalDateTime now = LocalDateTime.now();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dateMajCoordonnees, now);

        if (daysBetween == 0) {
            return "Aujourd'hui";
        } else if (daysBetween == 1) {
            return "Hier";
        } else if (daysBetween <= 7) {
            return String.format("Il y a %d jours", daysBetween);
        } else if (daysBetween <= 30) {
            return String.format("Il y a %d semaines", daysBetween / 7);
        } else {
            return String.format("Il y a %d mois", daysBetween / 30);
        }
    }
}