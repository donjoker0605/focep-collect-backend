package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
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

    // BigDecimal au lieu de Double
    @NotNull(message = "Le montant maximum de retrait est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "Le montant maximum doit être supérieur ou égal à 0")
    @DecimalMax(value = "1000000.0", message = "Le montant maximum ne peut pas dépasser 1,000,000")
    private BigDecimal montantMaxRetrait;

    @Builder.Default
    private Boolean active = true;

    @Min(value = 0, message = "L'ancienneté ne peut pas être négative")
    @Max(value = 480, message = "L'ancienneté ne peut pas dépasser 40 ans (480 mois)")
    @Builder.Default
    private Integer ancienneteEnMois = 0;

    // Champs existants
    private LocalDateTime dateModificationMontantMax;
    private String modifiePar;
    private String agenceNom;
    private Integer nombreClients;
    private Integer nombreComptes;
    private String fcmToken;
    private LocalDateTime fcmTokenUpdatedAt;

    // MÉTHODES UTILITAIRES CORRIGÉES
    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public boolean isNouveauCollecteur() {
        return ancienneteEnMois != null && ancienneteEnMois < 3;
    }

    public String getNomComplet() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }

    public boolean canPerformWithdrawals() {
        return Boolean.TRUE.equals(active) &&
                montantMaxRetrait != null &&
                montantMaxRetrait.compareTo(BigDecimal.ZERO) > 0;
    }

    // NOUVELLES MÉTHODES pour compatibilité transition
    @Deprecated(since = "2.0", forRemoval = true)
    public Double getMontantMaxRetraitAsDouble() {
        return montantMaxRetrait != null ? montantMaxRetrait.doubleValue() : null;
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public void setMontantMaxRetraitFromDouble(Double montant) {
        this.montantMaxRetrait = montant != null ? BigDecimal.valueOf(montant) : null;
    }
}