package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.entities.Collecteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CollecteurService {
    List<Collecteur> getAllCollecteurs();

    Page<Collecteur> getAllCollecteurs(Pageable pageable);

    Optional<Collecteur> getCollecteurById(Long id);

    Collecteur saveCollecteur(Collecteur collecteur);

    // Ajoutez cette méthode
    Collecteur saveCollecteur(CollecteurDTO dto, Long agenceId);

    void deactivateCollecteur(Long id);

    List<Collecteur> findByAgenceId(Long agenceId);

    Page<Collecteur> findByAgenceId(Long agenceId, Pageable pageable);

    Collecteur updateCollecteur(Collecteur collecteur);

    // Ajoutez cette méthode
    Collecteur updateCollecteur(Long id, CollecteurDTO dto);

    CollecteurDTO convertToDTO(Collecteur collecteur);

    Collecteur convertToEntity(CollecteurDTO dto);

    void updateCollecteurFromDTO(Collecteur collecteur, CollecteurDTO dto);

    boolean hasActiveOperations(Collecteur collecteur);

    Collecteur updateMontantMaxRetrait(Long collecteurId, Double nouveauMontant, String justification);
}