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
public class CollecteurStatisticsDTO {

    private Integer totalClients;
    private Long transactionsCeMois;
    private Double volumeEpargne;
    private Double volumeRetraits;
    private Double commissionsGenerees;

    // Statistiques détaillées
    private Integer clientsActifs;
    private Integer clientsInactifs;
    private Double moyenneEpargneParClient;
    private Double moyenneRetraitParClient;

    // Évolution
    private Double croissanceClients; // En pourcentage
    private Double croissanceEpargne; // En pourcentage
    private Double croissanceRetraits; // En pourcentage

    // Performance
    private Double tauxCommission; // En pourcentage
    private Double objectifAtteint; // En pourcentage
    private Integer rangementPerformance; // Classement parmi les collecteurs

    // Données temporelles
    private LocalDateTime derniereActivite;
    private LocalDateTime dateDerniereTransaction;
    private Long joursDepuisDerniereActivite;

    // Graphiques de données
    private List<StatistiquesMensuellesDTO> evolutionMensuelle;
    private List<TransactionParTypeDTO> repartitionTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatistiquesMensuellesDTO {
        private String mois; // Format: "2025-01"
        private Double epargne;
        private Double retraits;
        private Long nombreTransactions;
        private Integer nouveauxClients;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionParTypeDTO {
        private String type; // EPARGNE, RETRAIT, etc.
        private Long nombre;
        private Double montantTotal;
        private Double pourcentage;
    }
}
