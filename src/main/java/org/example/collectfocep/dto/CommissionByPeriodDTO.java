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
public class CommissionByPeriodDTO {
    private LocalDate periode;
    private Double totalCommissions;
    private Double totalTVA;
    private Double totalNet;
    private Integer nombreTransactions;
}