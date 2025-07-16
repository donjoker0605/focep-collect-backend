package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurUpdateDTO {

    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @Pattern(regexp = "^6[0-9]{8}$", message = "Format de téléphone invalide")
    private String telephone;

    @Size(min = 10, message = "Le numéro CNI doit avoir au moins 10 caractères")
    private String numeroCni;

    @Email(message = "Format d'email invalide")
    private String adresseMail;

    @Positive(message = "Le montant doit être positif")
    private Double montantMaxRetrait;

    private Boolean active;

    // Champ pour le changement de mot de passe
    @Size(min = 6, max = 128, message = "Le mot de passe doit contenir entre 6 et 128 caractères")
    private String newPassword;

    // Ne jamais permettre la modification de l'agence
    private Long agenceId;

    /**
     * Vérifie si un nouveau mot de passe est fourni
     */
    public boolean hasNewPassword() {
        return newPassword != null && !newPassword.trim().isEmpty();
    }
}