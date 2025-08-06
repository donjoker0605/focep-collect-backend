package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CommissionCalculationDTO;
import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.dto.CommissionTierDTO;
import org.example.collectfocep.entities.Commission;
import org.example.collectfocep.entities.CommissionTier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class})
public interface CommissionMapper {

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(expression = "java(commission.getMontant().doubleValue())", target = "montantCollecte")
    @Mapping(source = "dateCalcul", target = "dateDebut")
    @Mapping(source = "dateFinValidite", target = "dateFin")
    @Mapping(source = "type", target = "typeCommission")
    @Mapping(expression = "java(commission.getValeur() != null ? commission.getValeur().doubleValue() : 0.0)", target = "valeurCommission")
    @Mapping(source = "commissionParameter.tiers", target = "paliers")
    @Mapping(source = "commissionParameter.id", target = "commissionParameterId")
    CommissionCalculationDTO toCalculationDTO(Commission commission);

    @Mapping(source = "clientId", target = "client.id")
    @Mapping(expression = "java(java.math.BigDecimal.valueOf(dto.getMontantCollecte()))", target = "montant")
    @Mapping(source = "typeCommission", target = "type")
    @Mapping(expression = "java(java.math.BigDecimal.valueOf(dto.getValeurCommission()))", target = "valeur")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "collecteur", ignore = true)
    @Mapping(target = "tva", ignore = true)
    @Mapping(target = "dateCalcul", expression = "java(LocalDateTime.now())")
    @Mapping(target = "dateFinValidite", ignore = true)
    @Mapping(target = "compte", ignore = true)
    @Mapping(target = "commissionParameter", ignore = true)
    @Mapping(target = "rapport", ignore = true)
    // ✅ CORRECTION: Ignorer toutes les méthodes builder personnalisées
    @Mapping(target = "montantFromDouble", ignore = true)
    @Mapping(target = "tvaFromDouble", ignore = true)
    @Mapping(target = "montantFromString", ignore = true)
    @Mapping(target = "tvaFromString", ignore = true)
    Commission toEntity(CommissionCalculationDTO dto);

    @Mapping(target = "dateCalcul", expression = "java(LocalDateTime.now())")
    @Mapping(expression = "java(commission.getMontant())", target = "montantCommission")
    @Mapping(expression = "java(commission.getTva())", target = "montantTVA")
    @Mapping(source = "type", target = "typeCalcul")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(expression = "java(commission.getMontantNet())", target = "montantNet")
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "warnings", ignore = true)
    CommissionResult toResult(Commission commission);

    // ✅ CORRECTION: Mapper CommissionTier vers CommissionTierDTO
    // Ignorer toutes les propriétés qui n'existent pas dans le DTO
    @Mapping(target = "id", source = "id")
    @Mapping(target = "montantMin", source = "montantMin")
    @Mapping(target = "montantMax", source = "montantMax")
    @Mapping(target = "taux", source = "taux")
    CommissionTierDTO toTierDTO(CommissionTier tier);

    // ✅ CORRECTION: Mapper CommissionTierDTO vers CommissionTier
    // Ignorer toutes les propriétés d'entité qui ne viennent pas du DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "commissionParameter", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(source = "montantMin", target = "montantMin")
    @Mapping(source = "montantMax", target = "montantMax")
    @Mapping(source = "taux", target = "taux")
    CommissionTier toTierEntity(CommissionTierDTO dto);
}