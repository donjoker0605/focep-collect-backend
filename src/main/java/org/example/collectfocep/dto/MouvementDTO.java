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
    private Double montant;
    private String libelle;
    private String sens;
    private LocalDateTime dateOperation;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private String description;
    private String status;
    private String reference;
    private Long compteSourceId;
    private Long compteDestinationId;
    private Long journalId;
    private String journalReference;
    private Double commissionMontant;
    private String commissionType;
    private ClientBasicDTO client;
    private CollecteurBasicDTO collecteur;
}
