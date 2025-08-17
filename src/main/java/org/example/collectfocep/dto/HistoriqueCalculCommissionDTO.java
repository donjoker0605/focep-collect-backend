package org.example.collectfocep.dto;

import lombok.*;
import org.example.collectfocep.entities.HistoriqueCalculCommission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 🔥 NOUVEAU DTO: Pour éviter les problèmes de sérialisation JSON
 * 
 * Évite les relations lazy et les dépendances cycliques
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueCalculCommissionDTO {

    private Long id;
    private Long collecteurId;
    private String collecteurNom;
    private String collecteurPrenom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal montantCommissionTotal;
    private BigDecimal montantTvaTotal;
    private Integer nombreClients;
    private String statut;
    private LocalDateTime dateCalcul;
    private String calculePar;
    private Long agenceId;
    private Boolean remunere;
    private LocalDateTime dateRemuneration;
    private Long remunerationId;

    /**
     * Conversion depuis l'entité
     */
    public static HistoriqueCalculCommissionDTO fromEntity(HistoriqueCalculCommission entity) {
        if (entity == null) {
            return null;
        }

        // Éviter les problèmes de lazy loading avec try-catch
        Long collecteurId = null;
        String collecteurNom = null;
        String collecteurPrenom = null;
        
        try {
            if (entity.getCollecteur() != null) {
                collecteurId = entity.getCollecteur().getId();
                collecteurNom = entity.getCollecteur().getNom();
                collecteurPrenom = entity.getCollecteur().getPrenom();
            }
        } catch (Exception e) {
            // Ignore les erreurs de lazy loading
            System.out.println("⚠️ Lazy loading error pour collecteur: " + e.getMessage());
        }

        return HistoriqueCalculCommissionDTO.builder()
                .id(entity.getId())
                .collecteurId(collecteurId)
                .collecteurNom(collecteurNom)
                .collecteurPrenom(collecteurPrenom)
                .dateDebut(entity.getDateDebut())
                .dateFin(entity.getDateFin())
                .montantCommissionTotal(entity.getMontantCommissionTotal())
                .montantTvaTotal(entity.getMontantTvaTotal())
                .nombreClients(entity.getNombreClients())
                .statut(entity.getStatut() != null ? entity.getStatut().name() : null)
                .dateCalcul(entity.getDateCalcul())
                .calculePar(entity.getCalculePar())
                .agenceId(entity.getAgenceId())
                .remunere(entity.getRemunere())
                .dateRemuneration(entity.getDateRemuneration())
                .remunerationId(entity.getRemunerationId())
                .build();
    }

    /**
     * Méthodes utilitaires
     */
    public String getPeriodeDescription() {
        return String.format("%s → %s", dateDebut, dateFin);
    }

    public BigDecimal getMontantTotalAvecTva() {
        if (montantCommissionTotal == null) return montantTvaTotal != null ? montantTvaTotal : BigDecimal.ZERO;
        if (montantTvaTotal == null) return montantCommissionTotal;
        return montantCommissionTotal.add(montantTvaTotal);
    }

    public boolean peutEtreRemunere() {
        return "CALCULE".equals(statut) && !Boolean.TRUE.equals(remunere);
    }
}