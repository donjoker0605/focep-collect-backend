package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.ParametreCommission;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreCommissionDTO {
    
    private Long id;
    private Long agenceId;
    private String agenceNom;
    private ParametreCommission.TypeOperation typeOperation;
    private String typeOperationDisplay;
    private BigDecimal pourcentageCommission;
    private BigDecimal montantFixe;
    private BigDecimal montantMinimum;
    private BigDecimal montantMaximum;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private String createdBy;
    private String updatedBy;
    private String description;

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