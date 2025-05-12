package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.entities.Collecteur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CollecteurMapper {

    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(target = "justificationModification", ignore = true)
    CollecteurDTO toDTO(Collecteur collecteur);

    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "adresseMail", ignore = true)
    @Mapping(target = "telephone", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "ancienneteEnMois", ignore = true)
    @Mapping(target = "dateModificationMontantMax", ignore = true)
    @Mapping(target = "modifiePar", ignore = true)
    Collecteur toEntity(CollecteurDTO dto);

    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numeroCni", ignore = true)
    @Mapping(target = "adresseMail", ignore = true)
    @Mapping(target = "telephone", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "ancienneteEnMois", ignore = true)
    @Mapping(target = "dateModificationMontantMax", ignore = true)
    @Mapping(target = "modifiePar", ignore = true)
    void updateEntityFromDTO(CollecteurDTO dto, @MappingTarget Collecteur collecteur);
}