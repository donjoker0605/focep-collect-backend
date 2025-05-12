package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepartitionCommissionDTO {
    private double montantTotalCommission;
    private double montantTVAClient;
    private double partCollecteur;
    private double partEMF;
    private double tvaSurPartEMF;
    private LocalDateTime dateRepartition;
    private List<org.example.collectfocep.dto.MouvementCommissionDTO> mouvements;
}
