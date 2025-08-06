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

    // Gestion statut actif/inactif
    private Boolean valide = true;

    @NotNull(message = "L'ID du collecteur est obligatoire")
    private Long collecteurId;

    @NotNull(message = "L'ID de l'agence est obligatoire")
    private Long agenceId;
    private String numeroCompte;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // CHAMPS GÉOLOCALISATION
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

    // NOUVEAUX CHAMPS COMMISSION - Utilise les DTOs existants
    private CommissionParameterDTO commissionParameter;

    // MÉTHODES UTILITAIRES
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public boolean isManualLocation() {
        return coordonneesSaisieManuelle != null && coordonneesSaisieManuelle;
    }

    public String getNomComplet() {
        return String.format("%s %s",
                prenom != null ? prenom : "",
                nom != null ? nom : "").trim();
    }

    public String getLocationSummary() {
        if (!hasLocation()) {
            return "Pas de localisation";
        }

        String source = isManualLocation() ? "Saisie manuelle" : "GPS";
        return String.format("%.6f, %.6f (%s)", latitude, longitude, source);
    }

    public String getFullAddress() {
        if (adresseComplete != null && !adresseComplete.trim().isEmpty()) {
            return adresseComplete;
        }

        if (ville != null && quartier != null) {
            return String.format("%s, %s", quartier, ville);
        }

        return ville != null ? ville : "Adresse non renseignée";
    }

    public boolean needsCoordinateValidation() {
        if (!hasLocation()) {
            return false;
        }

        double lat = latitude;
        double lng = longitude;

        // Vérifier si dans les limites du Cameroun (avec tolérance)
        boolean inCameroon = lat >= 1.0 && lat <= 13.5 && lng >= 7.5 && lng <= 17.0;

        return !inCameroon;
    }

    public String getLocationSource() {
        if (!hasLocation()) {
            return "NONE";
        }
        return isManualLocation() ? "MANUAL" : "GPS";
    }

    public boolean areCoordinatesValid() {
        if (!hasLocation()) {
            return true;
        }

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return false;
        }

        if (Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001) {
            return false;
        }

        return true;
    }

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

        if (Math.abs(latitude - 37.4219983) < 0.001 && Math.abs(longitude - (-122.084)) < 0.001) {
            return "Coordonnées d'émulateur détectées";
        }

        return null;
    }

    // MÉTHODES COMMISSION
    public boolean hasCommissionParameter() {
        return commissionParameter != null;
    }

    public boolean isCommissionInherited() {
        return commissionParameter == null;
    }

    public String getCommissionSummary() {
        if (isCommissionInherited()) {
            return "Hérite de l'agence";
        }

        switch (commissionParameter.getType()) {
            case FIXED:
                return String.format("Fixe: %.0f FCFA", commissionParameter.getValeur());
            case PERCENTAGE:
                return String.format("Pourcentage: %.1f%%", commissionParameter.getValeur());
            case TIER:
                return String.format("Paliers: %d niveaux",
                        commissionParameter.getPaliersCommission() != null ? commissionParameter.getPaliersCommission().size() : 0);
            default:
                return "Type inconnu";
        }
    }
}