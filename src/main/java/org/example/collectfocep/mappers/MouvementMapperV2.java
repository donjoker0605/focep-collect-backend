package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Mouvement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.format.DateTimeFormatter;

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

    /**
     * Mapper vers MouvementDTO - ✅ CORRIGÉ
     */
    @Mapping(source = "client", target = "client", qualifiedByName = "clientToBasicDTO")
    @Mapping(source = "collecteur", target = "collecteur", qualifiedByName = "collecteurToBasicDTO")
    @Mapping(source = "journal.id", target = "journalId")
    @Mapping(source = "dateOperation", target = "dateOperation")
    @Mapping(source = "libelle", target = "description")
    @Mapping(target = "reference", expression = "java(generateReference(mouvement))")
    @Mapping(target = "status", constant = "COMPLETED")
    @Mapping(source = "journal.reference", target = "journalReference")
    @Mapping(source = "compteSource.id", target = "compteSourceId")
    @Mapping(source = "compteDestination.id", target = "compteDestinationId")
    @Mapping(target = "commissionMontant", ignore = true)
    @Mapping(target = "commissionType", ignore = true)
    @Mapping(source = "dateOperation", target = "dateCreation")
    @Mapping(source = "dateOperation", target = "dateModification")
    MouvementDTO toDTO(Mouvement mouvement);

    @Named("clientToBasicDTO")
    default ClientBasicDTO clientToBasicDTO(Client client) {
        if (client == null) return null;

        return ClientBasicDTO.builder()
                .id(client.getId())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .numeroCni(client.getNumeroCni())
                .numeroCompte(client.getNumeroCompte())
                .telephone(client.getTelephone())
                .ville(client.getVille())
                .quartier(client.getQuartier())
                .valide(client.getValide())
                .build();
    }

    @Named("collecteurToBasicDTO")
    default CollecteurBasicDTO collecteurToBasicDTO(Collecteur collecteur) {
        if (collecteur == null) return null;

        return CollecteurBasicDTO.builder()
                .id(collecteur.getId())
                .nom(collecteur.getNom())
                .prenom(collecteur.getPrenom())
                .adresseMail(collecteur.getAdresseMail())
                .telephone(collecteur.getTelephone())
                .agenceId(collecteur.getAgence() != null ? collecteur.getAgence().getId() : null)
                .agenceNom(collecteur.getAgence() != null ? collecteur.getAgence().getNom() : null)
                .build();
    }

    @Named("generateReference")
    default String generateReference(Mouvement mouvement) {
        if (mouvement.getId() == null || mouvement.getDateOperation() == null) {
            return "MVT-PENDING-" + System.currentTimeMillis();
        }
        return String.format("MVT-%d-%s", mouvement.getId(),
                mouvement.getDateOperation().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    }
}