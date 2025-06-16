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
public class TransactionSummaryDTO {

    private Long id;
    private String reference;
    private String type; // EPARGNE, RETRAIT
    private Double montant;
    private LocalDateTime dateOperation;

    // Client
    private Long clientId;
    private String clientNom;

    // Collecteur
    private Long collecteurId;
    private String collecteurNom;

    // Soldes
    private Double soldeAvant;
    private Double soldeApres;

    // Statut
    private String statut;
    private String description;
}