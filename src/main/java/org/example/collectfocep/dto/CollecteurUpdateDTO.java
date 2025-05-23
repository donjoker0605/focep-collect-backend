package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CollecteurUpdateDTO {

    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String nom;

    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String prenom;

    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Le numéro de téléphone doit être valide")
    private String telephone;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant maximum doit être positif")
    private Double montantMaxRetrait;

    private Boolean active;

    @Size(max = 500, message = "Les notes ne peuvent pas dépasser 500 caractères")
    private String notes;
}