package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionReportDTO {
    private String type; // COLLECTEUR, AGENCE, GLOBAL
    private Long entityId;
    private String entityName;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime dateGeneration;

    // Totaux
    private Double totalCommissions;
    private Double totalTVA;
    private Double totalNet;
    private Integer nombreCommissions;

    // Détails
    private List<CommissionCalculationResultDTO.CommissionDetailDTO> details;
    private List<CommissionByTypeDTO> parType;
    private List<CommissionByPeriodDTO> parPeriode;
}