package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.AuditLogRequest;
import org.example.collectfocep.dto.JournalActiviteDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface JournalActiviteService {

    /**
     * Enregistrer une activité avec requête complète
     */
    void logActivity(AuditLogRequest request);

    /**
     * Enregistrer une activité de manière simplifiée
     */
    void logActivity(String action, String entityType, Long entityId, Object details);

    /**
     * Récupérer les activités d'un utilisateur pour une date
     */
    Page<JournalActiviteDTO> getActivitesByUser(Long userId, LocalDate date, Pageable pageable);

    /**
     * Récupérer les activités d'une agence pour une date
     */
    Page<JournalActiviteDTO> getActivitesByAgence(Long agenceId, LocalDate date, Pageable pageable);

    /**
     * Récupérer les activités avec filtres multiples
     */
    Page<JournalActiviteDTO> getActivitesWithFilters(Long userId, Long agenceId, String action,
                                                     String entityType, LocalDate dateDebut,
                                                     LocalDate dateFin, Pageable pageable);

    /**
     * Statistiques d'activité pour un utilisateur
     */
    Map<String, Long> getActivityStats(Long userId, LocalDate dateDebut, LocalDate dateFin);
}