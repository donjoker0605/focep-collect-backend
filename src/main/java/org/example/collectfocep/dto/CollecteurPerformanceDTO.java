package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurPerformanceDTO {

    private Long collecteurId;
    private String collecteurNom;
    private String collecteurPrenom;

    // Statistiques clients
    private Long totalClients;
    private Long clientsActifs;
    private Double tauxActivite;

    // Performance financière
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeNet;
    private Double moyenneEpargneParClient;

    // Évolution
    private Double croissanceMensuelle;
    private Integer rangDansAgence;

    // Commissions
    private Double commissionsGenerees;
    private Double tauxCommission;
}
