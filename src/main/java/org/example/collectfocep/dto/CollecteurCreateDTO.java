package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurCreateDTO {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String adresseMail;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^6[0-9]{8}$", message = "Format de téléphone invalide")
    private String telephone;

    @NotBlank(message = "Le numéro CNI est obligatoire")
    @Size(min = 10, message = "Le numéro CNI doit avoir au moins 10 caractères")
    private String numeroCni;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit avoir au moins 6 caractères")
    private String password;

    @NotNull(message = "Le montant maximum de retrait est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montantMaxRetrait;

    @Builder.Default
    private Boolean active = true;

    // L'agenceId sera assignée automatiquement côté backend
    private Long agenceId;

    // Méthodes de compatibilité pour transition
    @Deprecated(since = "2.0", forRemoval = true)
    public Double getMontantMaxRetraitAsDouble() {
        return montantMaxRetrait != null ? montantMaxRetrait.doubleValue() : null;
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public void setMontantMaxRetraitFromDouble(Double montant) {
        this.montantMaxRetrait = montant != null ? BigDecimal.valueOf(montant) : null;
    }

    /**
     * Constructeur pour compatibilité avec CollecteurServiceImpl
     */
    public CollecteurCreateDTO(String nom, String prenom, String numeroCni,
                               String adresseMail, String telephone,
                               Long agenceId, BigDecimal montantMaxRetrait) {
        this.nom = nom;
        this.prenom = prenom;
        this.numeroCni = numeroCni;
        this.adresseMail = adresseMail;
        this.telephone = telephone;
        this.agenceId = agenceId;
        this.montantMaxRetrait = montantMaxRetrait;
    }
}