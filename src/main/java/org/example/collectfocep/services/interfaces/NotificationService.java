package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    /**
     * Trouver les notifications d'un utilisateur
     */
    Page<Notification> findByUser(String destinataire, Pageable pageable);

    /**
     * Compter les notifications non lues
     */
    Long getUnreadCount(String destinataire);

    /**
     * Marquer une notification comme lue
     */
    void markAsRead(Long notificationId);

    /**
     * Créer une nouvelle notification
     */
    Notification createNotification(String destinataire, String titre, String message,
                                    Notification.NotificationType type);

    /**
     * Marquer toutes les notifications comme lues
     */
    void markAllAsRead(String destinataire);

    /**
     * Supprimer les anciennes notifications
     */
    void deleteOldNotifications(int daysOld);
}
