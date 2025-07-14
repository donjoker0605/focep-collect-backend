package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.CommissionType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientDetailDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String numeroCni;
    private String ville;
    private String quartier;
    private String telephone;
    private String photoPath;
    private String numeroCompte;
    private Boolean valide;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long collecteurId;
    private Long agenceId;

    // 🔥 NOUVEAUX CHAMPS GÉOLOCALISATION
    private Double latitude;
    private Double longitude;
    private Boolean coordonneesSaisieManuelle;
    private String adresseComplete;
    private LocalDateTime dateMajCoordonnees;
    private String sourceLocalisation; // "GPS" ou "MANUAL"

    // Informations des transactions
    private List<MouvementDTO> transactions;
    private Integer totalTransactions;
    private Double soldeTotal;

    // Statistiques rapides
    private Double totalEpargne;
    private Double totalRetraits;

    // Paramètres de commission
    private CommissionType commissionType;
    private Double montantFixe;
    private Double pourcentage;
    private List<PalierCommissionDTO> paliersCommission;
    private String codeCommission;
    private CommissionParameterDTO commissionParameter;

    // 🔥 MÉTHODES UTILITAIRES GÉOLOCALISATION
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public boolean isManualLocation() {
        return coordonneesSaisieManuelle != null && coordonneesSaisieManuelle;
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

    // Méthodes utilitaires existantes
    public String getNomComplet() {
        return String.format("%s %s", prenom != null ? prenom : "", nom != null ? nom : "").trim();
    }

    public boolean isActive() {
        return valide != null && valide;
    }

    public boolean hasTransactions() {
        return transactions != null && !transactions.isEmpty();
    }

    public boolean hasPositiveBalance() {
        return soldeTotal != null && soldeTotal > 0;
    }

    public Double getBalanceEvolution() {
        if (totalEpargne == null || totalRetraits == null) {
            return 0.0;
        }
        return totalEpargne - totalRetraits;
    }

    public String getBalanceStatus() {
        if (soldeTotal == null) {
            return "Inconnu";
        }
        if (soldeTotal > 0) {
            return "Positif";
        } else if (soldeTotal < 0) {
            return "Négatif";
        } else {
            return "Neutre";
        }
    }

    public LocalDateTime getLastActivity() {
        if (dateMajCoordonnees != null && dateModification != null) {
            return dateMajCoordonnees.isAfter(dateModification) ? dateMajCoordonnees : dateModification;
        }
        return dateModification != null ? dateModification : dateMajCoordonnees;
    }

    public boolean needsLocationUpdate() {
        return !hasLocation() || (dateMajCoordonnees == null) ||
                (dateModification != null && dateModification.isAfter(dateMajCoordonnees));
    }
}