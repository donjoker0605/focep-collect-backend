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
     * Trouver les notifications par destinataire, triées par date de création décroissante
     */
    Page<Notification> findByDestinataireOrderByDateCreationDesc(String destinataire, Pageable pageable);

    /**
     * Compter les notifications non lues d'un destinataire
     */
    Long countByDestinataireAndLuFalse(String destinataire);

    /**
     * Marquer toutes les notifications d'un utilisateur comme lues
     */
    @Modifying
    @Query("UPDATE Notification n SET n.lu = true WHERE n.destinataire = :destinataire AND n.lu = false")
    void markAllAsReadByDestinataire(@Param("destinataire") String destinataire);

    /**
     * Supprimer les notifications anciennes
     */
    void deleteByDateCreationBefore(LocalDateTime cutoffDate);

    /**
     * Trouver les notifications non lues par destinataire
     */
    Page<Notification> findByDestinataireAndLuFalseOrderByDateCreationDesc(String destinataire, Pageable pageable);
}
