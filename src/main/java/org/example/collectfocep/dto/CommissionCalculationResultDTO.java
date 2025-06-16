package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionCalculationResultDTO {

    // Période
    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Résumé
    private Double totalCommissions;
    private Long nombreCollecteurs;
    private Long nombreClientsTotal;
    private Double totalEpargneTraite;

    // Détails
    private List<CommissionDetailDTO> details;

    // Métadonnées
    private String calculePar;
    private LocalDate dateCalcul;
    private String statut; // CALCULE, VALIDE, PAYE

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommissionDetailDTO {
        private Long collecteurId;
        private String collecteurNom;
        private Long nombreClients;
        private Double montantEpargne;
        private Double tauxCommission;
        private Double montantCommission;
        private String typeCalcul;
        private List<ClientCommissionDTO> detailsClients;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientCommissionDTO {
        private Long clientId;
        private String clientNom;
        private Double montantEpargne;
        private Double commission;
    }
}

