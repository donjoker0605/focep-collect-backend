package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.ClientDTO;
import org.example.collectfocep.entities.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "agence.id", target = "agenceId")
    ClientDTO toDTO(Client client);

    @Mapping(target = "collecteur", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "id", ignore = true)
    Client toEntity(ClientDTO clientDTO);

    @Mapping(target = "collecteur", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDTO(ClientDTO clientDTO, @MappingTarget Client client);
}