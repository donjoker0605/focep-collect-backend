package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDTO {

    private Long totalTransactions;
    private BigDecimal totalMontantEpargne;
    private BigDecimal totalMontantRetrait;
    private BigDecimal totalCommissions;
    private Long totalClients;
    private Long totalCollecteurs;
    private BigDecimal soldeTotal;
    private Map<String, Object> statistiques;
    private Map<String, BigDecimal> montantsParMois;
    private Map<String, Long> transactionsParType;
}