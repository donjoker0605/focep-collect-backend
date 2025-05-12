package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.MouvementCommissionDTO;
import org.example.collectfocep.dto.RepartitionCommissionDTO;
import org.example.collectfocep.entities.CommissionRepartition;
import org.example.collectfocep.entities.Compte;
import org.example.collectfocep.entities.Mouvement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class})
public interface RepartitionCommissionMapper {
    @Mapping(source = "collecteur.id", target = "partCollecteur")
    @Mapping(target = "dateRepartition", expression = "java(LocalDateTime.now())")
    @Mapping(source = "mouvements", target = "mouvements", qualifiedByName = "mouvementToDTO")
    RepartitionCommissionDTO toDTO(CommissionRepartition repartition);

    @Named("mouvementToDTO")
    @Mapping(source = "compteSource", target = "compteSource", qualifiedByName = "mapCompte")
    @Mapping(source = "compteDestination", target = "compteDestination", qualifiedByName = "mapCompte")
    @Mapping(source = "sens", target = "typeOperation") // Ajout de ce mapping
    MouvementCommissionDTO toMouvementDTO(Mouvement mouvement);

    @Named("mapCompte")
    default String mapCompte(Compte compte) {
        return compte != null ? compte.getNomCompte() : null;
    }
}
