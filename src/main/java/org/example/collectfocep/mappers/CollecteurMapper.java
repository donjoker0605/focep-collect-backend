package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CollecteurCreateDTO;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.dto.CollecteurUpdateDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Collecteur;
import org.mapstruct.*;

/**
 * ✅ CORRECTION: Mapper MapStruct existant avec ajouts pour DTOs manquants
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CollecteurMapper {

    // ✅ MAPPING EXISTANT - CONSERVÉ
    @Mapping(source = "agence.id", target = "agenceId")
    @Mapping(source = "agence.nom", target = "agenceNom") // Ajout pour compatibilité
    @Mapping(expression = "java(collecteur.getClients() != null ? collecteur.getClients().size() : 0)", target = "nombreClients")
    @Mapping(expression = "java(collecteur.getComptes() != null ? collecteur.getComptes().size() : 0)", target = "nombreComptes")
    CollecteurDTO toDTO(Collecteur collecteur);

    // ✅ MAPPING EXISTANT - CONSERVÉ
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

    // ✅ NOUVEAUX MAPPINGS MANQUANTS - AJOUTÉS

    /**
     * Mapper CollecteurDTO vers Entity (pour les méthodes deprecated)
     */
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

    // ✅ MAPPING UPDATE EXISTANT - CONSERVÉ
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

    /**
     * ✅ NOUVEAU: Mise à jour depuis CollecteurDTO (pour compatibilité deprecated)
     */
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

    // ✅ MÉTHODE HELPER EXISTANTE - CONSERVÉE
    @Named("mapAgence")
    default Agence mapAgence(Long agenceId) {
        if (agenceId == null) return null;
        Agence agence = new Agence();
        agence.setId(agenceId);
        return agence;
    }
}