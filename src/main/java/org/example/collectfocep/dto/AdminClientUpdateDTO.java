package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AdminClientUpdateDTO
 * DTO spécialisé pour les mises à jour de client par un ADMIN
 * Contient TOUS les champs que l'admin peut modifier (plus que le collecteur)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminClientUpdateDTO {

    // ============================================
    // CHAMPS MODIFIABLES PAR L'ADMIN (ÉTENDUS)
    // ============================================

    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

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
    // CHAMPS ADMIN UNIQUEMENT
    // ============================================

    /**
     * Statut du client (seul l'admin peut modifier)
     */
    private Boolean valide;

    /**
     * Transfert vers un autre collecteur (admin seulement)
     */
    private Long nouveauCollecteurId;

    // ============================================
    // CHAMPS GÉOLOCALISATION
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
    // CHAMPS NON MODIFIABLES (MÊME POUR ADMIN)
    // ============================================

    // Ces champs restent exclus :
    // - collecteurId, agenceId (sécurité - utiliser nouveauCollecteurId pour transfert)
    // - numeroCompte (sécurité absolue)
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
     * Vérifie si un transfert de collecteur est demandé
     */
    public boolean hasCollecteurTransfer() {
        return nouveauCollecteurId != null;
    }

    /**
     * Obtient un résumé des modifications proposées par l'admin
     */
    public String getAdminUpdateSummary() {
        StringBuilder summary = new StringBuilder("Modifications admin: ");
        boolean hasChanges = false;

        if (nom != null) {
            summary.append("nom, ");
            hasChanges = true;
        }
        if (prenom != null) {
            summary.append("prénom, ");
            hasChanges = true;
        }
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
        if (valide != null) {
            summary.append("statut, ");
            hasChanges = true;
        }
        if (hasCollecteurTransfer()) {
            summary.append("transfert collecteur, ");
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
     * Validation d'intégrité pour admin
     */
    public void validateAsAdmin() {
        if (hasCoordinates() && !areCoordinatesValid()) {
            throw new IllegalArgumentException("Coordonnées GPS invalides");
        }

        if (isEmulatorCoordinates()) {
            throw new IllegalArgumentException("Coordonnées d'émulateur détectées - utilisez des coordonnées réelles");
        }

        // Validation spécifique admin
        if (nom != null && nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide");
        }

        if (prenom != null && prenom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le prénom ne peut pas être vide");
        }
    }

    /**
     * Convertit vers ClientUpdateDTO (pour compatibilité avec les services existants)
     */
    public ClientUpdateDTO toClientUpdateDTO() {
        return ClientUpdateDTO.builder()
                .telephone(this.telephone)
                .numeroCni(this.numeroCni)
                .ville(this.ville)
                .quartier(this.quartier)
                .latitude(this.latitude)
                .longitude(this.longitude)
                .coordonneesSaisieManuelle(this.coordonneesSaisieManuelle)
                .adresseComplete(this.adresseComplete)
                .build();
    }
}