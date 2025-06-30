package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsernameAndTimestampBetween(String username, LocalDateTime start, LocalDateTime end);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Recherche paginée par utilisateur et période
     */
    Page<AuditLog> findByUsernameAndTimestampBetween(
            String username,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    /**
     * Recherche paginée par période uniquement
     */
    Page<AuditLog> findByTimestampBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    /**
     * Recherche par action et période
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action = :action " +
            "AND a.timestamp BETWEEN :start AND :end " +
            "ORDER BY a.timestamp DESC")
    List<AuditLog> findByActionAndPeriod(
            @Param("action") String action,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Compter les actions par utilisateur
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a " +
            "WHERE a.username = :username " +
            "AND a.timestamp BETWEEN :start AND :end " +
            "GROUP BY a.action")
    List<Object[]> countActionsByUser(
            @Param("username") String username,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Recherche par type d'entité et période
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType " +
            "AND a.timestamp BETWEEN :start AND :end " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByEntityTypeAndPeriod(
            @Param("entityType") String entityType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    /**
     * Dernières activités d'un utilisateur
     */
    @Query("SELECT a FROM AuditLog a WHERE a.username = :username " +
            "ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentByUsername(
            @Param("username") String username,
            Pageable pageable
    );

    /**
     * Recherche d'activités suspectes (erreurs répétées)
     */
    @Query("SELECT a FROM AuditLog a WHERE a.details LIKE 'ERROR:%' " +
            "AND a.timestamp > :since " +
            "ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentErrors(@Param("since") LocalDateTime since);

    /**
     * Statistiques par jour
     */
    @Query("SELECT DATE(a.timestamp) as date, COUNT(a) as count " +
            "FROM AuditLog a " +
            "WHERE a.timestamp BETWEEN :start AND :end " +
            "GROUP BY DATE(a.timestamp) " +
            "ORDER BY DATE(a.timestamp)")
    List<Object[]> getDailyStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}