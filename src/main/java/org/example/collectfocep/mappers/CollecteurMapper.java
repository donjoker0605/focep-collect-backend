package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CollecteurCreateDTO;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.dto.CollecteurUpdateDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Collecteur;
import org.mapstruct.*;

/**
 * Mapper MapStruct corrigé - SUPPRESSION des mappings deprecated
 * SOLUTION: Ne pas essayer d'ignorer les méthodes deprecated qui n'existent pas dans le builder
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CollecteurMapper {

    // ================================
    // MAPPING VERS DTO - CORRECT
    // ================================

    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(source = "agence.nom", target = "agenceNom")
    @Mapping(expression = "java(collecteur.getClients() != null ? collecteur.getClients().size() : 0)", target = "nombreClients")
    @Mapping(expression = "java(collecteur.getComptes() != null ? collecteur.getComptes().size() : 0)", target = "nombreComptes")
    CollecteurDTO toDTO(Collecteur collecteur);

    // ================================
    // MAPPING DEPUIS CollecteurCreateDTO - SIMPLIFIÉ
    // ================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agence", source = "agenceId", qualifiedByName = "mapAgence")
    @Mapping(target = "role", constant = "COLLECTEUR")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "ancienneteEnMois", constant = "0")
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "dateModificationMontantMax", ignore = true)
    @Mapping(target = "modifiePar", ignore = true)
    @Mapping(target = "rapport", ignore = true)
    @Mapping(target = "rapportId", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "comptes", ignore = true)
    @Mapping(target = "fcmToken", ignore = true)
    @Mapping(target = "fcmTokenUpdatedAt", ignore = true)
    Collecteur toEntity(CollecteurCreateDTO dto);

    // ================================
    // MAPPING DEPUIS CollecteurDTO (deprecated mais nécessaire) - SIMPLIFIÉ
    // ================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agence", source = "agenceId", qualifiedByName = "mapAgence")
    @Mapping(target = "role", constant = "COLLECTEUR")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "dateModificationMontantMax", ignore = true)
    @Mapping(target = "modifiePar", ignore = true)
    @Mapping(target = "rapport", ignore = true)
    @Mapping(target = "rapportId", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "comptes", ignore = true)
    @Mapping(target = "fcmToken", ignore = true)
    @Mapping(target = "fcmTokenUpdatedAt", ignore = true)
    Collecteur toEntity(CollecteurDTO dto);

    // ================================
    // MAPPING UPDATE DEPUIS CollecteurUpdateDTO - SIMPLIFIÉ
    // ================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "agenceId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "numeroCni", ignore = true)
    @Mapping(target = "adresseMail", ignore = true)
    @Mapping(target = "ancienneteEnMois", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "dateModificationMontantMax", ignore = true)
    @Mapping(target = "modifiePar", ignore = true)
    @Mapping(target = "rapport", ignore = true)
    @Mapping(target = "rapportId", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "comptes", ignore = true)
    @Mapping(target = "fcmToken", ignore = true)
    @Mapping(target = "fcmTokenUpdatedAt", ignore = true)
    void updateEntityFromDTO(CollecteurUpdateDTO dto, @MappingTarget Collecteur collecteur);

    // ================================
    // MAPPING UPDATE DEPUIS CollecteurDTO (deprecated mais nécessaire) - SIMPLIFIÉ
    // ================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agence", ignore = true)
    @Mapping(target = "agenceId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "numeroCni", ignore = true)
    @Mapping(target = "adresseMail", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "dateModificationMontantMax", ignore = true)
    @Mapping(target = "modifiePar", ignore = true)
    @Mapping(target = "rapport", ignore = true)
    @Mapping(target = "rapportId", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "comptes", ignore = true)
    @Mapping(target = "fcmToken", ignore = true)
    @Mapping(target = "fcmTokenUpdatedAt", ignore = true)
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