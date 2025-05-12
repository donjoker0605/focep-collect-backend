package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementCommissionDTO {
    private String typeOperation; // DEBIT, CREDIT
    private double montant;
    private String compteSource;
    private String compteDestination;
    private String libelle;
    private LocalDateTime dateOperation;
}
