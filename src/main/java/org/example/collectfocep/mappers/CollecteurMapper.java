package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CollecteurCreateDTO;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.dto.CollecteurUpdateDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Collecteur;
import org.mapstruct.*;

/**
 * CORRECTION FINALE MAPSTRUCT
 * Supprime TOUTES les références aux méthodes deprecated builder
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE) // ✅ IMPORTANT: Ignore toutes les propriétés non mappées
public interface CollecteurMapper {

    // ================================
    // MAPPING VERS DTO - CORRECT ET SIMPLE
    // ================================

    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(source = "agence.nomAgence", target = "agenceNom")
    @Mapping(expression = "java(collecteur.getClients() != null ? collecteur.getClients().size() : 0)", target = "nombreClients")
    @Mapping(expression = "java(collecteur.getComptes() != null ? collecteur.getComptes().size() : 0)", target = "nombreComptes")
    CollecteurDTO toDTO(Collecteur collecteur);

    // ================================
    // MAPPING DEPUIS CollecteurCreateDTO - RÉDUIT AU MINIMUM
    // ================================

    @Mapping(target = "agence", source = "agenceId", qualifiedByName = "mapAgence")
    @Mapping(target = "role", constant = "COLLECTEUR")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "ancienneteEnMois", constant = "0")
    Collecteur toEntity(CollecteurCreateDTO dto);

    // ================================
    // MAPPING DEPUIS CollecteurDTO (deprecated mais nécessaire) - RÉDUIT AU MINIMUM
    // ================================

    @Mapping(target = "agence", source = "agenceId", qualifiedByName = "mapAgence")
    @Mapping(target = "role", constant = "COLLECTEUR")
    Collecteur toEntity(CollecteurDTO dto);

    // ================================
    // MAPPING UPDATE DEPUIS CollecteurUpdateDTO - RÉDUIT AU MINIMUM
    // ================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntityFromDTO(CollecteurUpdateDTO dto, @MappingTarget Collecteur collecteur);

    // ================================
    // MAPPING UPDATE DEPUIS CollecteurDTO (deprecated) - RÉDUIT AU MINIMUM
    // ================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
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