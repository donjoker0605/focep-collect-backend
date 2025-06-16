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
public class CollecteurReportDTO {
    private Long collecteurId;
    private String nomCollecteur;
    private String periode;
    private LocalDateTime dateGeneration;

    // Statistiques
    private Integer totalClients;
    private Integer clientsActifs;
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeNet;
    private Integer nombreTransactions;

    // Détails
    private List<ClientActivityDTO> activitesClients;
    private List<DailyActivityDTO> activitesJournalieres;
    private List<TransactionSummaryDTO> resumeTransactions;
}