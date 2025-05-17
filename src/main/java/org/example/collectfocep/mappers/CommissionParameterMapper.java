package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.AgenceRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {CommissionMapper.class})
public abstract class CommissionParameterMapper {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CollecteurRepository collecteurRepository;

    @Autowired
    private AgenceRepository agenceRepository;

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "collecteur.id", target = "collecteurId")
    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(source = "type", target = "typeCommission")
    @Mapping(source = "valeur", target = "valeurCommission")
    @Mapping(source = "validFrom", target = "dateDebut")
    @Mapping(source = "validTo", target = "dateFin")
    @Mapping(source = "active", target = "actif")
    @Mapping(source = "tiers", target = "paliers")
    public abstract CommissionParameterDTO toDTO(CommissionParameter parameter);

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "collecteur", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(source = "typeCommission", target = "type")
    @Mapping(source = "valeurCommission", target = "valeur")
    @Mapping(source = "dateDebut", target = "validFrom")
    @Mapping(source = "dateFin", target = "validTo")
    @Mapping(source = "actif", target = "active")
    @Mapping(source = "paliers", target = "tiers")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    public abstract CommissionParameter toEntity(CommissionParameterDTO dto);

    @AfterMapping
    protected void setReferences(CommissionParameterDTO dto, @MappingTarget CommissionParameter entity) {
        // Charger les entités complètes depuis la base de données
        if (dto.getClientId() != null) {
            clientRepository.findById(dto.getClientId())
                    .ifPresent(entity::setClient);
        }

        if (dto.getCollecteurId() != null) {
            collecteurRepository.findById(dto.getCollecteurId())
                    .ifPresent(entity::setCollecteur);
        }

        if (dto.getAgenceId() != null) {
            agenceRepository.findById(dto.getAgenceId())
                    .ifPresent(entity::setAgence);
        }
    }
}