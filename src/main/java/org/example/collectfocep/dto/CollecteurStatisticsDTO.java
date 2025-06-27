package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurStatisticsDTO {

    private Long collecteurId;
    private String collecteurNom;
    private String collecteurPrenom;

    // Statistiques clients
    private Long totalClients;
    private Long clientsActifs;
    private Long clientsInactifs;
    private Double tauxClientsActifs;

    // Statistiques financières
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeNet;
    private Double moyenneEpargneParClient;

    // Statistiques d'activité
    private Long nombreOperationsJour;
    private Long nombreOperationsMois;
    private LocalDateTime derniereOperation;

    // Commissions
    private Double commissionsEnAttente;
    private Double totalCommissions;
    private Double commissionsMoisEnCours;

    // Performance
    private Double tauxCroissanceClients;
    private Double tauxCroissanceEpargne;
    private Integer rangAgence;

    private LocalDateTime dateCalcul;
    private Long transactionsCeMois;

}
