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
public class DailyActivityDTO {
    private LocalDate date;
    private Integer nombreTransactions;
    private Double montantEpargne;
    private Double montantRetrait;
    private Double soldeJournalier;
}