package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenceReportDTO {

    private Long agenceId;
    private String agenceNom;
    private String agenceCode;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Vue d'ensemble
    private Long totalCollecteurs;
    private Long collecteursActifs;
    private Long totalClients;
    private Long clientsActifs;

    // Finances
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeNet;
    private Double totalCommissions;

    // Performance
    private List<CollecteurPerformanceDTO> performanceCollecteurs;
    private Map<String, Double> evolutionMensuelle;
    private List<TopClientDTO> topClientsAgence;

    // Comparaisons
    private Double croissanceEpargne;
    private Double croissanceClients;
    private Double tauxActivite;
}
