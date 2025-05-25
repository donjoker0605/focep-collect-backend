package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    /**
     * Trouve les notifications pour un utilisateur donné
     */
    Page<Notification> findByUser(String userEmail, Pageable pageable);

    /**
     * Marque une notification comme lue
     */
    void markAsRead(Long notificationId);

    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    void markAllAsRead(String userEmail);

    /**
     * Obtient le nombre de notifications non lues pour un utilisateur
     */
    Long getUnreadCount(String userEmail);

    /**
     * Crée une nouvelle notification
     */
    Notification createNotification(String titre, String message,
                                    Notification.NotificationType type,
                                    String destinataire);

    /**
     * Supprime les anciennes notifications (plus de X jours)
     */
    void cleanupOldNotifications(int daysToKeep);
}