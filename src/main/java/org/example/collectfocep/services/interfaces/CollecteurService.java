package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.Collecteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CollecteurService {
    // MÉTHODES PRINCIPALES CORRIGÉES
    Collecteur saveCollecteur(CollecteurCreateDTO dto);
    Collecteur updateCollecteur(Long id, CollecteurUpdateDTO dto);

    // NOUVELLES MÉTHODES AJOUTÉES POUR ADMIN CONTROLLER
    Page<Collecteur> getCollecteursByAgence(Long agenceId, Pageable pageable);
    Page<Collecteur> searchCollecteursByAgence(Long agenceId, String search, Pageable pageable);
    Collecteur toggleCollecteurStatus(Long collecteurId);

    // MÉTHODES CORRIGÉES AVEC BONS PARAMÈTRES
    CollecteurStatisticsDTO getCollecteurStatistics(Long collecteurId);
    Map<String, Object> getCollecteurStatisticsWithDateRange(Long collecteurId, LocalDate dateDebut, LocalDate dateFin);
    Map<String, Object> getCollecteurPerformanceMetrics(Long collecteurId, LocalDate dateDebut, LocalDate dateFin);
    void updateCollecteurStatus(Long collecteurId, Boolean active);

    // MÉTHODES STANDARD
    Optional<Collecteur> getCollecteurById(Long id);
    List<Collecteur> getAllCollecteurs();
    Page<Collecteur> getAllCollecteurs(Pageable pageable);
    List<Collecteur> findByAgenceId(Long agenceId);
    Page<Collecteur> findByAgenceId(Long agenceId, Pageable pageable);
    void deactivateCollecteur(Long id);
    boolean hasActiveOperations(Collecteur collecteur);
    Collecteur updateMontantMaxRetrait(Long collecteurId, Double nouveauMontant, String justification);
    CollecteurDTO convertToDTO(Collecteur collecteur);
    CollecteurDashboardDTO getDashboardStats(Long collecteurId);

    // MÉTHODES DEPRECATED POUR COMPATIBILITÉ
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