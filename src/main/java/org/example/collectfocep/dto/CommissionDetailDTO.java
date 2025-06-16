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
public class CommissionDetailDTO {
    private Long commissionId;
    private LocalDateTime dateCalcul;
    private String type;
    private Double montant;
    private Double tva;
    private String beneficiaire;
}