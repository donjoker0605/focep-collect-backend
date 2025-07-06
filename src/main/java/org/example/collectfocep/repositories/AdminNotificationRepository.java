package org.example.collectfocep.repositories;



import org.example.collectfocep.entities.AdminNotification;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    /**
     * MÉTHODES POUR TON SERVICE EXISTANT
     */


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

    // Statistiques pour monitoring
    @Query("SELECT COUNT(n) FROM AdminNotification n WHERE n.dateCreation > :since")
    Long countCreatedSince(@Param("since") LocalDateTime since);

    // Notifications par type dans une période
    @Query("SELECT n.type, COUNT(n) FROM AdminNotification n " +
            "WHERE n.dateCreation > :since GROUP BY n.type")
    List<Object[]> getNotificationStatsByType(@Param("since") LocalDateTime since);

    // =====================================
    // MÉTHODES DE BASE PAR ADMIN
    // =====================================

    /**
     * Toutes les notifications d'un admin
     */
    List<AdminNotification> findByAdminIdOrderByDateCreationDesc(Long adminId);

    /**
     * Notifications avec pagination
     */
    Page<AdminNotification> findByAdminIdOrderByDateCreationDesc(Long adminId, Pageable pageable);

    /**
     * Notifications non lues d'un admin
     */
    @Query("SELECT an FROM AdminNotification an WHERE an.adminId = :adminId AND an.lu = false ORDER BY an.priority ASC, an.dateCreation DESC")
    List<AdminNotification> findUnreadByAdminId(@Param("adminId") Long adminId);

    /**
     * Notifications critiques non lues
     */
    @Query("SELECT an FROM AdminNotification an WHERE an.adminId = :adminId AND an.priority = 'CRITIQUE' AND an.lu = false ORDER BY an.dateCreation DESC")
    List<AdminNotification> findCriticalUnreadByAdminId(@Param("adminId") Long adminId);

    // =====================================
    // MÉTHODES DE COMPTAGE
    // =====================================

    /**
     * Compter notifications non lues
     */
    @Query("SELECT COUNT(an) FROM AdminNotification an WHERE an.adminId = :adminId AND an.lu = false")
    Long countByAdminIdAndLuFalse(@Param("adminId") Long adminId);

    /**
     * Compter notifications critiques non lues
     */
    @Query("SELECT COUNT(an) FROM AdminNotification an WHERE an.adminId = :adminId AND an.priority = 'CRITIQUE' AND an.lu = false")
    Long countCriticalUnreadByAdminId(@Param("adminId") Long adminId);

    /**
     * Compter par type
     */
    Long countByAdminIdAndType(Long adminId, NotificationType type);

    // =====================================
    // MÉTHODES DE RECHERCHE PAR CRITÈRES
    // =====================================

    /**
     * Notifications par collecteur
     */
    List<AdminNotification> findByCollecteurIdOrderByDateCreationDesc(Long collecteurId);

    /**
     * Notifications par type et admin
     */
    List<AdminNotification> findByAdminIdAndTypeOrderByDateCreationDesc(Long adminId, NotificationType type);

    /**
     * Notifications par priorité
     */
    List<AdminNotification> findByAdminIdAndPriorityOrderByDateCreationDesc(Long adminId, Priority priority);

    /**
     * Notifications récentes (dernières 24h)
     */
    @Query("SELECT an FROM AdminNotification an WHERE an.adminId = :adminId AND an.dateCreation >= :since ORDER BY an.dateCreation DESC")
    List<AdminNotification> findRecentByAdminId(@Param("adminId") Long adminId, @Param("since") LocalDateTime since);

    // =====================================
    // MÉTHODES POUR GROUPING
    // =====================================

    /**
     * Trouver notification groupable existante
     */
    @Query("SELECT an FROM AdminNotification an WHERE an.adminId = :adminId AND an.collecteurId = :collecteurId " +
            "AND an.type = :type AND an.lu = false AND an.dateCreation >= :since ORDER BY an.dateCreation DESC")
    Optional<AdminNotification> findGroupableNotification(
            @Param("adminId") Long adminId,
            @Param("collecteurId") Long collecteurId,
            @Param("type") NotificationType type,
            @Param("since") LocalDateTime since);

    // =====================================
    // MÉTHODES DE MISE À JOUR
    // =====================================

    /**
     * Marquer une notification comme lue
     */
    @Modifying
    @Query("UPDATE AdminNotification an SET an.lu = true, an.dateLecture = :dateLecture WHERE an.id = :id AND an.adminId = :adminId")
    int markAsRead(@Param("id") Long id, @Param("adminId") Long adminId, @Param("dateLecture") LocalDateTime dateLecture);

    /**
     * Marquer toutes les notifications comme lues
     */
    @Modifying
    @Query("UPDATE AdminNotification an SET an.lu = true, an.dateLecture = :dateLecture WHERE an.adminId = :adminId AND an.lu = false")
    int markAllAsRead(@Param("adminId") Long adminId, @Param("dateLecture") LocalDateTime dateLecture);

    /**
     * Marquer email comme envoyé
     */
    @Modifying
    @Query("UPDATE AdminNotification an SET an.emailSent = true, an.emailSentAt = :emailSentAt WHERE an.id = :id")
    int markEmailSent(@Param("id") Long id, @Param("emailSentAt") LocalDateTime emailSentAt);

    // =====================================
    // STATISTIQUES ET DASHBOARD
    // =====================================

    /**
     * Statistiques complètes pour dashboard admin
     */
    @Query("SELECT " +
            "COUNT(an) as total, " +
            "COUNT(CASE WHEN an.lu = false THEN 1 END) as nonLues, " +
            "COUNT(CASE WHEN an.priority = 'CRITIQUE' THEN 1 END) as critiques, " +
            "COUNT(CASE WHEN an.priority = 'CRITIQUE' AND an.lu = false THEN 1 END) as critiquesNonLues, " +
            "MAX(an.dateCreation) as derniere " +
            "FROM AdminNotification an WHERE an.adminId = :adminId")
    Object[] getStatsByAdminId(@Param("adminId") Long adminId);

    /**
     * Notifications avec détails collecteur (pour affichage)
     */
    @Query("SELECT an FROM AdminNotification an " +
            "LEFT JOIN FETCH an.collecteur c " +
            "WHERE an.adminId = :adminId " +
            "ORDER BY an.lu ASC, an.priority ASC, an.dateCreation DESC")
    List<AdminNotification> findByAdminIdWithCollecteurDetails(@Param("adminId") Long adminId);

    // =====================================
    // NETTOYAGE ET MAINTENANCE
    // =====================================

    /**
     * Notifications en attente d'email
     */
    @Query("SELECT an FROM AdminNotification an WHERE an.emailSent = false AND an.priority = 'CRITIQUE' ORDER BY an.dateCreation ASC")
    List<AdminNotification> findPendingEmailNotifications();

    @Query("SELECT COUNT(an) FROM AdminNotification an WHERE an.adminId = :adminId")
    Long countByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(an) FROM AdminNotification an WHERE an.adminId = :adminId AND an.priority = :priority")
    Long countByAdminIdAndPriority(@Param("adminId") Long adminId, @Param("priority") Priority priority);

    /**
     * Supprimer anciennes notifications lues
     */
    @Query("DELETE FROM AdminNotification an WHERE an.lu = true AND an.dateLecture < :cutoff")
    @Modifying
    int deleteOldReadNotifications(@Param("cutoff") LocalDateTime cutoff);
}