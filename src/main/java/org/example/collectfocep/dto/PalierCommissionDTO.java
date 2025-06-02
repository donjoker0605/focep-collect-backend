package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PalierCommissionDTO {
    private Long id;
    private Double montantMin;
    private Double montantMax;
    private Double taux;
}