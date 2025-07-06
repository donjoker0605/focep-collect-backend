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
    @Query("SELECT ja FROM JournalActivite ja WHERE ja.collecteurId IN :collecteurIds AND ja.timestamp >= :since ORDER BY ja.timestamp DESC")
    List<JournalActivite> findRecentActivitiesByCollecteurs(@Param("collecteurIds") List<Long> collecteurIds, @Param("since") LocalDateTime since);

    /**
     * Dernière activité d'un collecteur
     */
    @Query("SELECT ja FROM JournalActivite ja WHERE ja.collecteurId = :collecteurId ORDER BY ja.timestamp DESC")
    Optional<JournalActivite> findLastActivityByCollecteur(@Param("collecteurId") Long collecteurId, Pageable pageable);

    /**
     * Collecteurs inactifs
     */
    @Query("SELECT DISTINCT ja.collecteurId FROM JournalActivite ja WHERE ja.collecteurId IN :collecteurIds " +
            "GROUP BY ja.collecteurId HAVING MAX(ja.timestamp) < :inactivityThreshold")
    List<Long> findInactiveCollecteurs(@Param("collecteurIds") List<Long> collecteurIds, @Param("inactivityThreshold") LocalDateTime inactivityThreshold);

    @Query("SELECT ja FROM JournalActivite ja WHERE ja.collecteurId = :collecteurId AND ja.timestamp BETWEEN :start AND :end")
    List<JournalActivite> findByCollecteurIdAndTimestampBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}