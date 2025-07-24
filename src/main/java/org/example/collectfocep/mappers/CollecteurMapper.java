package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CollecteurCreateDTO;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.dto.CollecteurUpdateDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Collecteur;
import org.mapstruct.*;


@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CollecteurMapper {

    // ================================
    // MAPPING VERS DTO - CORRIGÉ SANS LAZY LOADING
    // ================================

    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(source = "agence.nomAgence", target = "agenceNom")
    @Mapping(target = "nombreClients", ignore = true)
    @Mapping(target = "nombreComptes", ignore = true)
    CollecteurDTO toDTO(Collecteur collecteur);

    // ================================
    // MAPPING DEPUIS CollecteurCreateDTO
    // ================================

    @Mapping(target = "agence", source = "agenceId", qualifiedByName = "mapAgence")
    @Mapping(target = "role", constant = "COLLECTEUR")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "ancienneteEnMois", constant = "0")
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "comptes", ignore = true)
    @Mapping(target = "rapport", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    Collecteur toEntity(CollecteurCreateDTO dto);

    // ================================
    // MAPPING DEPUIS CollecteurDTO (deprecated mais parfois nécessaire)
    // ================================

    @Mapping(target = "agence", source = "agenceId", qualifiedByName = "mapAgence")
    @Mapping(target = "role", constant = "COLLECTEUR")
    @Mapping(target = "clients", ignore = true) // ✅ Collections ignorées
    @Mapping(target = "comptes", ignore = true) // ✅ Collections ignorées
    @Mapping(target = "rapport", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    Collecteur toEntity(CollecteurDTO dto);

    // ================================
    // MAPPING UPDATE DEPUIS CollecteurUpdateDTO
    // ================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "clients", ignore = true) // ✅ Collections ignorées
    @Mapping(target = "comptes", ignore = true) // ✅ Collections ignorées
    @Mapping(target = "rapport", ignore = true)
    @Mapping(target = "active", ignore = true) // Ne pas modifier via update standard
    void updateEntityFromDTO(CollecteurUpdateDTO dto, @MappingTarget Collecteur collecteur);

    // ================================
    // MAPPING UPDATE DEPUIS CollecteurDTO (deprecated)
    // ================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "comptes", ignore = true)
    @Mapping(target = "rapport", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntityFromDTO(CollecteurDTO dto, @MappingTarget Collecteur collecteur);

    // ================================
    // MÉTHODE HELPER - INCHANGÉE
    // ================================

    @Named("mapAgence")
    default Agence mapAgence(Long agenceId) {
        if (agenceId == null) return null;
        Agence agence = new Agence();
        agence.setId(agenceId);
        return agence;
    }
}