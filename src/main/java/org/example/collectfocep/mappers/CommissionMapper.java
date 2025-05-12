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
    @Mapping(source = "montant", target = "montantCollecte")
    @Mapping(source = "dateCalcul", target = "dateDebut")
    @Mapping(source = "dateFinValidite", target = "dateFin")
    @Mapping(source = "type", target = "typeCommission")
    @Mapping(source = "valeur", target = "valeurCommission")
    @Mapping(source = "commissionParameter.tiers", target = "paliers")
    @Mapping(source = "commissionParameter.id", target = "commissionParameterId")
    CommissionCalculationDTO toCalculationDTO(Commission commission);

    @Mapping(source = "clientId", target = "client.id")
    @Mapping(source = "montantCollecte", target = "montant")
    @Mapping(source = "typeCommission", target = "type")
    @Mapping(source = "valeurCommission", target = "valeur")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "collecteur", ignore = true)
    @Mapping(target = "tva", ignore = true)
    @Mapping(target = "dateCalcul", expression = "java(LocalDateTime.now())")
    @Mapping(target = "dateFinValidite", ignore = true)
    @Mapping(target = "compte", ignore = true)
    @Mapping(target = "commissionParameter", ignore = true)
    Commission toEntity(CommissionCalculationDTO dto);

    @Mapping(target = "dateCalcul", expression = "java(LocalDateTime.now())")
    @Mapping(source = "montant", target = "montantCommission")
    @Mapping(source = "tva", target = "montantTVA")
    @Mapping(source = "type", target = "typeCalcul")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(target = "montantNet", expression = "java(commission.getMontant() - commission.getTva())")
    CommissionResult toResult(Commission commission);

    // Méthode pour mapper CommissionTier vers CommissionTierDTO
    @Mapping(target = "montantMin", source = "montantMin")
    @Mapping(target = "montantMax", source = "montantMax")
    @Mapping(target = "taux", source = "taux")
    CommissionTierDTO toTierDTO(CommissionTier tier);

    // Méthode pour mapper CommissionTierDTO vers CommissionTier
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "commissionParameter", ignore = true)
    @Mapping(target = "montantMin", source = "montantMin")
    @Mapping(target = "montantMax", source = "montantMax")
    @Mapping(target = "taux", source = "taux")
    CommissionTier toTierEntity(CommissionTierDTO dto);
}