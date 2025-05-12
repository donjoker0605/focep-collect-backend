package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionParameterDTO {
    private Long id;
    private Long clientId;
    private Long collecteurId;
    private Long agenceId;
    private String typeCommission;
    private Double valeurCommission;
    private String codeProduit;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private boolean actif;
    private List<org.example.collectfocep.dto.CommissionTierDTO> paliers;
}