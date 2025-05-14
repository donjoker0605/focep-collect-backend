package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.AgenceDTO;
import org.example.collectfocep.entities.Agence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper pour convertir entre Agence et AgenceDTO
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AgenceMapper {

    /**
     * Conversion d'Agence vers AgenceDTO
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "codeAgence", target = "codeAgence")
    @Mapping(source = "nomAgence", target = "nomAgence")
    AgenceDTO toDTO(Agence agence);

    /**
     * Conversion d'AgenceDTO vers Agence
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "codeAgence", target = "codeAgence")
    @Mapping(source = "nomAgence", target = "nomAgence")
    Agence toEntity(AgenceDTO dto);

    /**
     * Mise à jour d'une entité Agence à partir d'un AgenceDTO
     */
    @Mapping(source = "nomAgence", target = "nomAgence")
    @Mapping(source = "codeAgence", target = "codeAgence")
    void updateEntityFromDTO(AgenceDTO dto, @MappingTarget Agence agence);
}

// N'oubliez pas d'ajouter la dépendance MapStruct dans votre pom.xml si ce n'est pas déjà fait