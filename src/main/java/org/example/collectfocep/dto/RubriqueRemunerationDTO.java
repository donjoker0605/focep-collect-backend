package org.example.collectfocep.dto;

import lombok.*;
import org.example.collectfocep.entities.RubriqueRemuneration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour les rubriques de rémunération selon spécification FOCEP v2
 * Utilisé pour la communication API avec le frontend React Native
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubriqueRemunerationDTO {

    private Long id;
    
    private String nom;
    
    private RubriqueRemuneration.TypeRubrique type;
    
    /**
     * Valeur de la rubrique :
     * - Si type = CONSTANT : montant fixe en FCFA
     * - Si type = PERCENTAGE : pourcentage (ex: 15.5 pour 15.5%)
     */
    private BigDecimal valeur;
    
    /**
     * Date à partir de laquelle la rubrique devient active
     * Format: YYYY-MM-DD
     */
    private LocalDate dateApplication;
    
    /**
     * Délai en jours de validité
     * null = indéfini (toujours valide après dateApplication)
     */
    private Integer delaiJours;
    
    /**
     * Liste des IDs des collecteurs concernés par cette rubrique
     */
    private List<Long> collecteurIds;
    
    /**
     * Indique si la rubrique est active
     */
    private Boolean active;
    
    /**
     * Informations calculées (en lecture seule)
     */
    private boolean currentlyValid;
    private LocalDate dateExpiration;
    private String typeLabel;
    private String valeurFormatted;

    /**
     * Constructeur depuis entité pour la réponse API
     */
    public static RubriqueRemunerationDTO fromEntity(RubriqueRemuneration entity) {
        if (entity == null) {
            return null;
        }

        RubriqueRemunerationDTO dto = RubriqueRemunerationDTO.builder()
            .id(entity.getId())
            .nom(entity.getNom())
            .type(entity.getType())
            .valeur(entity.getValeur())
            .dateApplication(entity.getDateApplication())
            .delaiJours(entity.getDelaiJours())
            .collecteurIds(entity.getCollecteurIds())
            .active(entity.isActive())
            .currentlyValid(entity.isCurrentlyValid())
            .build();

        // Calcul des champs dérivés
        if (entity.getDelaiJours() != null) {
            dto.dateExpiration = entity.getDateApplication().plusDays(entity.getDelaiJours());
        }

        // Labels pour l'affichage
        dto.typeLabel = switch (entity.getType()) {
            case CONSTANT -> "Montant fixe";
            case PERCENTAGE -> "Pourcentage";
        };

        dto.valeurFormatted = switch (entity.getType()) {
            case CONSTANT -> String.format("%,.0f FCFA", entity.getValeur());
            case PERCENTAGE -> String.format("%.2f%%", entity.getValeur());
        };

        return dto;
    }

    /**
     * Conversion vers entité pour la création/mise à jour
     */
    public RubriqueRemuneration toEntity() {
        return RubriqueRemuneration.builder()
            .id(this.id)
            .nom(this.nom)
            .type(this.type)
            .valeur(this.valeur)
            .dateApplication(this.dateApplication)
            .delaiJours(this.delaiJours)
            .collecteurIds(this.collecteurIds)
            .active(this.active)
            .build();
    }
}