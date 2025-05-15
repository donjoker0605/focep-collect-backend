package org.example.collectfocep.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurUpdateDTO {
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @Pattern(regexp = "^[0-9]{9,10}$", message = "Le téléphone doit contenir 9 ou 10 chiffres")
    private String telephone;

    @DecimalMin(value = "0.0", message = "Le montant maximum doit être positif")
    private Double montantMaxRetrait;

    private Boolean active;
}
