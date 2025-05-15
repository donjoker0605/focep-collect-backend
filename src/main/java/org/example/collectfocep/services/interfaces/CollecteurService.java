package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.CollecteurCreateDTO;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.dto.CollecteurUpdateDTO;
import org.example.collectfocep.entities.Collecteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CollecteurService {
    // Nouvelles méthodes recommandées
    Collecteur saveCollecteur(CollecteurCreateDTO dto);
    Collecteur updateCollecteur(Long id, CollecteurUpdateDTO dto);

    // Méthodes standard
    Optional<Collecteur> getCollecteurById(Long id);
    List<Collecteur> getAllCollecteurs();
    Page<Collecteur> getAllCollecteurs(Pageable pageable);
    List<Collecteur> findByAgenceId(Long agenceId);
    Page<Collecteur> findByAgenceId(Long agenceId, Pageable pageable);
    void deactivateCollecteur(Long id);
    boolean hasActiveOperations(Collecteur collecteur);
    Collecteur updateMontantMaxRetrait(Long collecteurId, Double nouveauMontant, String justification);
    CollecteurDTO convertToDTO(Collecteur collecteur);

    // Méthodes deprecated pour compatibilité - À SUPPRIMER
    @Deprecated
    Collecteur saveCollecteur(CollecteurDTO dto, Long agenceId);
    @Deprecated
    Collecteur saveCollecteur(Collecteur collecteur);
    @Deprecated
    Collecteur convertToEntity(CollecteurDTO dto);
    @Deprecated
    void updateCollecteurFromDTO(Collecteur collecteur, CollecteurDTO dto);
    @Deprecated
    Collecteur updateCollecteur(Collecteur collecteur);
    @Deprecated
    Collecteur updateCollecteur(Long id, CollecteurDTO dto);
}