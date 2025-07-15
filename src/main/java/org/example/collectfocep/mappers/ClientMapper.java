package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.ClientDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.mapstruct.*;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(source = "latitude", target = "latitude", qualifiedByName = "bigDecimalToDouble")
    @Mapping(source = "longitude", target = "longitude", qualifiedByName = "bigDecimalToDouble")
    ClientDTO toDTO(Client client);

    @Mapping(source = "collecteurId", target = "collecteur", qualifiedByName = "idToCollecteur")
    @Mapping(source = "agenceId", target = "agence", qualifiedByName = "idToAgence")
    @Mapping(source = "latitude", target = "latitude", qualifiedByName = "doubleToBigDecimal")
    @Mapping(source = "longitude", target = "longitude", qualifiedByName = "doubleToBigDecimal")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    Client toEntity(ClientDTO clientDTO);

    @Mapping(source = "collecteurId", target = "collecteur", qualifiedByName = "idToCollecteur")
    @Mapping(source = "agenceId", target = "agence", qualifiedByName = "idToAgence")
    @Mapping(source = "latitude", target = "latitude", qualifiedByName = "doubleToBigDecimal")
    @Mapping(source = "longitude", target = "longitude", qualifiedByName = "doubleToBigDecimal")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "numeroCompte", ignore = true) // Ne pas modifier le numéro de compte
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

    @Named("bigDecimalToDouble")
    default Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    @Named("doubleToBigDecimal")
    default BigDecimal doubleToBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}