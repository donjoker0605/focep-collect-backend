package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BalanceVerificationDTO {
    private Boolean sufficient;
    private Double soldeDisponible;
    private Double montantDemande;
    private Double soldeApresOperation;
    private String message;
    private String clientNom;
    private String clientPrenom;
}