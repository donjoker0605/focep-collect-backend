package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.CompteDTO;
import org.example.collectfocep.entities.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompteMapper {

    @Mapping(target = "collecteurId", source = "compte", qualifiedByName = "getCollecteurId")
    @Mapping(target = "collecteurNom", source = "compte", qualifiedByName = "getCollecteurNom")
    CompteDTO toDTO(Compte compte);

    List<CompteDTO> toDTOList(List<Compte> comptes);

    @Named("getCollecteurId")
    default Long getCollecteurId(Compte compte) {
        if (compte instanceof CompteCollecteur) {
            return ((CompteCollecteur) compte).getCollecteur() != null ?
                    ((CompteCollecteur) compte).getCollecteur().getId() : null;
        } else if (compte instanceof CompteServiceEntity) {
            return ((CompteServiceEntity) compte).getCollecteur() != null ?
                    ((CompteServiceEntity) compte).getCollecteur().getId() : null;
        } else if (compte instanceof CompteManquant) {
            return ((CompteManquant) compte).getCollecteur() != null ?
                    ((CompteManquant) compte).getCollecteur().getId() : null;
        } else if (compte instanceof CompteSalaireCollecteur) {
            return ((CompteSalaireCollecteur) compte).getCollecteur() != null ?
                    ((CompteSalaireCollecteur) compte).getCollecteur().getId() : null;
        } else if (compte instanceof CompteAttente) {
            return ((CompteAttente) compte).getCollecteur() != null ?
                    ((CompteAttente) compte).getCollecteur().getId() : null;
        } else if (compte instanceof CompteChargeCollecte) {
            // CompteChargeCollecte n'a pas de collecteur, il appartient à une agence
            return null;
        }else if(compte instanceof CompteSysteme) {
            return null;
        }
        return null;
    }

    @Named("getCollecteurNom")
    default String getCollecteurNom(Compte compte) {
        Collecteur collecteur = null;

        if (compte instanceof CompteCollecteur) {
            collecteur = ((CompteCollecteur) compte).getCollecteur();
        } else if (compte instanceof CompteServiceEntity) {
            collecteur = ((CompteServiceEntity) compte).getCollecteur();
        } else if (compte instanceof CompteManquant) {
            collecteur = ((CompteManquant) compte).getCollecteur();
        } else if (compte instanceof CompteSalaireCollecteur) {
            collecteur = ((CompteSalaireCollecteur) compte).getCollecteur();
        } else if (compte instanceof CompteAttente) {
            collecteur = ((CompteAttente) compte).getCollecteur();
        } else if (compte instanceof CompteChargeCollecte) {
            // CompteChargeCollecte n'a pas de collecteur, il appartient à une agence
            collecteur = null;
        }

        if (collecteur != null && collecteur.getNom() != null && collecteur.getPrenom() != null) {
            return collecteur.getNom() + " " + collecteur.getPrenom();
        }
        
        // Gestion des comptes spéciaux
        if (compte instanceof CompteSysteme){
            return "Système";
        } else if (compte instanceof CompteChargeCollecte) {
            Agence agence = ((CompteChargeCollecte) compte).getAgence();
            return agence != null ? "Agence " + agence.getNom() : "Agence Inconnue";
        } else if (compte instanceof CompteAgence) {
            Agence agence = ((CompteAgence) compte).getAgence();
            return agence != null ? "Agence " + agence.getNom() : "Agence Inconnue";
        }
        
        return null;
    }
}