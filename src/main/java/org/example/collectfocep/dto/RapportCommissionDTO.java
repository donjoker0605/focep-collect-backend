package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapportCommissionDTO {
    private Long collecteurId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private List<CommissionClientDTO> commissionsClients;
    private double totalCommissions;
    private double totalTVA;
    private double remunerationCollecteur;
    private double partEMF;
    private double tvaSurPartEMF;
}
