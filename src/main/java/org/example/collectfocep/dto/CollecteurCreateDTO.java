package org.example.collectfocep.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurCreateDTO {
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le numéro CNI est obligatoire")
    private String numeroCni;

    @Email(message = "L'adresse email doit être valide")
    private String adresseMail;

    @NotBlank(message = "Le téléphone est obligatoire")
    private String telephone;

    @NotNull(message = "L'ID de l'agence est obligatoire")
    private Long agenceId;

    @NotNull(message = "Le montant maximum de retrait est obligatoire")
    private Double montantMaxRetrait;
}
