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

    @Email(message = "Format d'email invalide")
    private String adresseMail;

    @Size(min = 8, max = 15, message = "Le téléphone doit contenir entre 8 et 15 caractères")
    private String telephone;

    @Size(min = 8, max = 20, message = "Le numéro CNI doit contenir entre 8 et 20 caractères")
    private String numeroCni;

    private Double montantMaxRetrait;

    private Boolean active;

    // Nouveau mot de passe (optionnel)
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String nouveauMotDePasse;
}