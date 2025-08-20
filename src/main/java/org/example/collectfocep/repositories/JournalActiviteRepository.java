package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.JournalActivite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalActiviteRepository extends JpaRepository<JournalActivite, Long> {

    /**
     * Recherche par utilisateur et période - Version avec pagination
     */
    @Query("SELECT j FROM JournalActivite j " +
            "WHERE j.userId = :userId " +
            "AND j.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY j.timestamp DESC")
    Page<JournalActivite> findByUserIdAndTimestampBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Recherche par utilisateur et période - Version List directe
     * Utilisée par AdminActivityCache et autres services nécessitant une List complète
     */
    @Query("SELECT j FROM JournalActivite j " +
            "WHERE j.userId = :userId " +
            "AND j.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY j.timestamp DESC")
    List<JournalActivite> findByUserIdAndTimestampBetweenAsList(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Recherche par agence et période
     */
    @Query("SELECT j FROM JournalActivite j " +
            "WHERE j.agenceId = :agenceId " +
            "AND j.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY j.timestamp DESC")
    Page<JournalActivite> findByAgenceIdAndTimestampBetween(
            @Param("agenceId") Long agenceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Recherche par agence et période - Version List directe
     */
    @Query("SELECT j FROM JournalActivite j " +
            "WHERE j.agenceId = :agenceId " +
            "AND j.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY j.timestamp DESC")
    List<JournalActivite> findByAgenceIdAndTimestampBetweenAsList(
            @Param("agenceId") Long agenceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Recherche par action et période
     */
    @Query("SELECT j FROM JournalActivite j " +
            "WHERE j.action = :action " +
            "AND j.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY j.timestamp DESC")
    Page<JournalActivite> findByActionAndTimestampBetween(
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Statistiques par utilisateur
     */
    @Query("SELECT j.action, COUNT(j) FROM JournalActivite j " +
            "WHERE j.userId = :userId " +
            "AND j.timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY j.action")
    List<Object[]> getActivityStatsByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Activités récentes pour un utilisateur
     */
    @Query("SELECT j FROM JournalActivite j " +
            "WHERE j.userId = :userId " +
            "ORDER BY j.timestamp DESC " +
            "LIMIT :limit")
    List<JournalActivite> findRecentActivitiesByUser(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    /**
     * Compter les activités par type pour une période
     */
    @Query("SELECT COUNT(j) FROM JournalActivite j " +
            "WHERE j.action = :action " +
            "AND j.userId = :userId " +
            "AND j.timestamp BETWEEN :startDate AND :endDate")
    Long countByActionAndUserIdAndTimestampBetween(
            @Param("action") String action,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Recherche avec filtres multiples
     */
    @Query("SELECT j FROM JournalActivite j " +
            "WHERE (:userId IS NULL OR j.userId = :userId) " +
            "AND (:agenceId IS NULL OR j.agenceId = :agenceId) " +
            "AND (:action IS NULL OR j.action = :action) " +
            "AND (:entityType IS NULL OR j.entityType = :entityType) " +
            "AND j.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY j.timestamp DESC")
    Page<JournalActivite> findWithFilters(
            @Param("userId") Long userId,
            @Param("agenceId") Long agenceId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * 🔍 Trouve la dernière activité d'un utilisateur par type
     * Utilisé pour le monitoring d'inactivité des collecteurs
     */
    @Query("SELECT j.timestamp FROM JournalActivite j " +
            "WHERE j.userId = :userId " +
            "AND j.userType = :userType " +
            "ORDER BY j.timestamp DESC " +
            "LIMIT 1")
    Optional<LocalDateTime> findLastActivityByUserId(
            @Param("userId") Long userId,
            @Param("userType") String userType);

    /**
     * 🔍 Trouve la dernière activité d'un utilisateur (tous types)
     */
    @Query("SELECT j.timestamp FROM JournalActivite j " +
            "WHERE j.userId = :userId " +
            "ORDER BY j.timestamp DESC " +
            "LIMIT 1")
    Optional<LocalDateTime> findLastActivityByUserId(@Param("userId") Long userId);

}