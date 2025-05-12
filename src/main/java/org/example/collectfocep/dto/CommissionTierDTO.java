package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionTierDTO {
    private Double montantMin;
    private Double montantMax;
    private Double taux;
}
