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
public class ReportDTO {

    private Long id;
    private String type;
    private String title;
    private String description;
    private String status;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Long agenceId;
    private String nomAgence;
    private Long collecteurId;
    private String nomCollecteur;
    private String downloadUrl;
    private Long tailleFichier;
    private String formatFichier;
    private String createdBy;
    private Integer nombreEnregistrements;
    private String parametres;

    // ✅ MÉTHODES UTILITAIRES
    public boolean isCompleted() {
        return "completed".equals(this.status);
    }

    public boolean isPending() {
        return "pending".equals(this.status);
    }

    public boolean isFailed() {
        return "failed".equals(this.status);
    }
}