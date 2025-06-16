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

    private String nom;

    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @Pattern(regexp = "^6[0-9]{8}$", message = "Format de téléphone invalide")
    private String telephone;

    @Size(min = 10, message = "Le numéro CNI doit avoir au moins 10 caractères")
    private String numeroCni;

    // AJOUT du champ adresseMail qui était manquant
    @Email(message = "Format d'email invalide")
    private String adresseMail;

    @Positive(message = "Le montant doit être positif")
    private Double montantMaxRetrait;

    private Boolean active;

    // Ne jamais permettre la modification de l'agence
    private Long agenceId;
}