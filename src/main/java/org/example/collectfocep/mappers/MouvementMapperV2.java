package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.MouvementCommissionDTO;
import org.example.collectfocep.dto.MouvementProjection;
import org.example.collectfocep.entities.Mouvement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MouvementMapperV2 {

    /**
     * Mapping depuis une entité Mouvement vers DTO
     * Utilise les méthodes utilitaires pour éviter lazy loading
     */
    @Mapping(target = "typeOperation", source = "sens")
    @Mapping(target = "compteSource", source = "mouvement", qualifiedByName = "getCompteSourceNumero")
    @Mapping(target = "compteDestination", source = "mouvement", qualifiedByName = "getCompteDestinationNumero")
    MouvementCommissionDTO toCommissionDto(Mouvement mouvement);

    /**
     * Mapping depuis une projection vers DTO (plus efficace)
     */
    @Mapping(target = "typeOperation", source = "sens")
    @Mapping(target = "compteSource", source = "compteSourceNumero")
    @Mapping(target = "compteDestination", source = "compteDestinationNumero")
    MouvementCommissionDTO projectionToDto(MouvementProjection projection);

    @Named("getCompteSourceNumero")
    default String getCompteSourceNumero(Mouvement mouvement) {
        return mouvement.getCompteSourceNumero();
    }

    @Named("getCompteDestinationNumero")
    default String getCompteDestinationNumero(Mouvement mouvement) {
        return mouvement.getCompteDestinationNumero();
    }
}