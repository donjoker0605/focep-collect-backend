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
@AllArgsConstructor
@NoArgsConstructor
public class JournalCompletDTO {
    private Long journalId;
    private Long collecteurId;
    private String collecteurNom;
    private String collecteurPrenom;
    private LocalDate date;
    private String reference;
    private String statut;
    private boolean estCloture;
    private LocalDateTime dateCreation;
    private LocalDateTime dateCloture;

    // Statistiques
    private int nombreOperations;
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeJournal;

    // Liste des mouvements
    private List<MouvementJournalDTO> mouvements;

    // Méthodes calculées
    public Double getSoldeJournal() {
        if (totalEpargne == null) totalEpargne = 0.0;
        if (totalRetraits == null) totalRetraits = 0.0;
        return totalEpargne - totalRetraits;
    }

    public boolean peutEtreCloture() {
        return !estCloture && nombreOperations > 0;
    }

    public String getStatutAffichage() {
        if (estCloture) {
            return "CLÔTURÉ";
        }
        return nombreOperations > 0 ? "EN COURS" : "VIDE";
    }
}