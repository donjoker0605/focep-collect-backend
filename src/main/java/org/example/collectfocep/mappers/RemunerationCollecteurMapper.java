package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.RemunerationCollecteurDTO;
import org.example.collectfocep.entities.RemunerationCollecteur;
import org.example.collectfocep.entities.enums.TypeOperation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RemunerationCollecteurMapper {
    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "collecteur.ancienneteEnMois", target = "ancienneteEnMois")
    RemunerationCollecteurDTO toDTO(RemunerationCollecteur remuneration);

    default String typeOperationToString(TypeOperation typeOperation) {
        return typeOperation != null ? typeOperation.name() : null;
    }

    default TypeOperation stringToTypeOperation(String type) {
        return type != null ? TypeOperation.valueOf(type) : null;
    }
}
