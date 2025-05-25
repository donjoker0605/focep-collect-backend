package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Notification;
import org.example.collectfocep.repositories.NotificationRepository;
import org.example.collectfocep.services.interfaces.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> findByUser(String userEmail, Pageable pageable) {
        log.debug("Récupération des notifications pour l'utilisateur: {}", userEmail);
        return notificationRepository.findByDestinataireOrderByDateCreationDesc(userEmail, pageable);
    }

    @Override
    public void markAsRead(Long notificationId) {
        log.debug("Marquage de la notification {} comme lue", notificationId);
        notificationRepository.findById(notificationId)
                .ifPresent(notification -> {
                    notification.setLu(true);
                    notificationRepository.save(notification);
                });
    }

    @Override
    public void markAllAsRead(String userEmail) {
        log.debug("Marquage de toutes les notifications comme lues pour: {}", userEmail);
        notificationRepository.markAllAsReadByUser(userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(String userEmail) {
        return notificationRepository.countByDestinataireAndLuFalse(userEmail);
    }

    @Override
    public Notification createNotification(String titre, String message,
                                           Notification.NotificationType type,
                                           String destinataire) {
        log.debug("Création d'une notification pour: {}", destinataire);

        Notification notification = Notification.builder()
                .titre(titre)
                .message(message)
                .type(type)
                .destinataire(destinataire)
                .lu(false)
                .dateCreation(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    @Override
    public void cleanupOldNotifications(int daysToKeep) {
        log.info("Nettoyage des notifications anciennes (plus de {} jours)", daysToKeep);
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        notificationRepository.deleteByDateCreationBeforeAndLuTrue(cutoffDate);
    }
}