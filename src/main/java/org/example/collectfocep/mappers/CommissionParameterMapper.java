package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Agence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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

    @Mapping(source = "clientId", target = "client", qualifiedByName = "idToClient")
    @Mapping(source = "collecteurId", target = "collecteur", qualifiedByName = "idToCollecteur")
    @Mapping(source = "agenceId", target = "agence", qualifiedByName = "idToAgence")
    @Mapping(source = "typeCommission", target = "type")
    @Mapping(source = "valeurCommission", target = "valeur")
    @Mapping(source = "dateDebut", target = "validFrom")
    @Mapping(source = "dateFin", target = "validTo")
    @Mapping(source = "actif", target = "active")
    @Mapping(source = "paliers", target = "tiers")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true) // ✅ CORRECTION: Ignorer version
    CommissionParameter toEntity(CommissionParameterDTO dto);

    // Méthodes de mapping pour les entités liées
    @Named("idToClient")
    default Client idToClient(Long id) {
        if (id == null) return null;
        Client client = new Client();
        client.setId(id);
        return client;
    }

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