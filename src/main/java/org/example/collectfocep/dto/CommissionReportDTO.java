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
public class CommissionReportDTO {

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private Double totalCommissions;
    private Long nombreCollecteurs;
    private Long nombreClientsTotal;
    private Double totalEpargneTraite;

    private List<CommissionDetailDTO> details;

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
    }
}
