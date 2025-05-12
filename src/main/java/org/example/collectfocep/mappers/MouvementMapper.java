package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.MouvementCommissionDTO;
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
}