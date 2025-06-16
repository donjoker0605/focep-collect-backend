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
public class ClientActivityDTO {

    private Long clientId;
    private String clientNom;
    private String clientPrenom;

    // Période
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    // Statistiques
    private Double totalEpargne;
    private Double totalRetrait;
    private Double soldeDebut;
    private Double soldeFin;
    private Long nombreOperations;

    // Détail des opérations
    private List<TransactionSummaryDTO> transactions;

    // Analyse
    private Double moyenneEpargne;
    private Double moyenneRetrait;
    private Integer joursActifs;
    private String tendance; // CROISSANT, STABLE, DECROISSANT
}
