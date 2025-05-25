package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientDTO {
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le numéro CNI est obligatoire")
    private String numeroCni;

    private String ville;
    private String quartier;
    private String telephone;
    private String photoPath;
    private boolean valide = true;

    @NotNull(message = "L'ID du collecteur est obligatoire")
    private Long collecteurId;

    @NotNull(message = "L'ID de l'agence est obligatoire")
    private Long agenceId;
    private String numeroCompte;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}