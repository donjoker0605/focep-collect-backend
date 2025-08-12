package org.example.collectfocep.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.interfaces.EmailService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationRepository notificationRepository;
    private final NotificationCooldownRepository cooldownRepository;
    private final NotificationSettingsRepository settingsRepository;
    private final EmailService emailService;
    private final AdminRepository adminRepository;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final MouvementRepository mouvementRepository;
    private final JournalActiviteRepository journalActiviteRepository;
    private final ObjectMapper objectMapper;

    // CONFIGURATION SEUILS
    private static final double SEUIL_TRANSACTION_CRITIQUE_DEFAULT = 100_000.0;
    private static final int SEUIL_INACTIVITE_HEURES = 24;
    private static final int DEFAULT_COOLDOWN_MINUTES = 30;

    // ===================================================
    // MÉTHODES PRINCIPALES DASHBOARD
    // ===================================================

    /**
     * 📊 Dashboard principal des notifications
     */
    public NotificationDashboardDTO getDashboard(Long adminId) {
        try {
            log.debug("📊 Génération dashboard notifications: adminId={}", adminId);

            // Statistiques
            NotificationStatsDTO stats = getStats(adminId);

            // Notifications récentes (dernières 10)
            List<AdminNotificationDTO> recentNotifications = getAllNotificationsInternal(
                    adminId, null, null, false,
                    org.springframework.data.domain.PageRequest.of(0, 10)
            ).getContent();

            // Notifications critiques non lues
            List<AdminNotificationDTO> criticalNotifications = getCriticalNotificationsDTO(adminId);

            return NotificationDashboardDTO.builder()
                    .stats(stats)
                    .recentNotifications(recentNotifications)
                    .criticalNotifications(criticalNotifications)
                    .lastUpdate(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur génération dashboard: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la génération du dashboard", e);
        }
    }

    /**
     * 📋 Récupérer toutes les notifications avec filtres et pagination
     */
    public Page<AdminNotificationDTO> getAllNotifications(Long adminId, NotificationType type,
                                                          Priority priority, boolean unreadOnly,
                                                          Pageable pageable) {
        return getAllNotificationsInternal(adminId, type, priority, unreadOnly, pageable);
    }

    private Page<AdminNotificationDTO> getAllNotificationsInternal(Long adminId, NotificationType type,
                                                                   Priority priority, boolean unreadOnly,
                                                                   Pageable pageable) {
        try {
            log.debug("📋 Récupération notifications: adminId={}, type={}, priority={}, unreadOnly={}",
                    adminId, type, priority, unreadOnly);

            List<AdminNotification> allNotifications;

            if (unreadOnly) {
                allNotifications = notificationRepository.findUnreadByAdminId(adminId);
            } else {
                allNotifications = notificationRepository.findByAdminIdOrderByDateCreationDesc(adminId);
            }

            // Filtrer par type si spécifié
            if (type != null) {
                allNotifications = allNotifications.stream()
                        .filter(n -> n.getType() == type)
                        .collect(Collectors.toList());
            }

            // Filtrer par priorité si spécifiée
            if (priority != null) {
                allNotifications = allNotifications.stream()
                        .filter(n -> n.getPriority() == priority)
                        .collect(Collectors.toList());
            }

            // Convertir en DTO
            List<AdminNotificationDTO> dtos = allNotifications.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // Pagination manuelle
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), dtos.size());

            if (start >= dtos.size()) {
                return new PageImpl<>(Collections.emptyList(), pageable, dtos.size());
            }

            List<AdminNotificationDTO> pageContent = dtos.subList(start, end);
            return new PageImpl<>(pageContent, pageable, dtos.size());

        } catch (Exception e) {
            log.error("❌ Erreur récupération notifications: {}", e.getMessage(), e);
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }

    /**
     * 🔢 Récupérer statistiques des notifications
     */
    public NotificationStatsDTO getStats(Long adminId) {
        try {
            log.debug("🔢 Calcul statistiques: adminId={}", adminId);

            Long total = notificationRepository.countByAdminId(adminId);
            Long nonLues = notificationRepository.countByAdminIdAndLuFalse(adminId);
            Long critiques = notificationRepository.countByAdminIdAndPriority(adminId, Priority.CRITIQUE);
            Long critiquesNonLues = notificationRepository.countCriticalUnreadByAdminId(adminId, Priority.CRITIQUE);

            // Dernière notification
            LocalDateTime derniere = notificationRepository.findByAdminIdOrderByDateCreationDesc(adminId)
                    .stream()
                    .findFirst()
                    .map(AdminNotification::getDateCreation)
                    .orElse(null);

            return NotificationStatsDTO.builder()
                    .total(total)
                    .nonLues(nonLues)
                    .critiques(critiques)
                    .critiquesNonLues(critiquesNonLues)
                    .derniereNotification(derniere)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur calcul statistiques: {}", e.getMessage(), e);
            return NotificationStatsDTO.builder()
                    .total(0L).nonLues(0L).critiques(0L).critiquesNonLues(0L)
                    .build();
        }
    }

    /**
     * 🚨 Récupérer notifications critiques
     */
    public List<AdminNotification> getCriticalNotifications(Long adminId) {
        try {
            return notificationRepository.findCriticalUnreadByAdminId(adminId, Priority.CRITIQUE);
        } catch (Exception e) {
            log.error("❌ Erreur récupération notifications critiques: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<AdminNotificationDTO> getCriticalNotificationsDTO(Long adminId) {
        return getCriticalNotifications(adminId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===================================================
    // ACTIONS SUR NOTIFICATIONS
    // ===================================================

    /**
     * ✅ Marquer notification comme lue
     */
    public boolean markAsRead(Long notificationId, Long adminId) {
        try {
            log.info("✅ Marquer notification comme lue: notificationId={}, adminId={}", notificationId, adminId);

            Optional<AdminNotification> optionalNotification = notificationRepository.findById(notificationId);
            if (optionalNotification.isEmpty()) {
                log.warn("⚠️ Notification non trouvée: {}", notificationId);
                return false;
            }

            AdminNotification notification = optionalNotification.get();

            // Vérifier autorisation
            if (!notification.getAdminId().equals(adminId)) {
                log.warn("⚠️ Accès non autorisé: notification={}, admin={}", notificationId, adminId);
                return false;
            }

            if (!notification.isRead()) {
                notification.markAsRead();
                notificationRepository.save(notification);
                log.info("✅ Notification marquée comme lue avec succès");
                return true;
            }

            return true; // Déjà lue

        } catch (Exception e) {
            log.error("❌ Erreur marquage lecture: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ Marquer toutes les notifications comme lues
     */
    public int markAllAsRead(Long adminId) {
        try {
            log.info("✅ Marquer toutes notifications comme lues: adminId={}", adminId);

            LocalDateTime now = LocalDateTime.now();
            int updatedCount = notificationRepository.markAllAsRead(adminId, now);

            log.info("✅ {} notifications marquées comme lues", updatedCount);
            return updatedCount;

        } catch (Exception e) {
            log.error("❌ Erreur marquage global: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 🗑️ Supprimer une notification
     */
    public boolean deleteNotification(Long notificationId, Long adminId) {
        try {
            log.info("🗑️ Supprimer notification: notificationId={}, adminId={}", notificationId, adminId);

            Optional<AdminNotification> optionalNotification = notificationRepository.findById(notificationId);
            if (optionalNotification.isEmpty()) {
                log.warn("⚠️ Notification non trouvée: {}", notificationId);
                return false;
            }

            AdminNotification notification = optionalNotification.get();

            // Vérifier autorisation
            if (!notification.getAdminId().equals(adminId)) {
                log.warn("⚠️ Accès non autorisé: notification={}, admin={}", notificationId, adminId);
                return false;
            }

            notificationRepository.delete(notification);
            log.info("✅ Notification supprimée avec succès");
            return true;

        } catch (Exception e) {
            log.error("❌ Erreur suppression notification: {}", e.getMessage(), e);
            return false;
        }
    }

    // ===================================================
    // COMPTEURS
    // ===================================================

    /**
     * 🔢 Compter notifications non lues
     */
    public Long getUnreadCount(Long adminId) {
        try {
            return notificationRepository.countByAdminIdAndLuFalse(adminId);
        } catch (Exception e) {
            log.error("❌ Erreur comptage non lues: {}", e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 🔢 Compter notifications critiques non lues
     */
    public Long getCriticalCount(Long adminId) {
        try {
            return notificationRepository.countCriticalUnreadByAdminId(adminId, Priority.CRITIQUE);
        } catch (Exception e) {
            log.error("❌ Erreur comptage critiques: {}", e.getMessage(), e);
            return 0L;
        }
    }

    // ===================================================
    // FILTRES ET RECHERCHE
    // ===================================================

    /**
     * 🔍 Notifications par collecteur
     */
    public List<AdminNotificationDTO> getNotificationsByCollecteur(Long adminId, Long collecteurId) {
        try {
            log.debug("🔍 Notifications par collecteur: adminId={}, collecteurId={}", adminId, collecteurId);

            List<AdminNotification> notifications = notificationRepository.findByCollecteurIdOrderByDateCreationDesc(collecteurId);

            // Filtrer par admin
            return notifications.stream()
                    .filter(n -> n.getAdminId().equals(adminId))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Erreur notifications collecteur: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 🔍 Notifications par période
     */
    public List<AdminNotificationDTO> getNotificationsByPeriod(Long adminId, LocalDateTime debut, LocalDateTime fin) {
        try {
            log.debug("🔍 Notifications par période: adminId={}, debut={}, fin={}", adminId, debut, fin);

            List<AdminNotification> notifications = notificationRepository.findRecentByAdminId(adminId, debut);

            // Filtrer par période de fin
            return notifications.stream()
                    .filter(n -> n.getDateCreation().isBefore(fin))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Erreur notifications période: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ===================================================
    // CONFIGURATION
    // ===================================================

    /**
     * ⚙️ Récupérer configuration des notifications
     */
    public List<NotificationSettingsDTO> getSettings(Long adminId) {
        try {
            log.debug("⚙️ Récupération configuration: adminId={}", adminId);

            List<NotificationSettings> settings = settingsRepository.findByAdminIdOrderByType(adminId);

            return settings.stream()
                    .map(this::convertSettingsToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Erreur récupération config: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * ⚙️ Mettre à jour configuration
     */
    public void updateSettings(Long adminId, List<NotificationSettingsDTO> settingsDTOs) {
        try {
            log.info("⚙️ Mise à jour configuration: adminId={}", adminId);

            for (NotificationSettingsDTO dto : settingsDTOs) {
                Optional<NotificationSettings> existing = settingsRepository.findByAdminIdAndType(adminId, dto.getType());

                if (existing.isPresent()) {
                    NotificationSettings settings = existing.get();
                    settings.setEnabled(dto.getEnabled());
                    settings.setEmailEnabled(dto.getEmailEnabled());
                    settings.setThresholdValue(dto.getThresholdValue());
                    settings.setCooldownMinutes(dto.getCooldownMinutes());
                    settings.preUpdate();
                    settingsRepository.save(settings);
                } else {
                    NotificationSettings newSettings = NotificationSettings.builder()
                            .adminId(adminId)
                            .type(dto.getType())
                            .enabled(dto.getEnabled())
                            .emailEnabled(dto.getEmailEnabled())
                            .thresholdValue(dto.getThresholdValue())
                            .cooldownMinutes(dto.getCooldownMinutes())
                            .build();
                    settingsRepository.save(newSettings);
                }
            }

            log.info("✅ Configuration mise à jour avec succès");

        } catch (Exception e) {
            log.error("❌ Erreur mise à jour config: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la mise à jour de la configuration", e);
        }
    }

    // ===================================================
    // ACTIONS AVANCÉES
    // ===================================================

    /**
     * 📧 Renvoyer email pour notification critique
     */
    public boolean resendEmail(Long notificationId, Long adminId) {
        try {
            log.info("📧 Renvoi email notification: notificationId={}, adminId={}", notificationId, adminId);

            Optional<AdminNotification> optionalNotification = notificationRepository.findById(notificationId);
            if (optionalNotification.isEmpty()) {
                return false;
            }

            AdminNotification notification = optionalNotification.get();

            // Vérifier autorisation
            if (!notification.getAdminId().equals(adminId)) {
                return false;
            }

            // Renvoyer email
            sendCriticalNotification(adminId, notification);
            return true;

        } catch (Exception e) {
            log.error("❌ Erreur renvoi email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 🧹 Nettoyer anciennes notifications
     */
    public int cleanupOldNotifications(Long adminId, int daysOld) {
        try {
            log.info("🧹 Nettoyage notifications: adminId={}, daysOld={}", adminId, daysOld);

            LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);

            // Compter d'abord
            List<AdminNotification> toDelete = notificationRepository.findByAdminIdOrderByDateCreationDesc(adminId)
                    .stream()
                    .filter(n -> n.isRead() && n.getDateLecture() != null && n.getDateLecture().isBefore(cutoff))
                    .collect(Collectors.toList());

            // Supprimer
            notificationRepository.deleteAll(toDelete);

            log.info("✅ {} notifications supprimées", toDelete.size());
            return toDelete.size();

        } catch (Exception e) {
            log.error("❌ Erreur nettoyage: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 🧪 Créer notification de test
     */
    public void createTestNotification(Long adminId, CreateTestNotificationRequest request) {
        try {
            log.info("🧪 Création notification test: adminId={}, type={}", adminId, request.getType());

            AdminNotification testNotification = AdminNotification.builder()
                    .adminId(adminId)
                    .collecteurId(request.getCollecteurId())
                    .type(request.getType())
                    .priority(request.getPriority())
                    .title(request.getTitle() != null ? request.getTitle() : "Notification de test")
                    .message(request.getMessage() != null ? request.getMessage() : "Ceci est une notification de test")
                    .dateCreation(LocalDateTime.now())
                    .lu(false)
                    .groupedCount(1)
                    .emailSent(false)
                    .build();

            notificationRepository.save(testNotification);

            // Envoyer email si critique
            if (testNotification.isCritical()) {
                CompletableFuture.runAsync(() -> sendCriticalNotification(adminId, testNotification));
            }

            log.info("✅ Notification de test créée avec succès");

        } catch (Exception e) {
            log.error("❌ Erreur création notification test: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la création de la notification test", e);
        }
    }

    // ===================================================
    // MÉTHODES EXISTANTES CORRIGÉES
    // ===================================================

    /**
     * MÉTHODE PRINCIPALE: Évaluer et créer notification si critique
     */
    public void evaluateAndNotify(ActivityEvent event) {
        try {
            log.debug("🔍 Évaluation événement: type={}, collecteur={}", event.getType(), event.getCollecteurId());

            // 1. Déterminer si l'événement mérite une notification critique
            NotificationCritique notification = evaluateCriticalEvent(event);

            if (notification == null) {
                log.debug("📝 Événement non critique ignoré: {}", event.getType());
                return;
            }

            // Circuit breaker pour éviter spam
            if (isInCooldown(event.getCollecteurId(), notification.getType())) {
                log.debug("🕒 Notification en cooldown ignorée: collecteur={}, type={}",
                        event.getCollecteurId(), notification.getType());
                return;
            }

            // 2. Identifier l'admin responsable
            Long adminId = findResponsibleAdmin(event.getCollecteurId());
            if (adminId == null) {
                log.warn("⚠️ Admin non trouvé pour collecteur: {}", event.getCollecteurId());
                return;
            }

            // Vérifier si grouper avec notification existante
            Optional<AdminNotification> existingNotif = notificationRepository.findGroupableNotification(
                    adminId, event.getCollecteurId(), notification.getType(), LocalDateTime.now().minusHours(1));

            if (existingNotif.isPresent()) {
                groupNotification(existingNotif.get(), notification);
                return;
            }

            // 3. Créer la notification
            AdminNotification notif = AdminNotification.builder()
                    .adminId(adminId)
                    .collecteurId(event.getCollecteurId())
                    .type(notification.getType())
                    .priority(notification.getPriority())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .entityId(event.getEntityId())
                    .data(notification.getData())
                    .dateCreation(LocalDateTime.now())
                    .agenceId(event.getAgenceId())
                    .groupedCount(1)
                    .build();

            notificationRepository.save(notif);

            // 4. Marquer dans circuit breaker
            markNotificationSent(event.getCollecteurId(), notification.getType());

            // 5. Envoi immédiat si critique (asynchrone pour performance)
            if (notification.getPriority() == Priority.CRITIQUE) {
                CompletableFuture.runAsync(() -> sendCriticalNotification(adminId, notif));
            }

            log.info("🔔 Notification {} créée pour admin {}: {}",
                    notification.getType(), adminId, notification.getTitle());

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'évaluation de notification: {}", e.getMessage(), e);
        }
    }

    // ===================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ===================================================

    /**
     * Convertir AdminNotification en DTO
     */
    private AdminNotificationDTO convertToDTO(AdminNotification notification) {
        String collecteurNom = null;
        String agenceNom = null;

        if (notification.getCollecteurId() != null) {
            collecteurNom = getCollecteurNom(notification.getCollecteurId());
        }

        if (notification.getAgenceId() != null) {
            agenceNom = getAgenceNom(notification.getAgenceId());
        }

        return AdminNotificationDTO.builder()
                .id(notification.getId())
                .adminId(notification.getAdminId())
                .collecteurId(notification.getCollecteurId())
                .collecteurNom(collecteurNom)
                .agenceId(notification.getAgenceId())
                .agenceNom(agenceNom)
                .type(notification.getType())
                .priority(notification.getPriority())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .dateCreation(notification.getDateCreation())
                .dateLecture(notification.getDateLecture())
                .lu(notification.getLu())
                .groupedCount(notification.getGroupedCount())
                .emailSent(notification.getEmailSent())
                .minutesSinceCreation(notification.getMinutesSinceCreation())
                .build();
    }

    /**
     * Convertir NotificationSettings en DTO
     */
    private NotificationSettingsDTO convertSettingsToDTO(NotificationSettings settings) {
        return NotificationSettingsDTO.builder()
                .type(settings.getType())
                .enabled(settings.getEnabled())
                .emailEnabled(settings.getEmailEnabled())
                .thresholdValue(settings.getThresholdValue())
                .cooldownMinutes(settings.getCooldownMinutes())
                .build();
    }

    /**
     * Circuit breaker pour éviter spam notifications
     */
    private boolean isInCooldown(Long collecteurId, NotificationType type) {
        return cooldownRepository.isInCooldown(
                collecteurId,
                type.name(),
                LocalDateTime.now().minusMinutes(DEFAULT_COOLDOWN_MINUTES)
        );
    }

    private void markNotificationSent(Long collecteurId, NotificationType type) {
        Optional<NotificationCooldown> existing = cooldownRepository.findByCollecteurIdAndNotificationType(
                collecteurId, type.name());

        if (existing.isPresent()) {
            cooldownRepository.updateLastSentAt(collecteurId, type.name(), LocalDateTime.now());
        } else {
            NotificationCooldown cooldown = NotificationCooldown.create(collecteurId, type.name());
            cooldownRepository.save(cooldown);
        }
    }

    /**
     * Grouper notifications similaires
     */
    private void groupNotification(AdminNotification existing, NotificationCritique newEvent) {
        existing.incrementGroupedCount();
        existing.setMessage(existing.getMessage() +
                String.format(" (+%d occurrences)", existing.getGroupedCount() - 1));
        notificationRepository.save(existing);

        log.info("📊 Notification groupée: {} (total: {})",
                existing.getType(), existing.getGroupedCount());
    }

    /**
     * Envoyer email critique
     */
    private void sendCriticalNotification(Long adminId, AdminNotification notification) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            if (adminOpt.isEmpty()) {
                log.warn("⚠️ Admin non trouvé: {}", adminId);
                return;
            }

            Admin admin = adminOpt.get();
            if (admin.getAdresseMail() == null || admin.getAdresseMail().trim().isEmpty()) {
                log.warn("⚠️ Admin sans email: {}", adminId);
                return;
            }

            EmailNotification email = EmailNotification.urgent(
                    admin.getAdresseMail(),
                    notification.getTitle(),
                    buildEmailContent(notification)
            );

            boolean sent = emailService.send(email);
            if (sent) {
                notification.markEmailSent();
                notificationRepository.save(notification);
                log.info("📧 Email critique envoyé à: {}", admin.getAdresseMail());
            }

        } catch (Exception e) {
            log.error("❌ Erreur envoi email critique: {}", e.getMessage(), e);
        }
    }

    /**
     * Méthodes utilitaires pour noms
     */
    private String getCollecteurNom(Long collecteurId) {
        return collecteurRepository.findById(collecteurId)
                .map(c -> c.getPrenom() + " " + c.getNom())
                .orElse("Collecteur inconnu");
    }

    private String getAgenceNom(Long agenceId) {
        return "Agence " + agenceId;
    }

    /**
     * Trouver l'admin responsable d'un collecteur
     */
    private Long findResponsibleAdmin(Long collecteurId) {
        return collecteurRepository.findById(collecteurId)
                .map(collecteur -> {
                    Long agenceId = collecteur.getAgence().getId();
                    List<Admin> admins = adminRepository.findByAgenceId(agenceId);
                    if (admins != null && !admins.isEmpty()) {
                        return admins.get(0).getId();
                    }
                    return null;
                })
                .orElse(null);
    }


    /**
     * Évaluation des événements critiques
     */
    private NotificationCritique evaluateCriticalEvent(ActivityEvent event) {
        switch (event.getType()) {
            case "TRANSACTION_EPARGNE":
            case "TRANSACTION_RETRAIT":
                return evaluateTransactionEvent(event);
            case "CREATE_CLIENT":
                return evaluateClientCreationEvent(event);
            case "SOLDE_COLLECTEUR_CHECK":
                return evaluateSoldeEvent(event);
            default:
                return null;
        }
    }

    private NotificationCritique evaluateTransactionEvent(ActivityEvent event) {
        Double montant = event.getMontant();
        if (montant == null || montant < SEUIL_TRANSACTION_CRITIQUE_DEFAULT) return null;

        String collecteurNom = getCollecteurNom(event.getCollecteurId());

        return NotificationCritique.builder()
                .type(NotificationType.MONTANT_ELEVE)
                .priority(Priority.CRITIQUE)
                .title("Transaction importante détectée")
                .message(String.format("Transaction de %,.0f FCFA par %s", montant, collecteurNom))
                .data(buildTransactionData(event))
                .build();
    }

    private NotificationCritique evaluateClientCreationEvent(ActivityEvent event) {
        // Logique d'évaluation pour création client
        return null; // À implémenter selon vos besoins
    }

    private NotificationCritique evaluateSoldeEvent(ActivityEvent event) {
        Double solde = event.getMontant(); // Utiliser montant comme solde
        if (solde == null || solde >= 0) return null;

        String collecteurNom = getCollecteurNom(event.getCollecteurId());

        return NotificationCritique.builder()
                .type(NotificationType.SOLDE_NEGATIF)
                .priority(Priority.CRITIQUE)
                .title("Solde collecteur négatif")
                .message(String.format("Solde de %s: %,.0f FCFA (négatif)", collecteurNom, solde))
                .data(buildSoldeData(event))
                .build();
    }

    /**
     * Construction des données JSON
     */
    private String buildTransactionData(ActivityEvent event) {
        try {
            Map<String, Object> data = Map.of(
                    "montant", event.getMontant(),
                    "collecteurId", event.getCollecteurId(),
                    "entityId", event.getEntityId() != null ? event.getEntityId() : 0L,
                    "timestamp", event.getTimestamp()
            );
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildSoldeData(ActivityEvent event) {
        try {
            Map<String, Object> data = Map.of(
                    "solde", event.getMontant(),
                    "collecteurId", event.getCollecteurId(),
                    "timestamp", event.getTimestamp()
            );
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildEmailContent(AdminNotification notification) {
        StringBuilder content = new StringBuilder();
        content.append("<h2>🚨 Notification Critique - FOCEP Collecte</h2>");
        content.append("<p><strong>Type:</strong> ").append(notification.getType()).append("</p>");
        content.append("<p><strong>Priorité:</strong> ").append(notification.getPriority()).append("</p>");
        content.append("<p><strong>Message:</strong> ").append(notification.getMessage()).append("</p>");
        content.append("<p><strong>Date:</strong> ").append(notification.getDateCreation()).append("</p>");
        content.append("<hr>");
        content.append("<p>Veuillez vous connecter à votre dashboard administrateur pour plus de détails.</p>");
        content.append("<p><strong>FOCEP Collecte</strong> - Système de notifications automatiques</p>");
        return content.toString();
    }

    // ===================================================
    // MÉTHODES HÉRITÉES (compatibilité)
    // ===================================================

    public List<AdminNotification> getAllNotifications(Long adminId) {
        return notificationRepository.findByAdminIdOrderByDateCreationDesc(adminId);
    }

    // Nettoyage automatique
    /**
     * 🔍 Surveillance proactive des situations critiques (toutes les 30 minutes)
     */
    @Scheduled(cron = "0 */30 * * * ?") // Toutes les 30 minutes
    @Transactional
    public void monitorCriticalSituations() {
        try {
            log.debug("🔍 Début surveillance situations critiques");
            
            // 1. Surveiller les collecteurs inactifs
            monitorInactiveCollecteurs();
            
            // 2. Surveiller les soldes anormaux
            monitorAbnormalBalances();
            
            // 3. Surveiller les transactions suspectes
            monitorSuspiciousTransactions();
            
            // 4. Surveiller les erreurs système
            monitorSystemErrors();
            
            log.debug("✅ Surveillance situations critiques terminée");
            
        } catch (Exception e) {
            log.error("❌ Erreur surveillance critique: {}", e.getMessage(), e);
        }
    }

    /**
     * 📊 Génération hebdomadaire de rapports de notification (tous les lundis à 6h)
     */
    @Scheduled(cron = "0 0 6 * * MON") // Tous les lundis à 6h du matin
    @Transactional(readOnly = true)
    public void generateWeeklyNotificationReport() {
        try {
            log.info("📊 Génération rapport hebdomadaire notifications");
            
            LocalDateTime weekStart = LocalDateTime.now().minusWeeks(1).withHour(0).withMinute(0);
            LocalDateTime weekEnd = LocalDateTime.now().withHour(0).withMinute(0);
            
            List<Admin> admins = adminRepository.findAll();
            
            for (Admin admin : admins) {
                generateAdminWeeklyReport(admin.getId(), weekStart, weekEnd);
            }
            
            log.info("✅ Rapports hebdomadaires générés pour {} admins", admins.size());
            
        } catch (Exception e) {
            log.error("❌ Erreur génération rapport hebdomadaire: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int deleted = notificationRepository.deleteOldReadNotifications(cutoff);
        log.info("🧹 Nettoyage automatique: {} notifications supprimées", deleted);
    }

    // ===================================================
    // MÉTHODES DE SURVEILLANCE PROACTIVE
    // ===================================================

    /**
     * Surveille les collecteurs inactifs depuis plus de 24h
     */
    private void monitorInactiveCollecteurs() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(SEUIL_INACTIVITE_HEURES);
            
            List<Collecteur> inactiveCollecteurs = collecteurRepository.findInactiveSince(threshold);
            
            for (Collecteur collecteur : inactiveCollecteurs) {
                // Vérifier si on n'a pas déjà envoyé cette notification récemment
                if (isInCooldown(collecteur.getId(), NotificationType.COLLECTEUR_INACTIF)) {
                    continue;
                }
                
                Long adminId = findResponsibleAdmin(collecteur.getId());
                if (adminId != null) {
                    createCriticalNotification(adminId, collecteur.getId(), 
                        NotificationType.COLLECTEUR_INACTIF,
                        "Collecteur inactif détecté",
                        String.format("Le collecteur %s %s n'a pas eu d'activité depuis %d heures", 
                            collecteur.getNom(), collecteur.getPrenom(), SEUIL_INACTIVITE_HEURES));
                    
                    markNotificationSent(collecteur.getId(), NotificationType.COLLECTEUR_INACTIF);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur surveillance collecteurs inactifs: {}", e.getMessage());
        }
    }

    /**
     * Surveille les soldes anormaux (négatifs ou très élevés)
     */
    private void monitorAbnormalBalances() {
        try {
            // Détecter les soldes négatifs
            List<Object[]> negativeBalances = mouvementRepository.findClientsWithNegativeBalances();
            
            for (Object[] result : negativeBalances) {
                Long clientId = (Long) result[0];
                Double balance = (Double) result[1];
                Long collecteurId = (Long) result[2];
                
                if (isInCooldown(collecteurId, NotificationType.SOLDE_NEGATIF)) {
                    continue;
                }
                
                Long adminId = findResponsibleAdmin(collecteurId);
                if (adminId != null) {
                    createCriticalNotification(adminId, collecteurId,
                        NotificationType.SOLDE_NEGATIF,
                        "Solde client négatif détecté",
                        String.format("Le client ID %d a un solde négatif de %.2f FCFA", clientId, balance));
                    
                    markNotificationSent(collecteurId, NotificationType.SOLDE_NEGATIF);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur surveillance soldes anormaux: {}", e.getMessage());
        }
    }

    /**
     * Surveille les transactions suspectes (montants très élevés)
     */
    private void monitorSuspiciousTransactions() {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(1);
            double threshold = SEUIL_TRANSACTION_CRITIQUE_DEFAULT;
            
            List<Mouvement> largeTransactions = mouvementRepository.findLargeTransactionsSince(since, threshold);
            
            for (Mouvement transaction : largeTransactions) {
                if (transaction.getCollecteurId() != null) {
                    if (isInCooldown(transaction.getCollecteurId(), NotificationType.TRANSACTION_CRITIQUE)) {
                        continue;
                    }
                    
                    Long adminId = findResponsibleAdmin(transaction.getCollecteurId());
                    if (adminId != null) {
                        createCriticalNotification(adminId, transaction.getCollecteurId(),
                            NotificationType.TRANSACTION_CRITIQUE,
                            "Transaction importante détectée",
                            String.format("Transaction de %.2f FCFA effectuée par le collecteur ID %d", 
                                transaction.getMontant(), transaction.getCollecteurId()));
                        
                        markNotificationSent(transaction.getCollecteurId(), NotificationType.TRANSACTION_CRITIQUE);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur surveillance transactions suspectes: {}", e.getMessage());
        }
    }

    /**
     * Surveille les erreurs système
     */
    private void monitorSystemErrors() {
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(30);
            
            // Cette méthode peut être étendue pour monitorer différents types d'erreurs système
            // Pour l'instant, on log juste qu'elle est appelée
            log.debug("🔍 Surveillance erreurs système depuis: {}", since);
            
        } catch (Exception e) {
            log.error("❌ Erreur surveillance erreurs système: {}", e.getMessage());
        }
    }

    /**
     * Génère un rapport hebdomadaire pour un admin
     */
    private void generateAdminWeeklyReport(Long adminId, LocalDateTime weekStart, LocalDateTime weekEnd) {
        try {
            List<AdminNotification> weekNotifications = notificationRepository
                .findByAdminIdAndDateCreationBetween(adminId, weekStart, weekEnd);
            
            if (weekNotifications.isEmpty()) {
                return;
            }
            
            long criticalCount = weekNotifications.stream()
                .mapToLong(n -> n.getPriority() == Priority.CRITIQUE ? 1 : 0)
                .sum();
            
            long unreadCount = weekNotifications.stream()
                .mapToLong(n -> n.isRead() ? 0 : 1)
                .sum();
            
            String reportMessage = String.format(
                "Rapport hebdomadaire: %d notifications reçues, dont %d critiques. %d notifications non lues.",
                weekNotifications.size(), criticalCount, unreadCount);
            
            AdminNotification weeklyReport = AdminNotification.builder()
                .adminId(adminId)
                .type(NotificationType.RAPPORT_HEBDOMADAIRE)
                .priority(Priority.INFORMATIF)
                .title("Rapport hebdomadaire des notifications")
                .message(reportMessage)
                .dateCreation(LocalDateTime.now())
                .lu(false)
                .groupedCount(1)
                .emailSent(false)
                .build();
            
            notificationRepository.save(weeklyReport);
            
        } catch (Exception e) {
            log.error("❌ Erreur génération rapport admin {}: {}", adminId, e.getMessage());
        }
    }

    /**
     * Méthode utilitaire pour créer une notification critique
     */
    private void createCriticalNotification(Long adminId, Long collecteurId, NotificationType type,
                                          String title, String message) {
        try {
            AdminNotification notification = AdminNotification.builder()
                .adminId(adminId)
                .collecteurId(collecteurId)
                .type(type)
                .priority(Priority.CRITIQUE)
                .title(title)
                .message(message)
                .dateCreation(LocalDateTime.now())
                .lu(false)
                .groupedCount(1)
                .emailSent(false)
                .build();
            
            notificationRepository.save(notification);
            
            // Envoi email asynchrone pour notification critique
            CompletableFuture.runAsync(() -> sendCriticalNotification(adminId, notification));
            
            log.info("🚨 Notification critique créée: {} pour admin {}", type, adminId);
            
        } catch (Exception e) {
            log.error("❌ Erreur création notification critique: {}", e.getMessage());
        }
    }
}