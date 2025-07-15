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
}