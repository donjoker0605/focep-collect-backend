package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Notification;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.NotificationRepository;
import org.example.collectfocep.services.interfaces.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Page<Notification> findByUser(String destinataire, Pageable pageable) {
        log.info("Récupération des notifications pour: {}", destinataire);
        return notificationRepository.findByDestinataireOrderByDateCreationDesc(destinataire, pageable);
    }

    @Override
    public Long getUnreadCount(String destinataire) {
        return notificationRepository.countByDestinataireAndLuFalse(destinataire);
    }

    @Override
    public void markAsRead(Long notificationId) {
        log.info("Marquage de la notification {} comme lue", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée"));

        notification.setLu(true);
        notificationRepository.save(notification);
    }

    @Override
    public Notification createNotification(String destinataire, String titre, String message,
                                           Notification.NotificationType type) {
        log.info("Création d'une notification pour: {}", destinataire);

        Notification notification = Notification.builder()
                .destinataire(destinataire)
                .titre(titre)
                .message(message)
                .type(type)
                .lu(false)
                .dateCreation(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(String destinataire) {
        log.info("Marquage de toutes les notifications comme lues pour: {}", destinataire);
        notificationRepository.markAllAsReadByDestinataire(destinataire);
    }

    @Override
    public void deleteOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteByDateCreationBefore(cutoffDate);
        log.info("Suppression des notifications antérieures à: {}", cutoffDate);
    }
}
