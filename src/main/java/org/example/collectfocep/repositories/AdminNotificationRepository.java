package org.example.collectfocep.repositories;



import org.example.collectfocep.entities.AdminNotification;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    /**
     * ✅ MÉTHODES POUR TON SERVICE EXISTANT
     */

    // Compter notifications non lues pour un admin
    @Query("SELECT COUNT(n) FROM AdminNotification n WHERE n.adminId = :adminId AND n.lu = false")
    Long countByAdminIdAndLuFalse(@Param("adminId") Long adminId);

    // Compter notifications urgentes non lues
    @Query("SELECT COUNT(n) FROM AdminNotification n WHERE n.adminId = :adminId " +
            "AND n.priority = :priority AND n.lu = false")
    Long countByAdminIdAndPriorityAndLuFalse(@Param("adminId") Long adminId,
                                             @Param("priority") Priority priority);

    // Récupérer notifications non lues triées par priorité et date
    @Query("SELECT n FROM AdminNotification n WHERE n.adminId = :adminId " +
            "AND n.lu = false ORDER BY n.priority DESC, n.dateCreation DESC")
    List<AdminNotification> findUnreadByAdminIdOrderByPriorityAndDate(@Param("adminId") Long adminId);

    // Trouver notification groupable récente
    @Query("SELECT n FROM AdminNotification n WHERE n.adminId = :adminId " +
            "AND n.collecteurId = :collecteurId AND n.type = :type " +
            "AND n.dateCreation > :cutoff ORDER BY n.dateCreation DESC")
    Optional<AdminNotification> findRecentGroupable(@Param("adminId") Long adminId,
                                                    @Param("collecteurId") Long collecteurId,
                                                    @Param("type") NotificationType type,
                                                    @Param("cutoff") LocalDateTime cutoff);

    // Toutes les notifications d'un admin (avec pagination si besoin)
    @Query("SELECT n FROM AdminNotification n WHERE n.adminId = :adminId " +
            "ORDER BY n.dateCreation DESC")
    List<AdminNotification> findByAdminIdOrderByDateCreationDesc(@Param("adminId") Long adminId);

    // Notifications par agence (pour super admin)
    @Query("SELECT n FROM AdminNotification n WHERE n.agenceId = :agenceId " +
            "ORDER BY n.dateCreation DESC")
    List<AdminNotification> findByAgenceIdOrderByDateCreationDesc(@Param("agenceId") Long agenceId);

    // Notifications dans une période
    @Query("SELECT n FROM AdminNotification n WHERE n.adminId = :adminId " +
            "AND n.dateCreation BETWEEN :startDate AND :endDate " +
            "ORDER BY n.dateCreation DESC")
    List<AdminNotification> findByAdminIdAndDateRange(@Param("adminId") Long adminId,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    // Nettoyer anciennes notifications
    @Query("DELETE FROM AdminNotification n WHERE n.lu = true " +
            "AND n.dateCreation < :cutoffDate")
    void deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Statistiques pour monitoring
    @Query("SELECT COUNT(n) FROM AdminNotification n WHERE n.dateCreation > :since")
    Long countCreatedSince(@Param("since") LocalDateTime since);

    // Notifications par type dans une période
    @Query("SELECT n.type, COUNT(n) FROM AdminNotification n " +
            "WHERE n.dateCreation > :since GROUP BY n.type")
    List<Object[]> getNotificationStatsByType(@Param("since") LocalDateTime since);
}