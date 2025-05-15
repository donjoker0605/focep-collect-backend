package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.ClientDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "agence.id", target = "agenceId")
    ClientDTO toDTO(Client client);

    @Mapping(source = "collecteurId", target = "collecteur", qualifiedByName = "idToCollecteur")
    @Mapping(source = "agenceId", target = "agence", qualifiedByName = "idToAgence")
    @Mapping(target = "id", ignore = true)
    Client toEntity(ClientDTO clientDTO);

    @Mapping(source = "collecteurId", target = "collecteur", qualifiedByName = "idToCollecteur")
    @Mapping(source = "agenceId", target = "agence", qualifiedByName = "idToAgence")
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDTO(ClientDTO clientDTO, @MappingTarget Client client);

    @Named("idToCollecteur")
    default Collecteur idToCollecteur(Long id) {
        if (id == null) return null;
        Collecteur collecteur = new Collecteur();
        collecteur.setId(id);
        return collecteur;
    }

    @Named("idToAgence")
    default Agence idToAgence(Long id) {
        if (id == null) return null;
        Agence agence = new Agence();
        agence.setId(id);
        return agence;
    }
}