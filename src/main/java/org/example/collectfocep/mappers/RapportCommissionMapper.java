package org.example.collectfocep.mappers;

import org.example.collectfocep.entities.Commission;
import org.example.collectfocep.entities.RapportCommission;
import org.example.collectfocep.dto.CommissionClientDTO;
import org.example.collectfocep.dto.RapportCommissionDTO;
import org.example.collectfocep.entities.Commission;
import org.example.collectfocep.entities.RapportCommission;
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
    @Mapping(source = "montant", target = "montantCollecte")
    @Mapping(expression = "java(mapMontantCommission(commission))", target = "montantCommission")
    @Mapping(expression = "java(mapMontantTVA(commission))", target = "montantTVA")
    @Mapping(source = "type", target = "typeCommission")
    @Mapping(target = "dateCalcul", expression = "java(LocalDateTime.now())")
    CommissionClientDTO toClientDTO(Commission commission);

    default double mapMontantCommission(Commission commission) {
        return commission != null ? commission.getMontant() : 0.0;
    }

    default double mapMontantTVA(Commission commission) {
        return commission != null ? commission.getTva() : 0.0;
    }
}