package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionResult {
    private double montantCommission;
    private double montantTVA;
    private double montantNet;
    private String typeCalcul;
    private LocalDateTime dateCalcul;
    private Long clientId;
    private Long collecteurId;
}