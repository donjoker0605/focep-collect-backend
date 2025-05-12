package org.example.collectfocep.mappers;

import org.example.collectfocep.dto.JournalDTO;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JournalMapper {

    private final CollecteurRepository collecteurRepository;

    @Autowired
    public JournalMapper(CollecteurRepository collecteurRepository) {
        this.collecteurRepository = collecteurRepository;
    }

    public Journal toEntity(JournalDTO dto) {
        if (dto == null) {
            return null;
        }

        Collecteur collecteur = collecteurRepository.findById(dto.getCollecteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé avec ID: " + dto.getCollecteurId()));

        return Journal.builder()
                .id(dto.getId())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .collecteur(collecteur)
                .estCloture(dto.isEstCloture())
                .dateCloture(dto.getDateCloture() != null ? dto.getDateCloture().atStartOfDay() : null)
                .build();
    }

    public JournalDTO toDto(Journal entity) {
        if (entity == null) {
            return null;
        }

        return JournalDTO.builder()
                .id(entity.getId())
                .dateDebut(entity.getDateDebut())
                .dateFin(entity.getDateFin())
                .collecteurId(entity.getCollecteur().getId())
                .estCloture(entity.isEstCloture())
                .dateCloture(entity.getDateCloture() != null ? entity.getDateCloture().toLocalDate() : null)
                .build();
    }
}