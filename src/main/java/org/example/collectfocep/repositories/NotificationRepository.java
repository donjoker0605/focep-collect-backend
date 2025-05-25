package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Trouve les notifications pour un utilisateur, triées par date de création décroissante
     */
    Page<Notification> findByDestinataireOrderByDateCreationDesc(String destinataire, Pageable pageable);

    /**
     * Compte les notifications non lues pour un utilisateur
     */
    Long countByDestinataireAndLuFalse(String destinataire);

    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    @Modifying
    @Query("UPDATE Notification n SET n.lu = true WHERE n.destinataire = :destinataire AND n.lu = false")
    void markAllAsReadByUser(@Param("destinataire") String destinataire);

    /**
     * Supprime les notifications anciennes et déjà lues
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.dateCreation < :cutoffDate AND n.lu = true")
    void deleteByDateCreationBeforeAndLuTrue(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Trouve les notifications récentes (non lues ou créées dans les X derniers jours)
     */
    @Query("SELECT n FROM Notification n WHERE n.destinataire = :destinataire " +
            "AND (n.lu = false OR n.dateCreation > :cutoffDate) " +
            "ORDER BY n.dateCreation DESC")
    Page<Notification> findRecentByUser(@Param("destinataire") String destinataire,
                                        @Param("cutoffDate") LocalDateTime cutoffDate,
                                        Pageable pageable);
}