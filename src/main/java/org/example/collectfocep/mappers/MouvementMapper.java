package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.MouvementCommissionDTO;
import org.example.collectfocep.dto.MouvementDTO;
import org.example.collectfocep.entities.Mouvement;
import org.springframework.stereotype.Component;

@Component
public class MouvementMapper {

    /**
     * Convertit un mouvement en DTO pour les commissions
     */
    public MouvementCommissionDTO toCommissionDto(Mouvement mouvement) {
        if (mouvement == null) {
            return null;
        }

        MouvementCommissionDTO dto = new MouvementCommissionDTO();
        dto.setTypeOperation(mouvement.getSens());
        dto.setMontant(mouvement.getMontant());
        dto.setCompteSource(mouvement.getCompteSource() != null ?
                mouvement.getCompteSource().getNumeroCompte() : null);
        dto.setCompteDestination(mouvement.getCompteDestination() != null ?
                mouvement.getCompteDestination().getNumeroCompte() : null);
        dto.setLibelle(mouvement.getLibelle());
        dto.setDateOperation(mouvement.getDateOperation());

        return dto;
    }

    /**
     * Convertit un mouvement en MouvementDTO
     */
    public MouvementDTO toDTO(Mouvement mouvement) {
        if (mouvement == null) {
            return null;
        }

        MouvementDTO dto = new MouvementDTO();
        dto.setId(mouvement.getId());
        dto.setMontant(mouvement.getMontant());
        dto.setLibelle(mouvement.getLibelle());
        dto.setSens(mouvement.getSens());
        dto.setDateHeure(mouvement.getDateOperation());
        dto.setDateCreation(mouvement.getDateOperation());
        dto.setDateModification(mouvement.getDateOperation()); // Fallback
        dto.setDescription(mouvement.getLibelle());
        dto.setStatus("COMPLETED");

        // Informations sur les comptes
        if (mouvement.getCompteSource() != null) {
            dto.setCompteSourceId(mouvement.getCompteSource().getId());
        }
        if (mouvement.getCompteDestination() != null) {
            dto.setCompteDestinationId(mouvement.getCompteDestination().getId());
        }

        // Informations sur le journal
        if (mouvement.getJournal() != null) {
            dto.setJournalId(mouvement.getJournal().getId());
        }

        // Générer une référence
        dto.setReference(generateReference(mouvement));

        return dto;
    }

    private String generateReference(Mouvement mouvement) {
        if (mouvement.getId() == null) {
            return "MVT-PENDING-" + System.currentTimeMillis();
        }
        return "MVT-" + mouvement.getId();
    }
}