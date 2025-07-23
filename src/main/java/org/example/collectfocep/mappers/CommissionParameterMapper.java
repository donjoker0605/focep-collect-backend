package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.dto.CommissionTierDTO;
import org.example.collectfocep.dto.PalierCommissionDTO;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionTier;
import org.example.collectfocep.entities.CommissionType;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * ✅ MAPPER FINAL CORRIGÉ : CommissionParameter
 * Résout tous les problèmes de propriétés non mappées
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@Component
public interface CommissionParameterMapper {

    // ================================
    // MAPPINGS PRINCIPAUX
    // ================================

    /**
     * Entity vers DTO - Mapping simple et sûr
     */
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.nom", target = "clientNom")
    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "collecteur.nom", target = "collecteurNom")
    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(source = "agence.nomAgence", target = "agenceNom")
    @Mapping(source = "tiers", target = "paliersCommission")
    @Mapping(expression = "java(bigDecimalToDouble(entity.getValeur()))", target = "valeur")
    CommissionParameterDTO toDTO(CommissionParameter entity);

    /**
     * DTO vers Entity - TOUTES LES PROPRIÉTÉS BUILDER IGNORÉES
     */
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "collecteur", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "tiers", ignore = true)
    @Mapping(target = "version", ignore = true)
    // ✅ CORRECTION CRITIQUE : Ignorer explicitement toutes les méthodes builder
    @Mapping(target = "valeurFromDouble", ignore = true)
    @Mapping(target = "valeurFromString", ignore = true)
    @Mapping(expression = "java(doubleToBigDecimal(dto.getValeur()))", target = "valeur")
    CommissionParameter toEntity(CommissionParameterDTO dto);

    /**
     * Mise à jour partielle - TOUTES LES PROPRIÉTÉS PROBLÉMATIQUES IGNORÉES
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "collecteur", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "tiers", ignore = true)
    @Mapping(target = "version", ignore = true)
    // ✅ CORRECTION CRITIQUE : Ignorer les méthodes builder
    @Mapping(target = "valeurFromDouble", ignore = true)
//    @Mapping(target = "valeurFromString", ignore = true)
    @Mapping(expression = "java(doubleToBigDecimal(dto.getValeur()))", target = "valeur")
    void updateEntityFromDTO(CommissionParameterDTO dto, @MappingTarget CommissionParameter entity);

    // ================================
    // MAPPINGS PALIERS/TIERS
    // ================================

    /**
     * CommissionTier vers PalierCommissionDTO
     */
    @Mapping(source = "montantMin", target = "montantMin")
    @Mapping(source = "montantMax", target = "montantMax")
    @Mapping(source = "taux", target = "taux")
    @Mapping(source = "description", target = "description")
    PalierCommissionDTO toPalierDTO(CommissionTier tier);

    /**
     * PalierCommissionDTO vers CommissionTier
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "commissionParameter", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(source = "montantMin", target = "montantMin")
    @Mapping(source = "montantMax", target = "montantMax")
    @Mapping(source = "taux", target = "taux")
    @Mapping(source = "description", target = "description")
    CommissionTier toPalierEntity(PalierCommissionDTO dto);

    /**
     * Liste de paliers
     */
    List<PalierCommissionDTO> toPalierDTOList(List<CommissionTier> tiers);
    List<CommissionTier> toPalierEntityList(List<PalierCommissionDTO> dtos);

    // ================================
    // MAPPINGS ALTERNATIFS
    // ================================

    /**
     * CommissionTier vers CommissionTierDTO
     */
    @Mapping(source = "montantMin", target = "montantMin")
    @Mapping(source = "montantMax", target = "montantMax")
    @Mapping(source = "taux", target = "taux")
    CommissionTierDTO toTierDTO(CommissionTier tier);

    /**
     * CommissionTierDTO vers CommissionTier
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "commissionParameter", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(source = "montantMin", target = "montantMin")
    @Mapping(source = "montantMax", target = "montantMax")
    @Mapping(source = "taux", target = "taux")
    CommissionTier toTierEntity(CommissionTierDTO dto);

    // ================================
    // MÉTHODES DE VALIDATION
    // ================================

    /**
     * Valide la cohérence des paliers de commission
     */
    default boolean validateTiers(List<PalierCommissionDTO> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return true;
        }

        for (PalierCommissionDTO tier : tiers) {
            if (tier.getMontantMin() == null || tier.getTaux() == null) {
                return false;
            }
            if (tier.getMontantMin() < 0 || tier.getTaux() < 0 || tier.getTaux() > 100) {
                return false;
            }
            if (tier.getMontantMax() != null && tier.getMontantMin() >= tier.getMontantMax()) {
                return false;
            }
        }

        for (int i = 0; i < tiers.size(); i++) {
            for (int j = i + 1; j < tiers.size(); j++) {
                if (tiersOverlap(tiers.get(i), tiers.get(j))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Vérifie si deux paliers se chevauchent
     */
    default boolean tiersOverlap(PalierCommissionDTO tier1, PalierCommissionDTO tier2) {
        Double min1 = tier1.getMontantMin();
        Double max1 = tier1.getMontantMax();
        Double min2 = tier2.getMontantMin();
        Double max2 = tier2.getMontantMax();

        if (max1 == null) max1 = Double.MAX_VALUE;
        if (max2 == null) max2 = Double.MAX_VALUE;

        return min1 < max2 && max1 > min2;
    }

    // ================================
    // MÉTHODES UTILITAIRES SÉCURISÉES
    // ================================

    /**
     * Convertit BigDecimal vers Double de manière sécurisée
     */
    default Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    /**
     * Convertit Double vers BigDecimal de manière sécurisée
     */
    default BigDecimal doubleToBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    /**
     * Détermine le scope d'un paramètre de commission
     */
    default String determineScope(CommissionParameter parameter) {
        if (parameter.getClient() != null) return "CLIENT";
        if (parameter.getCollecteur() != null) return "COLLECTEUR";
        if (parameter.getAgence() != null) return "AGENCE";
        return "UNKNOWN";
    }
}