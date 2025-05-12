package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.entities.CommissionParameter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommissionMapper.class})
public interface CommissionParameterMapper {
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(source = "type", target = "typeCommission")
    @Mapping(source = "valeur", target = "valeurCommission")
    @Mapping(source = "validFrom", target = "dateDebut")
    @Mapping(source = "validTo", target = "dateFin")
    @Mapping(source = "active", target = "actif")
    @Mapping(source = "tiers", target = "paliers")
    CommissionParameterDTO toDTO(CommissionParameter parameter);

    @Mapping(source = "clientId", target = "client.id")
    @Mapping(source = "collecteurId", target = "collecteur.id")
    @Mapping(source = "agenceId", target = "agence.id")
    @Mapping(source = "typeCommission", target = "type")
    @Mapping(source = "valeurCommission", target = "valeur")
    @Mapping(source = "dateDebut", target = "validFrom")
    @Mapping(source = "dateFin", target = "validTo")
    @Mapping(source = "actif", target = "active")
    @Mapping(source = "paliers", target = "tiers")
    @Mapping(target = "id", ignore = true)
    CommissionParameter toEntity(CommissionParameterDTO dto);
}