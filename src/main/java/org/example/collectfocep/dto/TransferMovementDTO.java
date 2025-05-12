package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferMovementDTO {
    private Long mouvementId;
    private String libelle;
    private String sens;
    private double montant;
    private String compteSource;
    private String compteDestination;
    private LocalDateTime dateOperation;
}
