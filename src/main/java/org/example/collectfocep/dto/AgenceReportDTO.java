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
public class AgenceReportDTO {
    private Long agenceId;
    private String nomAgence;
    private String codeAgence;
    private String periode;
    private LocalDateTime dateGeneration;

    // Statistiques globales
    private Integer totalCollecteurs;
    private Integer collecteursActifs;
    private Integer totalClients;
    private Integer clientsActifs;

    // Finances
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeNet;
    private Double totalCommissions;

    // Performances
    private List<CollecteurPerformanceDTO> topCollecteurs;
    private List<ClientActivityDTO> clientsLesPlusActifs;
    private List<DailyActivityDTO> activiteParJour;
    private List<MonthlyTrendDTO> tendancesMensuelles;
}