package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalDTO {
    private Long id;
    private String reference;

    // Ajoutez ces champs
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDateTime dateOuverture; // Optionnel si nécessaire

    // Conservez les autres champs existants
    private LocalDate dateJournal;
    private Long collecteurId;
    private String collecteurNom;
    private Double totalEpargne;
    private Double totalRetrait;
    private Double soldeDebut;
    private Double soldeFin;
    private Long nombreOperations;
    private List<MouvementDTO> operations;
    private Boolean estCloture;
    private LocalDateTime dateCloture;
    private String cloturePar;
    private Double montantVerse;
    private String statut;
    private String observations;
}