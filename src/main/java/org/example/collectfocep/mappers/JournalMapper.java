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
        if (dto == null) return null;

        Collecteur collecteur = collecteurRepository.findById(dto.getCollecteurId())
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé avec ID: " + dto.getCollecteurId()));

        return Journal.builder()
                .id(dto.getId())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .collecteur(collecteur)
                .estCloture(dto.getEstCloture() != null ? dto.getEstCloture() : false)
                .dateCloture(dto.getDateCloture())
                .reference(dto.getReference())
                .statut(dto.getStatut() != null ? dto.getStatut() : "OUVERT")
                .build();
    }

    public JournalDTO toDTO(Journal entity) {
        if (entity == null) return null;

        return JournalDTO.builder()
                .id(entity.getId())
                .reference(entity.getReference())
                .dateDebut(entity.getDateDebut())
                .dateFin(entity.getDateFin())
                // Optionnel: convertir en LocalDateTime si nécessaire
                .dateOuverture(entity.getDateDebut() != null ? entity.getDateDebut().atStartOfDay() : null)
                .dateCloture(entity.getDateCloture())
                .estCloture(entity.isEstCloture())
                .collecteurId(entity.getCollecteur() != null ? entity.getCollecteur().getId() : null)
                .collecteurNom(entity.getCollecteur() != null ?
                        entity.getCollecteur().getNom() + " " + entity.getCollecteur().getPrenom() : null)
                .statut(entity.getStatut())
                .build();
    }
}