package org.example.collectfocep.mappers;

import org.example.collectfocep.entities.Commission;
import org.example.collectfocep.entities.RapportCommission;
import org.example.collectfocep.dto.CommissionClientDTO;
import org.example.collectfocep.dto.RapportCommissionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class})
public interface RapportCommissionMapper {

    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "commissions", target = "commissionsClients")
    RapportCommissionDTO toDTO(RapportCommission rapport);

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.nom", target = "nomClient")
    @Mapping(source = "compte.numeroCompte", target = "numeroCompte")
    @Mapping(expression = "java(mapMontantCollecte(commission))", target = "montantCollecte")
    @Mapping(expression = "java(mapMontantCommission(commission))", target = "montantCommission")
    @Mapping(expression = "java(mapMontantTVA(commission))", target = "montantTVA")
    @Mapping(source = "type", target = "typeCommission")
    @Mapping(target = "dateCalcul", expression = "java(LocalDateTime.now())")
    CommissionClientDTO toClientDTO(Commission commission);

    // Méthodes de mapping avec gestion sécurisée des BigDecimal
    default double mapMontantCollecte(Commission commission) {
        if (commission == null || commission.getMontant() == null) {
            return 0.0;
        }
        return commission.getMontant().doubleValue();
    }

    default double mapMontantCommission(Commission commission) {
        if (commission == null || commission.getMontant() == null) {
            return 0.0;
        }
        return commission.getMontant().doubleValue();
    }

    default double mapMontantTVA(Commission commission) {
        if (commission == null || commission.getTva() == null) {
            return 0.0;
        }
        return commission.getTva().doubleValue();
    }

    // Méthode de mapping pour le montant net
    default double mapMontantNet(Commission commission) {
        if (commission == null) {
            return 0.0;
        }
        return commission.getMontantNet().doubleValue();
    }

    // Méthode de mapping pour le montant total
    default double mapMontantTotal(Commission commission) {
        if (commission == null) {
            return 0.0;
        }
        return commission.getMontantTotal().doubleValue();
    }
}