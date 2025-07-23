package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.Collecteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CollecteurService {

    // ✅ MÉTHODES PRINCIPALES - NOUVELLES ET SÉCURISÉES

    /**
     * Crée un nouveau collecteur avec le mot de passe fourni
     */
    Collecteur saveCollecteur(CollecteurCreateDTO dto);

    /**
     * Met à jour un collecteur existant
     */
    Collecteur updateCollecteur(Long id, CollecteurUpdateDTO dto);

    /**
     * 🔥 NOUVELLE: Réinitialise le mot de passe d'un collecteur par l'admin
     */
    void resetCollecteurPassword(Long collecteurId, String newPassword);

    /**
     * Récupère les collecteurs filtrés par agence (avec sécurité)
     */
    Page<Collecteur> getCollecteursByAgence(Long agenceId, Pageable pageable);

    /**
     * Recherche des collecteurs dans une agence
     */
    Page<Collecteur> searchCollecteursByAgence(Long agenceId, String search, Pageable pageable);

    /**
     * Bascule le statut actif/inactif d'un collecteur
     */
    Collecteur toggleCollecteurStatus(Long collecteurId);

    /**
     * Récupère les statistiques d'un collecteur
     */
    CollecteurStatisticsDTO getCollecteurStatistics(Long collecteurId);

    // ✅ MÉTHODES EXISTANTES CONSERVÉES

    /**
     * Met à jour le montant maximum de retrait
     */
    Collecteur updateMontantMaxRetrait(Long collecteurId, BigDecimal nouveauMontant, String justification);

    /**
     * Récupère un collecteur par ID
     */
    Optional<Collecteur> getCollecteurById(Long id);

    /**
     * Convertit un collecteur en DTO
     */
    CollecteurDTO convertToDTO(Collecteur collecteur);

    /**
     * Trouve les collecteurs par agence
     */
    List<Collecteur> findByAgenceId(Long agenceId);

    /**
     * Trouve les collecteurs par agence avec pagination
     */
    Page<Collecteur> findByAgenceId(Long agenceId, Pageable pageable);

    /**
     * Désactive un collecteur (soft delete)
     */
    void deactivateCollecteur(Long id);

    /**
     * Récupère tous les collecteurs
     */
    List<Collecteur> getAllCollecteurs();

    /**
     * Récupère tous les collecteurs avec pagination
     */
    Page<Collecteur> getAllCollecteurs(Pageable pageable);

    /**
     * Vérifie si un collecteur a des opérations actives
     */
    boolean hasActiveOperations(Collecteur collecteur);

    /**
     * Récupère les statistiques du dashboard d'un collecteur
     */
    CollecteurDashboardDTO getDashboardStats(Long collecteurId);

    /**
     * 🔥 NOUVELLE: Statistiques avec plage de dates
     */
    Map<String, Object> getCollecteurStatisticsWithDateRange(Long collecteurId, LocalDate dateDebut, LocalDate dateFin);

    /**
     * 🔥 NOUVELLE: Métriques de performance
     */
    Map<String, Object> getCollecteurPerformanceMetrics(Long collecteurId, LocalDate dateDebut, LocalDate dateFin);

    /**
     * 🔥 NOUVELLE: Mise à jour du statut
     */
    void updateCollecteurStatus(Long collecteurId, Boolean active);

    // ✅ MÉTHODES DEPRECATED - CONSERVÉES POUR COMPATIBILITÉ

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