package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.CommissionType;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommissionParameterDTO {
    private Long id;
    private CommissionType type;
    private Double valeur;
    private String codeProduit;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean active;
    private List<PalierCommissionDTO> paliersCommission;

    // Relations par ID
    private Long clientId;
    private Long collecteurId;
    private Long agenceId;

    // Noms pour affichage
    private String clientNom;
    private String collecteurNom;
    private String agenceNom;

    /**
     * Alias pour compatibilité avec les controllers existants
     */
    public CommissionType getTypeCommission() {
        return this.type;
    }

    /**
     * Détermine le scope basé sur les relations définies
     */
    public String getScope() {
        if (clientId != null) return "CLIENT";
        if (collecteurId != null) return "COLLECTEUR";
        if (agenceId != null) return "AGENCE";
        return "UNKNOWN";
    }

    /**
     * Validation basique du DTO
     */
    public boolean isValid() {
        // Vérifications de base
        if (type == null) return false;
        if (valeur == null || valeur <= 0) return false;

        // Validation spécifique selon le type
        switch (type) {
            case FIXED:
                return valeur > 0;
            case PERCENTAGE:
                return valeur > 0 && valeur <= 100;
            case TIER:
                return paliersCommission != null &&
                        !paliersCommission.isEmpty() &&
                        validatePaliers();
            default:
                return false;
        }
    }

    /**
     * Validation du scope
     */
    public boolean isValidScope() {
        // Au moins une relation doit être définie
        return clientId != null || collecteurId != null || agenceId != null;
    }

    /**
     * Validation des paliers pour type TIER
     */
    private boolean validatePaliers() {
        if (paliersCommission == null || paliersCommission.isEmpty()) {
            return false;
        }

        // Trier par montant minimum
        List<PalierCommissionDTO> sortedPaliers = paliersCommission.stream()
                .sorted((a, b) -> Double.compare(a.getMontantMin(), b.getMontantMax()))
                .toList();

        // Vérifier la continuité
        for (int i = 0; i < sortedPaliers.size(); i++) {
            PalierCommissionDTO current = sortedPaliers.get(i);

            // Min < Max pour chaque palier
            if (current.getMontantMin() >= current.getMontantMax()) {
                return false;
            }

            // Pas de chevauchement avec le suivant
            if (i < sortedPaliers.size() - 1) {
                PalierCommissionDTO next = sortedPaliers.get(i + 1);
                if (current.getMontantMax() > next.getMontantMin()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Message d'erreur de validation détaillé
     */
    public String getValidationError() {
        if (type == null) return "Type de commission requis";
        if (valeur == null || valeur <= 0) return "Valeur de commission invalide";
        if (!isValidScope()) return "Au moins une relation (client/collecteur/agence) doit être définie";

        switch (type) {
            case PERCENTAGE:
                if (valeur > 100) return "Le pourcentage ne peut pas dépasser 100%";
                break;
            case TIER:
                if (paliersCommission == null || paliersCommission.isEmpty()) {
                    return "Les paliers sont requis pour le type TIER";
                }
                if (!validatePaliers()) {
                    return "Paliers invalides (chevauchements ou incohérences)";
                }
                break;
        }

        return null;
    }
}