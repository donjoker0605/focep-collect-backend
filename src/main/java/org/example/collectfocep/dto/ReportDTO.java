package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    private Long id;
    private String type;
    private String title;
    private String status; // pending, completed, error

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private Long agenceId;
    private String agenceNom;

    private Long collecteurId;
    private String collecteurNom;

    private LocalDateTime dateGeneration;
    private String generePar;

    private String downloadUrl;
    private Long fileSize;
    private String format;

    // Résumé des données
    private Object summary;
    private Object details;
}