package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionByTypeDTO {
    private String typeCommission;
    private Double totalCommissions;
    private Double totalTVA;
    private Double totalNet;
    private Integer nombreTransactions;
}

