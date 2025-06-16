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
    private LocalDate dateJournal;

    // Collecteur
    private Long collecteurId;
    private String collecteurNom;

    // Statistiques
    private Double totalEpargne;
    private Double totalRetrait;
    private Double soldeDebut;
    private Double soldeFin;
    private Long nombreOperations;

    // Opérations
    private List<MouvementDTO> operations;

    // Clôture
    private Boolean estCloture;
    private LocalDateTime dateCloture;
    private String cloturePar;
    private Double montantVerse;

    // Validation
    private String statut;
    private String observations;
}