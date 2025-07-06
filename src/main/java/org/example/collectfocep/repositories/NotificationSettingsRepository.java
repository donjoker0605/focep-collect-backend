package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    /**
     * Settings par admin et type
     */
    Optional<NotificationSettings> findByAdminIdAndType(Long adminId, String type);

    /**
     * Tous les settings d'un admin
     */
    List<NotificationSettings> findByAdminIdOrderByType(Long adminId);

    /**
     * Settings activés pour un admin
     */
    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.adminId = :adminId AND ns.enabled = true")
    List<NotificationSettings> findEnabledByAdminId(@Param("adminId") Long adminId);

    /**
     * Vérifier si notifications activées pour un type
     */
    @Query("SELECT COUNT(ns) > 0 FROM NotificationSettings ns WHERE ns.adminId = :adminId AND ns.type = :type AND ns.enabled = true")
    boolean isNotificationEnabled(@Param("adminId") Long adminId, @Param("type") String type);

    /**
     * Vérifier si email activé pour un type
     */
    @Query("SELECT COUNT(ns) > 0 FROM NotificationSettings ns WHERE ns.adminId = :adminId AND ns.type = :type AND ns.enabled = true AND ns.emailEnabled = true")
    boolean isEmailEnabled(@Param("adminId") Long adminId, @Param("type") String type);
}

