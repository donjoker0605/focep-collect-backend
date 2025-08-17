package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.collectfocep.entities.HistoriqueRemuneration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour l'historique des rémunérations - évite les problèmes de sérialisation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueRemunerationDTO {
    
    private Long id;
    private Long collecteurId;
    private String collecteurNom;
    private String periode;
    private LocalDate dateDebutPeriode;
    private LocalDate dateFinPeriode;
    private LocalDateTime dateRemuneration;
    private BigDecimal montantSInitial;
    private BigDecimal totalRubriquesVi;
    private BigDecimal montantEmf;
    private BigDecimal montantTva;
    private String details;
    private String effectuePar;
    
    /**
     * Conversion depuis entité
     */
    public static HistoriqueRemunerationDTO fromEntity(HistoriqueRemuneration entity) {
        if (entity == null) {
            return null;
        }
        
        // Récupération sécurisée des informations du collecteur
        Long collecteurId = null;
        String collecteurNom = "N/A";
        
        try {
            if (entity.getCollecteur() != null) {
                collecteurId = entity.getCollecteur().getId();
                String prenom = entity.getCollecteur().getPrenom();
                String nom = entity.getCollecteur().getNom();
                if (prenom != null && nom != null) {
                    collecteurNom = prenom + " " + nom;
                } else if (nom != null) {
                    collecteurNom = nom;
                } else {
                    collecteurNom = "Collecteur ID " + collecteurId;
                }
            }
        } catch (Exception e) {
            // En cas d'erreur lazy loading, utiliser l'ID seulement
            collecteurNom = "Collecteur";
        }
        
        // Gestion sécurisée de la période
        String periode = "N/A";
        if (entity.getDateDebutPeriode() != null && entity.getDateFinPeriode() != null) {
            periode = entity.getDateDebutPeriode() + " → " + entity.getDateFinPeriode();
        }
        
        return HistoriqueRemunerationDTO.builder()
                .id(entity.getId())
                .collecteurId(collecteurId)
                .collecteurNom(collecteurNom)
                .periode(periode)
                .dateDebutPeriode(entity.getDateDebutPeriode())
                .dateFinPeriode(entity.getDateFinPeriode())
                .dateRemuneration(entity.getDateRemuneration())
                .montantSInitial(entity.getMontantSInitial() != null ? entity.getMontantSInitial() : BigDecimal.ZERO)
                .totalRubriquesVi(entity.getTotalRubriquesVi() != null ? entity.getTotalRubriquesVi() : BigDecimal.ZERO)
                .montantEmf(entity.getMontantEmf() != null ? entity.getMontantEmf() : BigDecimal.ZERO)
                .montantTva(entity.getMontantTva() != null ? entity.getMontantTva() : BigDecimal.ZERO)
                .details(entity.getDetails())
                .effectuePar(entity.getEffectuePar())
                .build();
    }
}