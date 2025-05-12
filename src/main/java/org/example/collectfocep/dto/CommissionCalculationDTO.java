package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionCalculationDTO {
    private Long clientId;
    private Double montantCollecte;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String typeCommission;
    private Double valeurCommission;
    private List<CommissionTierDTO> paliers;
    private Long commissionParameterId;
}