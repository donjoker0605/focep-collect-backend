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
public class CommissionClientDTO {
    private Long clientId;
    private String nomClient;
    private String numeroCompte;
    private double montantCollecte;
    private double montantCommission;
    private double montantTVA;
    private String typeCommission;
    private LocalDateTime dateCalcul;
}