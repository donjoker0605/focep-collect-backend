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
public class AdminDashboardDTO {

    // Informations générales
    private String periode;
    private LocalDateTime lastUpdate;

    // Statistiques collecteurs
    private Long totalCollecteurs;
    private Long collecteursActifs;
    private Long collecteursInactifs;
    private Double tauxCollecteursActifs;

    // Statistiques clients
    private Long totalClients;
    private Long clientsActifs;
    private Long clientsInactifs;
    private Double tauxClientsActifs;

    // Statistiques financières
    private Double totalEpargne;
    private Double totalRetrait;
    private Double soldeNet;

    // Commissions
    private Long commissionsEnAttente;
    private Double totalCommissions;

    // Agences
    private Long agencesActives;
}
