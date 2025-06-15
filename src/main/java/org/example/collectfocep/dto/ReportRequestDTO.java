package org.example.collectfocep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDTO {

    @NotBlank(message = "Le type de rapport est requis")
    private String type; // collecteur, commission, agence, global

    @NotNull(message = "La date de début est requise")
    private LocalDateTime dateDebut;

    @NotNull(message = "La date de fin est requise")
    private LocalDateTime dateFin;

    private Long collecteurId; // Requis pour les rapports de collecteur
    private Long clientId; // Optionnel pour filtrer par client
    private String format; // PDF, EXCEL, CSV
    private String titre; // Titre personnalisé du rapport
    private String description; // Description du rapport
    private Boolean includeDetails; // Inclure les détails des transactions
    private Boolean includeGraphiques; // Inclure les graphiques

    // ✅ VALIDATIONS MÉTIER
    public boolean isCollecteurReportType() {
        return "collecteur".equals(this.type) || "commission".equals(this.type);
    }

    public boolean requiresCollecteurId() {
        return isCollecteurReportType() && this.collecteurId == null;
    }
}
