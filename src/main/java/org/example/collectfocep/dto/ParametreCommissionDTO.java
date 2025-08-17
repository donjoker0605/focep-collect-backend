package org.example.collectfocep.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.ParametreCommission;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 💰 DTO Paramètre Commission avec validation Bean stricte pour SuperAdmin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreCommissionDTO {
    
    private Long id;
    
    @NotNull(message = "L'ID de l'agence est obligatoire")
    @Positive(message = "L'ID de l'agence doit être positif")
    private Long agenceId;
    
    private String agenceNom; // Read-only
    
    @NotNull(message = "Le type d'opération est obligatoire")
    private ParametreCommission.TypeOperation typeOperation;
    
    private String typeOperationDisplay; // Read-only
    
    @NotNull(message = "Le pourcentage de commission est obligatoire")
    @DecimalMin(value = "0.0", message = "Le pourcentage de commission ne peut pas être négatif")
    @DecimalMax(value = "100.0", message = "Le pourcentage de commission ne peut pas dépasser 100%")
    @Digits(integer = 3, fraction = 2, message = "Le pourcentage doit avoir au maximum 3 chiffres avant la virgule et 2 après")
    private BigDecimal pourcentageCommission;
    
    @DecimalMin(value = "0.0", message = "Le montant fixe ne peut pas être négatif")
    @Digits(integer = 10, fraction = 2, message = "Le montant fixe doit avoir au maximum 10 chiffres avant la virgule et 2 après")
    private BigDecimal montantFixe;
    
    @DecimalMin(value = "0.0", message = "Le montant minimum ne peut pas être négatif")
    @Digits(integer = 10, fraction = 2, message = "Le montant minimum doit avoir au maximum 10 chiffres avant la virgule et 2 après")
    private BigDecimal montantMinimum;
    
    @DecimalMin(value = "0.0", message = "Le montant maximum ne peut pas être négatif")
    @Digits(integer = 10, fraction = 2, message = "Le montant maximum doit avoir au maximum 10 chiffres avant la virgule et 2 après")
    private BigDecimal montantMaximum;
    
    @NotNull(message = "Le statut actif est obligatoire")
    @Builder.Default
    private Boolean actif = true;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateModification;
    
    @Size(max = 100, message = "Le nom du créateur ne doit pas dépasser 100 caractères")
    private String createdBy;
    
    @Size(max = 100, message = "Le nom du modificateur ne doit pas dépasser 100 caractères")
    private String updatedBy;
    
    private String description; // Read-only, calculé automatiquement
    
    // ================================
    // MÉTHODES DE VALIDATION CUSTOM
    // ================================
    
    @AssertTrue(message = "Le montant minimum ne peut pas être supérieur au montant maximum")
    public boolean isValidMontantRange() {
        if (montantMinimum == null || montantMaximum == null) {
            return true; // Pas de validation si l'un des deux est null
        }
        return montantMinimum.compareTo(montantMaximum) <= 0;
    }
    
    @AssertTrue(message = "Au moins un type de commission (pourcentage ou montant fixe) doit être défini")
    public boolean hasCommissionType() {
        return (pourcentageCommission != null && pourcentageCommission.compareTo(BigDecimal.ZERO) > 0) ||
               (montantFixe != null && montantFixe.compareTo(BigDecimal.ZERO) > 0);
    }
    
    // ================================
    // MÉTHODES UTILITAIRES
    // ================================
    
    public String getTypeCommission() {
        if (typeOperation == null) return null;
        return typeOperation.name();
    }
    
    public void setTypeCommission(String typeCommission) {
        if (typeCommission != null) {
            this.typeOperation = ParametreCommission.TypeOperation.valueOf(typeCommission);
        }
    }
    
    public BigDecimal getValeur() {
        return pourcentageCommission;
    }
    
    public void setValeur(BigDecimal valeur) {
        this.pourcentageCommission = valeur;
    }
    
    public BigDecimal getSeuilMin() {
        return montantMinimum;
    }
    
    public void setSeuilMin(BigDecimal seuilMin) {
        this.montantMinimum = seuilMin;
    }
    
    public BigDecimal getSeuilMax() {
        return montantMaximum;
    }
    
    public void setSeuilMax(BigDecimal seuilMax) {
        this.montantMaximum = seuilMax;
    }
    
    public Boolean getActive() {
        return actif;
    }
    
    public void setActive(Boolean active) {
        this.actif = active;
    }

    public static ParametreCommissionDTO fromEntity(ParametreCommission entity) {
        if (entity == null) return null;
        
        return ParametreCommissionDTO.builder()
                .id(entity.getId())
                .agenceId(entity.getAgence() != null ? entity.getAgence().getId() : null)
                .agenceNom(entity.getAgence() != null ? entity.getAgence().getNomAgence() : null)
                .typeOperation(entity.getTypeOperation())
                .typeOperationDisplay(entity.getTypeOperation() != null ? entity.getTypeOperation().getDisplayName() : null)
                .pourcentageCommission(entity.getPourcentageCommission())
                .montantFixe(entity.getMontantFixe())
                .montantMinimum(entity.getMontantMinimum())
                .montantMaximum(entity.getMontantMaximum())
                .actif(entity.getActif())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .description(entity.getDescription())
                .build();
    }
}