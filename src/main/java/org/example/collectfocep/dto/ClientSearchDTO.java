package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO optimisé pour la recherche de clients
 * Contient uniquement les informations nécessaires pour l'autocomplete
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientSearchDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String numeroCompte;
    private String numeroCni;
    private String telephone;
    private String displayName; // Format "Prénom NOM" pour affichage
    private Boolean hasPhone; // Indicateur présence téléphone

    private Boolean valide; // Statut actif/inactif du client

    private Long agenceId; // Pour filtrage par agence
    private Long collecteurId; // Pour identification du collecteur

    /**
     * Méthode utilitaire pour créer le nom d'affichage
     */
    public String getDisplayName() {
        if (displayName == null) {
            return String.format("%s %s",
                    prenom != null ? prenom : "",
                    nom != null ? nom.toUpperCase() : "").trim();
        }
        return displayName;
    }

    /**
     * Vérifie si le client a un téléphone valide
     */
    public Boolean getHasPhone() {
        if (hasPhone == null) {
            return telephone != null && !telephone.trim().isEmpty();
        }
        return hasPhone;
    }

    /**
     * Obtient le statut formaté
     */
    public String getStatusDisplay() {
        if (valide == null) {
            return "Inconnu";
        }
        return valide ? "Actif" : "Inactif";
    }

    /**
     * Obtient une description complète pour l'affichage
     */
    public String getFullDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(getDisplayName());

        if (numeroCompte != null) {
            desc.append(" (").append(numeroCompte).append(")");
        }

        if (telephone != null && !telephone.trim().isEmpty()) {
            desc.append(" - ").append(telephone);
        }

        if (valide != null) {
            desc.append(" [").append(getStatusDisplay()).append("]");
        }

        return desc.toString();
    }

    /**
     * Vérifie si le client est actif
     */
    public boolean isActive() {
        return valide != null && valide;
    }
}