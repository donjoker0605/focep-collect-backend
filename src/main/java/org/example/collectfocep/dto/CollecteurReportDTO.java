package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurReportDTO {

    private Long collecteurId;
    private String collecteurNom;
    private String collecteurPrenom;
    private String collecteurEmail;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Résumé
    private Long totalClients;
    private Long nouveauxClients;
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeNet;
    private Long nombreOperations;
    private Double commissionsGenerees;

    // Détails par client
    private List<ClientActivityDTO> activitesClients;

    // Évolution quotidienne
    private List<DailyActivityDTO> evolutionQuotidienne;

    // Top clients
    private List<TopClientDTO> topClients;
}
