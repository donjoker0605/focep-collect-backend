package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MouvementDTO {
    private Long id;
    private String typeMouvement; // EPARGNE, RETRAIT
    private Double montant;
    private LocalDateTime dateHeure;
    private String description;
    private String reference;
    private String status;

    // Informations du client
    private ClientBasicDTO client;

    // Informations du collecteur
    private CollecteurBasicDTO collecteur;

    // Informations du journal
    private Long journalId;
    private String journalReference;

    // Informations des comptes
    private Long compteSourceId;
    private Long compteDestinationId;

    // Commission
    private Double commissionMontant;
    private String commissionType;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
