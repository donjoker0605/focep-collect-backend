package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.NotificationCooldown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationCooldownRepository extends JpaRepository<NotificationCooldown, Long> {

    /**
     * Trouver cooldown par collecteur et type
     */
    Optional<NotificationCooldown> findByCollecteurIdAndNotificationType(Long collecteurId, String notificationType);

    /**
     * Vérifier si en cooldown
     */
    @Query("SELECT COUNT(nc) > 0 FROM NotificationCooldown nc WHERE nc.collecteurId = :collecteurId " +
            "AND nc.notificationType = :type AND nc.lastSentAt > :cooldownThreshold")
    boolean isInCooldown(@Param("collecteurId") Long collecteurId,
                         @Param("type") String notificationType,
                         @Param("cooldownThreshold") LocalDateTime cooldownThreshold);

    /**
     * Nettoyer anciens cooldowns
     */
    @Modifying
    @Query("DELETE FROM NotificationCooldown nc WHERE nc.lastSentAt < :before")
    int cleanupOldCooldowns(@Param("before") LocalDateTime before);

    /**
     * Mettre à jour ou créer cooldown
     */
    @Modifying
    @Query("UPDATE NotificationCooldown nc SET nc.lastSentAt = :lastSentAt WHERE nc.collecteurId = :collecteurId AND nc.notificationType = :type")
    int updateLastSentAt(@Param("collecteurId") Long collecteurId,
                         @Param("type") String notificationType,
                         @Param("lastSentAt") LocalDateTime lastSentAt);
}