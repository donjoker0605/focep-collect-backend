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
public class ClotureJournalPreviewDTO {

    private Long collecteurId;
    private String collecteurNom;
    private LocalDate date;
    private Long journalId;
    private String referenceJournal;
    private Boolean journalExiste;
    private Boolean dejaClôture;

    // Données comptables
    private Double soldeCompteService;
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeNet;

    // Opérations du jour
    private Integer nombreOperations;
    private List<OperationJournalierDTO> operations;

    // Comptes annexes
    private Double soldeCompteManquant;
    private Double soldeCompteAttente;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationJournalierDTO {
        private Long id;
        private String type;
        private Double montant;
        private String clientNom;
        private String clientPrenom;
        private LocalDateTime dateOperation;
    }
}