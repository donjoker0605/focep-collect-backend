package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CollecteurDashboardDTO {
    private Long collecteurId;
    private Integer totalClients;
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeTotal;
    private List<MouvementDTO> transactionsRecentes;
    private JournalDTO journalActuel;
    private LocalDateTime lastUpdate;
}

