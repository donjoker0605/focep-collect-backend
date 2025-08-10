package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO spécialisé pour les mises à jour de client par un collecteur
 * Contient SEULEMENT les champs que le collecteur peut modifier
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientUpdateDTO {

    // ============================================
    // CHAMPS MODIFIABLES PAR LE COLLECTEUR
    // ============================================

    @Pattern(regexp = "^(\\+237|237)?[\\s]?[679]\\d{8}$",
            message = "Le numéro de téléphone doit être au format camerounais (+237XXXXXXXXX)")
    private String telephone;

    @Size(min = 8, max = 20, message = "Le numéro CNI doit contenir entre 8 et 20 caractères")
    private String numeroCni;

    @Size(min = 2, max = 50, message = "La ville doit contenir entre 2 et 50 caractères")
    private String ville;

    @Size(min = 2, max = 50, message = "Le quartier doit contenir entre 2 et 50 caractères")
    private String quartier;

    // ============================================
    // CHAMPS GÉOLOCALISATION (modifiables)
    // ============================================

    @DecimalMin(value = "-90.0", message = "La latitude doit être supérieure ou égale à -90")
    @DecimalMax(value = "90.0", message = "La latitude doit être inférieure ou égale à 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "La longitude doit être supérieure ou égale à -180")
    @DecimalMax(value = "180.0", message = "La longitude doit être inférieure ou égale à 180")
    private Double longitude;

    private Boolean coordonneesSaisieManuelle;

    @Size(max = 500, message = "L'adresse complète ne peut pas dépasser 500 caractères")
    private String adresseComplete;

    // ============================================
    // PARAMÈTRES DE COMMISSION (modifiable par admin)
    // ============================================
    
    private CommissionParameterDTO commissionParameter;
    
    private Boolean valide; // Statut actif/inactif (admin seulement)

    // ============================================
    // CHAMPS NON MODIFIABLES PAR LE COLLECTEUR
    // ============================================

    // Ces champs sont exclus volontairement :
    // - nom, prenom (modifiable seulement par admin)
    // - collecteurId, agenceId (sécurité)
    // - numeroCompte (sécurité)
    // - valide (seulement admin)
    // - dateCreation, dateModification (automatiques)

    // ============================================
    // MÉTHODES DE VALIDATION MÉTIER
    // ============================================

    /**
     * Vérifie si des coordonnées GPS sont fournies
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Vérifie si des paramètres de commission sont fournis
     */
    public boolean hasCommissionParameter() {
        return commissionParameter != null;
    }

    /**
     * Vérifie si les coordonnées sont valides
     */
    public boolean areCoordinatesValid() {
        if (!hasCoordinates()) {
            return true; // Pas de coordonnées = valide (pas obligatoire)
        }

        return latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }

    /**
     * Vérifie si les coordonnées semblent être dans les limites du Cameroun
     */
    public boolean areCoordinatesInCameroon() {
        if (!hasCoordinates()) {
            return true; // Pas de coordonnées = considéré comme valide
        }

        return latitude >= 1.0 && latitude <= 13.5 &&
                longitude >= 8.0 && longitude <= 16.5;
    }

    /**
     * Détecte les coordonnées d'émulateur (Mountain View, CA)
     */
    public boolean isEmulatorCoordinates() {
        if (!hasCoordinates()) {
            return false;
        }

        return Math.abs(latitude - 37.4219983) < 0.001 &&
                Math.abs(longitude - (-122.084)) < 0.001;
    }

    /**
     * Obtient un résumé des modifications proposées
     */
    public String getUpdateSummary() {
        StringBuilder summary = new StringBuilder("Modifications: ");
        boolean hasChanges = false;

        if (telephone != null) {
            summary.append("téléphone, ");
            hasChanges = true;
        }
        if (numeroCni != null) {
            summary.append("CNI, ");
            hasChanges = true;
        }
        if (ville != null) {
            summary.append("ville, ");
            hasChanges = true;
        }
        if (quartier != null) {
            summary.append("quartier, ");
            hasChanges = true;
        }
        if (hasCoordinates()) {
            summary.append("localisation, ");
            hasChanges = true;
        }

        if (!hasChanges) {
            return "Aucune modification";
        }

        // Supprimer la dernière virgule
        String result = summary.toString();
        return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
    }

    /**
     * Validation d'intégrité
     */
    public void validate() {
        if (hasCoordinates() && !areCoordinatesValid()) {
            throw new IllegalArgumentException("Coordonnées GPS invalides");
        }

        if (isEmulatorCoordinates()) {
            throw new IllegalArgumentException("Coordonnées d'émulateur détectées - utilisez des coordonnées réelles");
        }
    }
}