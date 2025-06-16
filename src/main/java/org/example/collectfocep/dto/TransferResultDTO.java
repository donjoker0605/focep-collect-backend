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
public class TransferResultDTO {

    private Long transferId;
    private LocalDateTime dateTransfer;

    // Collecteurs
    private Long sourceCollecteurId;
    private String sourceCollecteurNom;
    private Long destinationCollecteurId;
    private String destinationCollecteurNom;

    // Résultat
    private Integer nombreClientsTransferes;
    private List<ClientTransferResultDTO> resultatsClients;

    // Statut
    private String statut;
    private String message;
    private String executePar;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientTransferResultDTO {
        private Long clientId;
        private String clientNom;
        private Boolean success;
        private String message;
        private Double soldeTransfere;
    }
}
