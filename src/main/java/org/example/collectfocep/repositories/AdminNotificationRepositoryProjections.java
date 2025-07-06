package org.example.collectfocep.repositories;

import org.example.collectfocep.dto.NotificationDashboardProjection;
import org.example.collectfocep.dto.NotificationStatsProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminNotificationRepositoryProjections {

    /**
     * Dashboard optimisé avec projections
     */
    @Query("SELECT " +
            "an.id as id, " +
            "an.type as type, " +
            "an.priority as priority, " +
            "an.title as title, " +
            "an.message as message, " +
            "an.dateCreation as dateCreation, " +
            "an.lu as lu, " +
            "an.groupedCount as groupedCount, " +
            "CONCAT(c.nom, ' ', c.prenom) as collecteurNom, " +
            "a.nomAgence as agenceNom " +
            "FROM AdminNotification an " +
            "LEFT JOIN Collecteur c ON an.collecteurId = c.id " +
            "LEFT JOIN Agence a ON an.agenceId = a.id " +
            "WHERE an.adminId = :adminId " +
            "ORDER BY an.lu ASC, an.priority ASC, an.dateCreation DESC")
    List<NotificationDashboardProjection> findDashboardProjectionsByAdminId(@Param("adminId") Long adminId, Pageable pageable);

    /**
     * Statistiques optimisées
     */
    @Query("SELECT " +
            "COUNT(an) as total, " +
            "COUNT(CASE WHEN an.lu = false THEN 1 END) as nonLues, " +
            "COUNT(CASE WHEN an.priority = 'CRITIQUE' THEN 1 END) as critiques, " +
            "COUNT(CASE WHEN an.priority = 'CRITIQUE' AND an.lu = false THEN 1 END) as critiquesNonLues, " +
            "MAX(an.dateCreation) as derniere " +
            "FROM AdminNotification an WHERE an.adminId = :adminId")
    NotificationStatsProjection getStatsProjectionByAdminId(@Param("adminId") Long adminId);
}
