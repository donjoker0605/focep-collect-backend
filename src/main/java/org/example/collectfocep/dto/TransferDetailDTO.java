package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferDetailDTO {
    private Long transferId;
    private Long sourceCollecteurId;
    private String sourceCollecteurNom;
    private Long targetCollecteurId;
    private String targetCollecteurNom;
    private LocalDateTime dateTransfert;
    private boolean interAgenceTransfert;
    private int nombreComptes;
    private double montantTotal;
    private double montantCommissions;
    private List<TransferMovementDTO> mouvements;
    // Ajouts pour résoudre les erreurs
    private List<ClientTransfereDTO> clientsTransferes;
    private List<TransferEventDTO> events;
    private InterAgencyTransferDTO interAgencyInfo;
    private Map<String, Object> statistics;
}