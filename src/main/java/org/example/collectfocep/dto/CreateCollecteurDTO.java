package org.example.collectfocep.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 👨‍💼 DTO pour la création complète d'un collecteur par le SuperAdmin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCollecteurDTO {
    
    // ================================
    // INFORMATIONS PERSONNELLES
    // ================================
    
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
    private String prenom;

    @Email(message = "L'adresse email doit être valide")
    @NotBlank(message = "L'adresse email est obligatoire")
    private String adresseMail;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
    private String password;

    @NotBlank(message = "Le numéro CNI est obligatoire")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Le numéro CNI doit contenir entre 10 et 15 chiffres")
    private String numeroCni;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^(\\+237|237)?[\\s]?[679]\\d{8}$", message = "Le téléphone doit être au format camerounais")
    private String telephone;

    // ================================
    // AFFECTATIONS
    // ================================
    
    @NotNull(message = "L'ID de l'agence est obligatoire")
    @Positive(message = "L'ID de l'agence doit être positif")
    private Long agenceId;

    @NotNull(message = "L'ID de l'admin est obligatoire")
    @Positive(message = "L'ID de l'admin doit être positif")
    private Long adminId;

    // ================================
    // PARAMÈTRES COLLECTEUR
    // ================================
    
    @NotNull(message = "Le montant maximum de retrait est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "Le montant maximum doit être supérieur ou égal à 0")
    @DecimalMax(value = "1000000.0", message = "Le montant maximum ne peut pas dépasser 1,000,000")
    private BigDecimal montantMaxRetrait;

    @Builder.Default
    private Boolean active = true;

    // ================================
    // PARAMÈTRES DE COMMISSION
    // ================================
    
    /**
     * Paramètres de commission pour ce collecteur
     * Peuvent être : montant fixe, pourcentage, ou par palier
     */
    private List<ParametreCommissionDTO> parametresCommission;

    // ================================
    // MÉTHODES UTILITAIRES
    // ================================
    
    public String getDisplayName() {
        return String.format("%s %s", prenom, nom);
    }
    
    public boolean hasCommissionParams() {
        return parametresCommission != null && !parametresCommission.isEmpty();
    }
}