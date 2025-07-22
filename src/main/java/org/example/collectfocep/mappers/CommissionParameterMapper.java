package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.dto.CommissionTierDTO;
import org.example.collectfocep.dto.PalierCommissionDTO;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionTier;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ✅ NOUVEAU : Mapper pour CommissionParameter
 * Conversion bidirectionnelle Entity <-> DTO
 */
@Mapper(componentModel = "spring")
@Component
public interface CommissionParameterMapper {

    // ================================
    // MAPPINGS PRINCIPAUX
    // ================================

    /**
     * Entity vers DTO
     */
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.nom", target = "clientNom")
    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "collecteur.nom", target = "collecteurNom")
    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(source = "agence.nomAgence", target = "agenceNom")
    @Mapping(source = "tiers", target = "paliersCommission")
    CommissionParameterDTO toDTO(CommissionParameter entity);

    /**
     * DTO vers Entity (pour création/mise à jour)
     */
    @Mapping(target = "client", ignore = true) // Géré manuellement dans le service
    @Mapping(target = "collecteur", ignore = true) // Géré manuellement dans le service
    @Mapping(target = "agence", ignore = true) // Géré manuellement dans le service
    @Mapping(target = "tiers", ignore = true) // Géré manuellement avec validation
    @Mapping(target = "version", ignore = true) // Géré par JPA
    @Mapping(target = "valeurFromDouble", ignore = true)
    @Mapping(target = "valeurFromString", ignore = true)
    CommissionParameter toEntity(CommissionParameterDTO dto);

    /**
     * Liste Entity vers Liste DTO
     */
    List<CommissionParameterDTO> toDTOList(List<CommissionParameter> entities);

    /**
     * Liste DTO vers Liste Entity
     */
    List<CommissionParameter> toEntityList(List<CommissionParameterDTO> dtos);

    // ================================
    // MAPPINGS TIER/PALIERS
    // ================================

    /**
     * CommissionTier vers PalierCommissionDTO
     */
    @Mapping(target = "description", source = "description")
    PalierCommissionDTO tierToPalierDTO(CommissionTier tier);

    /**
     * PalierCommissionDTO vers CommissionTier
     */
    @Mapping(target = "commissionParameter", ignore = true) // Géré par le parent
    @Mapping(target = "version", ignore = true) // Géré par JPA
    CommissionTier palierDTOToTier(PalierCommissionDTO palierDTO);

    /**
     * Liste Tiers vers Liste Paliers
     */
    List<PalierCommissionDTO> tiersTopaliers(List<CommissionTier> tiers);

    /**
     * Liste Paliers vers Liste Tiers
     */
    List<CommissionTier> paliersToTiers(List<PalierCommissionDTO> paliers);

    // ================================
    // MAPPINGS SPÉCIALISÉS
    // ================================

    /**
     * Mapping pour mise à jour partielle (PATCH)
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "collecteur", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "tiers", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "valeurFromDouble", ignore = true)
    void updateEntityFromDTO(CommissionParameterDTO dto, @MappingTarget CommissionParameter entity);

    // ================================
    // MÉTHODES PERSONNALISÉES
    // ================================

    /**
     * Validation des paliers lors du mapping
     */
    default boolean validateTiers(List<PalierCommissionDTO> paliers) {
        if (paliers == null || paliers.isEmpty()) {
            return true; // Pas de paliers = valide pour types non-TIER
        }

        // Valider chaque palier individuellement
        for (PalierCommissionDTO palier : paliers) {
            if (!palier.isValid()) {
                return false;
            }
        }

        // Vérifier chevauchements
        for (int i = 0; i < paliers.size(); i++) {
            for (int j = i + 1; j < paliers.size(); j++) {
                if (paliers.get(i).overlappsWith(paliers.get(j))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Enrichissement DTO avec informations calculées
     */
    @AfterMapping
    default void enrichDTO(@MappingTarget CommissionParameterDTO dto, CommissionParameter entity) {
        // Validation cohérence
        if (!dto.isValidScope()) {
            throw new IllegalStateException("Scope invalide pour paramètre ID: " + entity.getId());
        }
    }

    /**
     * Validation avant mapping vers entity
     */
    @BeforeMapping
    default void validateBeforeMapping(CommissionParameterDTO dto) {
        if (dto.getTypeCommission() == null) {
            throw new IllegalArgumentException("Type de commission requis");
        }

        if (!dto.isValid()) {
            throw new IllegalArgumentException("DTO invalide: " + dto.getValidationError());
        }
    }

    // ================================
    // EXPRESSIONS CUSTOM
    // ================================

    /**
     * Formatage du nom complet pour affichage
     */
    @Named("formatNomComplet")
    default String formatNomComplet(String nom, String prenom) {
        if (nom == null && prenom == null) return null;
        if (nom == null) return prenom;
        if (prenom == null) return nom;
        return nom + " " + prenom;
    }

    /**
     * Calcul description automatique du paramètre
     */
    @Named("generateDescription")
    default String generateDescription(CommissionParameter entity) {
        if (entity.getType() == null) return "";

        String scope = "";
        if (entity.getClient() != null) scope = "Client: " + entity.getClient().getNom();
        else if (entity.getCollecteur() != null) scope = "Collecteur: " + entity.getCollecteur().getNom();
        else if (entity.getAgence() != null) scope = "Agence: " + entity.getAgence().getNomAgence();

        // Gérer BigDecimal pour valeur
        double valeurDouble = entity.getValeur() != null ? entity.getValeur().doubleValue() : 0.0;

        return String.format("%s - %s (%.2f)",
                entity.getType(),
                scope,
                valeurDouble);
    }
}