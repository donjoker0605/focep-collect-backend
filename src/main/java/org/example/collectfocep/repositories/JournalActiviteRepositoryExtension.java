package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.JournalActivite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JournalActiviteRepositoryExtension {

    /**
     * Activités récentes par collecteurs (pour notifications)
     */
    @Query("SELECT ja FROM JournalActivite ja " +
            "WHERE ja.userId IN :collecteurIds " +
            "AND ja.userType = 'COLLECTEUR' " +
            "AND ja.timestamp >= :since " +
            "ORDER BY ja.timestamp DESC")
    List<JournalActivite> findRecentActivitiesByCollecteurs(
            @Param("collecteurIds") List<Long> collecteurIds,
            @Param("since") LocalDateTime since);

    /**
     * Dernière activité d'un collecteur
     */
    @Query("SELECT ja FROM JournalActivite ja " +
            "WHERE ja.userId = :collecteurId " +
            "AND ja.userType = 'COLLECTEUR' " +
            "ORDER BY ja.timestamp DESC")
    Optional<JournalActivite> findLastActivityByCollecteur(
            @Param("collecteurId") Long collecteurId,
            Pageable pageable);

    /**
     * Collecteurs inactifs
     */
    @Query("SELECT DISTINCT ja.userId FROM JournalActivite ja " +
            "WHERE ja.userId IN :collecteurIds " +
            "AND ja.userType = 'COLLECTEUR' " +
            "GROUP BY ja.userId " +
            "HAVING MAX(ja.timestamp) < :inactivityThreshold")
    List<Long> findInactiveCollecteurs(
            @Param("collecteurIds") List<Long> collecteurIds,
            @Param("inactivityThreshold") LocalDateTime inactivityThreshold);

    /**
     *  Remplace la méthode problématique
     */
    @Query("SELECT ja FROM JournalActivite ja " +
            "WHERE ja.userId = :collecteurId " +
            "AND ja.userType = 'COLLECTEUR' " +
            "AND ja.timestamp BETWEEN :start AND :end " +
            "ORDER BY ja.timestamp DESC")
    List<JournalActivite> findByCollecteurIdAndTimestampBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Version plus explicite utilisant userId directement
     */
    @Query("SELECT ja FROM JournalActivite ja " +
            "WHERE ja.userId = :userId " +
            "AND ja.userType = :userType " +
            "AND ja.timestamp BETWEEN :start AND :end " +
            "ORDER BY ja.timestamp DESC")
    List<JournalActivite> findByUserIdAndUserTypeAndTimestampBetween(
            @Param("userId") Long userId,
            @Param("userType") String userType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Activités par agence avec type d'utilisateur
     */
    @Query("SELECT ja FROM JournalActivite ja " +
            "WHERE ja.agenceId = :agenceId " +
            "AND (:userType IS NULL OR ja.userType = :userType) " +
            "AND ja.timestamp BETWEEN :start AND :end " +
            "ORDER BY ja.timestamp DESC")
    List<JournalActivite> findByAgenceIdAndUserTypeAndTimestampBetween(
            @Param("agenceId") Long agenceId,
            @Param("userType") String userType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}