package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurDTO {
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @NotBlank(message = "Le numéro CNI est obligatoire")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Le numéro CNI doit contenir entre 10 et 15 chiffres")
    private String numeroCni;

    @Email(message = "L'adresse email doit être valide")
    @NotBlank(message = "L'adresse email est obligatoire")
    private String adresseMail;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^[0-9]{9,10}$", message = "Le téléphone doit contenir 9 ou 10 chiffres")
    private String telephone;

    @NotNull(message = "L'ID de l'agence est obligatoire")
    @Positive(message = "L'ID de l'agence doit être positif")
    private Long agenceId;

    @NotNull(message = "Le montant maximum de retrait est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "Le montant maximum doit être supérieur ou égal à 0")
    @DecimalMax(value = "1000000.0", message = "Le montant maximum ne peut pas dépasser 1,000,000")
    private Double montantMaxRetrait;

    private Boolean active = true;

    @Min(value = 0, message = "L'ancienneté ne peut pas être négative")
    @Max(value = 480, message = "L'ancienneté ne peut pas dépasser 40 ans (480 mois)")
    private Integer ancienneteEnMois = 0;

    // Getter/Setter pour convention JavaBean
    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}