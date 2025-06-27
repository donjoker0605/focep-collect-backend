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
public class MouvementDTO {
    private Long id;
    private double montant;
    private String libelle;
    private String sens;
    private LocalDateTime dateOperation;
    private String typeMouvement;

    // Ajoutez ces propriétés manquantes
    private ClientBasicDTO client;
    private CollecteurBasicDTO collecteur;
    private Long journalId;
    private String status;
    private String journalReference;
    private Long compteSourceId;
    private Long compteDestinationId;
    private Double commissionMontant;
    private String commissionType;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private String reference;
    private String description;
}
