package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterAgencyTransferDTO {
    private Long sourceAgenceId;
    private String sourceAgenceNom;
    private Long targetAgenceId;
    private String targetAgenceNom;
    private String sourceLiaisonCompte;
    private String targetLiaisonCompte;
    private double montantTransfere;
}