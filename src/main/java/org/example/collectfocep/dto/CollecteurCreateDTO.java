package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurCreateDTO {

    @NotBlank(message = "Le nom est requis")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est requis")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String prenom;

    @NotBlank(message = "Le numéro CNI est requis")
    @Size(max = 20, message = "Le numéro CNI ne peut pas dépasser 20 caractères")
    private String numeroCni;

    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "L'adresse email doit être valide")
    @Size(max = 150, message = "L'email ne peut pas dépasser 150 caractères")
    private String adresseMail;

    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Le numéro de téléphone doit être valide")
    private String telephone;

    @NotNull(message = "L'ID de l'agence est requis")
    @Positive(message = "L'ID de l'agence doit être positif")
    private Long agenceId;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant maximum doit être positif")
    private Double montantMaxRetrait = 100000.0; // Valeur par défaut
}