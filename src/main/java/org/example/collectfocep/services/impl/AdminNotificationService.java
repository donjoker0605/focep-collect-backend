package org.example.collectfocep.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ActivityEvent;
import org.example.collectfocep.dto.ActivitySummary;
import org.example.collectfocep.dto.AdminDashboardActivities;
import org.example.collectfocep.dto.NotificationCritique;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;
import org.example.collectfocep.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@Transactional
public class AdminNotificationService {

    private final AdminNotificationRepository notificationRepository;
    private final EmailService emailService;
    private final AdminRepository adminRepository;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final MouvementRepository mouvementRepository;
    private final JournalActiviteRepository journalActiviteRepository;

    // CONFIGURATION SEUILS AMÉLIORÉE - maintenant configurable
    private static final double SEUIL_TRANSACTION_CRITIQUE_DEFAULT = 100_000.0;
    private static final int SEUIL_INACTIVITE_HEURES = 24;

    // Circuit breaker pour éviter spam
    private final Map<String, LocalDateTime> lastNotificationMap = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.time.Duration NOTIFICATION_COOLDOWN = java.time.Duration.ofMinutes(5);

    @Autowired
    public AdminNotificationService(
            AdminNotificationRepository notificationRepository,
            EmailService emailService,
            AdminRepository adminRepository,
            CollecteurRepository collecteurRepository,
            ClientRepository clientRepository,
            MouvementRepository mouvementRepository,
            JournalActiviteRepository journalActiviteRepository) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.adminRepository = adminRepository;
        this.collecteurRepository = collecteurRepository;
        this.clientRepository = clientRepository;
        this.mouvementRepository = mouvementRepository;
        this.journalActiviteRepository = journalActiviteRepository;
    }

    /**
     * MÉTHODE PRINCIPALE AMÉLIORÉE: Évaluer et créer notification si critique
     */
    public void evaluateAndNotify(ActivityEvent event) {
        try {
            log.debug("🔍 Évaluation événement: type={}, collecteur={}",
                    event.getType(), event.getCollecteurId());

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
            AdminNotification existingNotif = findGroupableNotification(
                    adminId, event.getCollecteurId(), notification.getType());

            if (existingNotif != null) {
                groupNotification(existingNotif, notification);
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
                    .groupedCount(1) //
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

    /**
     * ✅ NOUVEAU: Circuit breaker pour éviter spam notifications
     */
    private boolean isInCooldown(Long collecteurId, NotificationType type) {
        String key = collecteurId + ":" + type.name();
        LocalDateTime lastNotification = lastNotificationMap.get(key);

        if (lastNotification != null) {
            java.time.Duration timeSinceLastNotification =
                    java.time.Duration.between(lastNotification, LocalDateTime.now());
            return timeSinceLastNotification.compareTo(NOTIFICATION_COOLDOWN) < 0;
        }

        return false;
    }

    private void markNotificationSent(Long collecteurId, NotificationType type) {
        String key = collecteurId + ":" + type.name();
        lastNotificationMap.put(key, LocalDateTime.now());
    }

    /**
     * Grouper notifications similaires
     */
    private AdminNotification findGroupableNotification(Long adminId, Long collecteurId, NotificationType type) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1); // Grouper sur 1h

        return notificationRepository.findRecentGroupable(adminId, collecteurId, type, cutoff)
                .orElse(null);
    }

    private void groupNotification(AdminNotification existing, NotificationCritique newEvent) {
        existing.setGroupedCount(existing.getGroupedCount() + 1);
        existing.setLastOccurrence(LocalDateTime.now());
        existing.setMessage(existing.getMessage() +
                String.format(" (+%d occurrences)", existing.getGroupedCount() - 1));

        notificationRepository.save(existing);
        log.info("📊 Notification groupée: {} (total: {})",
                existing.getType(), existing.getGroupedCount());
    }

    /**
     * Évaluation avec nouvelles vérifications
     */
    private NotificationCritique evaluateCriticalEvent(ActivityEvent event) {
        switch (event.getType()) {
            case "TRANSACTION_EPARGNE":
            case "TRANSACTION_RETRAIT":
                return evaluateTransactionEvent(event);

            case "CREATE_CLIENT":
                return evaluateClientCreationEvent(event);

            case "CREATE_COLLECTEUR":
                return evaluateCollecteurCreationEvent(event);

            case "MODIFY_COLLECTEUR":
                return evaluateCollecteurModificationEvent(event);

            case "SOLDE_COLLECTEUR_CHECK": // ✅ NOUVEAU
                return evaluateSoldeEvent(event);

            case "COLLECTEUR_LOGIN":
                return evaluateInactivityEvent(event);

            default:
                return null; // Événement non critique
        }
    }

    /**
     * Évaluation transaction avec seuil montantMaxRetrait
     */
    private NotificationCritique evaluateTransactionEvent(ActivityEvent event) {
        Double montant = event.getMontant();
        if (montant == null) return null;

        try {
            Collecteur collecteur = collecteurRepository.findById(event.getCollecteurId()).orElse(null);
            if (collecteur == null) return null;

            // Vérifier d'abord montantMaxRetrait du collecteur
            if ("TRANSACTION_RETRAIT".equals(event.getType()) &&
                    montant > collecteur.getMontantMaxRetrait()) {

                return NotificationCritique.builder()
                        .type(NotificationType.RETRAIT_DEPASSEMENT)
                        .priority(Priority.CRITIQUE)
                        .title("Retrait dépassant le seuil autorisé")
                        .message(String.format(
                                "Retrait de %,.0f FCFA par %s %s dépasse le seuil autorisé de %,.0f FCFA",
                                montant,
                                collecteur.getPrenom(),
                                collecteur.getNom(),
                                collecteur.getMontantMaxRetrait()
                        ))
                        .data(buildTransactionData(event, collecteur.getMontantMaxRetrait()))
                        .build();
            }

            // Ensuite vérifier seuil global
            if (montant >= SEUIL_TRANSACTION_CRITIQUE_DEFAULT) {
                String collecteurNom = collecteur.getPrenom() + " " + collecteur.getNom();

                return NotificationCritique.builder()
                        .type(NotificationType.TRANSACTION_IMPORTANTE)
                        .priority(Priority.CRITIQUE)
                        .title("Transaction importante détectée")
                        .message(String.format("Transaction de %,.0f FCFA par %s",
                                montant, collecteurNom))
                        .data(buildTransactionData(event, SEUIL_TRANSACTION_CRITIQUE_DEFAULT))
                        .build();
            }

        } catch (Exception e) {
            log.error("❌ Erreur évaluation transaction: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Évaluation client GPS avec priority HAUTE (critique selon tes specs)
     */
    private NotificationCritique evaluateClientCreationEvent(ActivityEvent event) {
        if (event.getEntityId() == null) return null;

        try {
            Client client = clientRepository.findById(event.getEntityId()).orElse(null);

            if (client != null && (client.getLatitude() == null || client.getLongitude() == null)) {
                String collecteurNom = getCollecteurNom(event.getCollecteurId());

                return NotificationCritique.builder()
                        .type(NotificationType.CLIENT_SANS_GPS)
                        .priority(Priority.HAUTE)
                        .title("Client créé sans géolocalisation")
                        .message(String.format("Client %s %s créé par %s sans coordonnées GPS",
                                client.getPrenom(), client.getNom(), collecteurNom))
                        .data(buildClientData(client))
                        .build();
            }
        } catch (Exception e) {
            log.error("❌ Erreur évaluation client GPS: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Évaluation création collecteur
     */
    private NotificationCritique evaluateCollecteurCreationEvent(ActivityEvent event) {
        if (event.getEntityId() == null) return null;

        try {
            Collecteur collecteur = collecteurRepository.findById(event.getEntityId()).orElse(null);
            if (collecteur == null) return null;

            return NotificationCritique.builder()
                    .type(NotificationType.COLLECTEUR_CREATED)
                    .priority(Priority.NORMALE)
                    .title("Nouveau collecteur créé")
                    .message(String.format(
                            "Nouveau collecteur %s %s créé dans l'agence %s",
                            collecteur.getPrenom(),
                            collecteur.getNom(),
                            collecteur.getAgence().getNomAgence()
                    ))
                    .data(buildCollecteurData(collecteur))
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur évaluation création collecteur: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Évaluation modification collecteur
     */
    private NotificationCritique evaluateCollecteurModificationEvent(ActivityEvent event) {
        if (event.getEntityId() == null) return null;

        try {
            Collecteur collecteur = collecteurRepository.findById(event.getEntityId()).orElse(null);
            if (collecteur == null) return null;

            return NotificationCritique.builder()
                    .type(NotificationType.COLLECTEUR_MODIFIED)
                    .priority(Priority.NORMALE)
                    .title("Collecteur modifié")
                    .message(String.format(
                            "Collecteur %s %s a été modifié",
                            collecteur.getPrenom(),
                            collecteur.getNom()
                    ))
                    .data(buildCollecteurData(collecteur))
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur évaluation modification collecteur: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Évaluation solde négatif
     */
    private NotificationCritique evaluateSoldeEvent(ActivityEvent event) {
        Double solde = event.getSolde();

        if (solde != null && solde < 0) {
            String collecteurNom = getCollecteurNom(event.getCollecteurId());

            return NotificationCritique.builder()
                    .type(NotificationType.SOLDE_NEGATIF)
                    .priority(Priority.CRITIQUE)
                    .title("Solde collecteur négatif")
                    .message(String.format("Solde de %s: %,.0f FCFA (négatif)",
                            collecteurNom, solde))
                    .data(buildSoldeData(event))
                    .build();
        }

        return null;
    }

    /**
     * Évaluation inactivité (placeholder)
     */
    private NotificationCritique evaluateInactivityEvent(ActivityEvent event) {
        // À implémenter selon tes besoins d'inactivité
        return null;
    }

    /**
     * Trouver l'admin responsable d'un collecteur
     */
    private Long findResponsibleAdmin(Long collecteurId) {
        return collecteurRepository.findById(collecteurId)
                .map(collecteur -> {
                    Long agenceId = collecteur.getAgence().getId();
                    return adminRepository.findByAgenceId(agenceId)
                            .map(Admin::getId)
                            .orElse(null);
                })
                .orElse(null);
    }

    /**
     * Envoi email critique asynchrone
     */
    private void sendCriticalNotification(Long adminId, AdminNotification notification) {
        try {
            Admin admin = adminRepository.findById(adminId).orElse(null);
            if (admin != null && admin.getAdresseMail() != null) {
                EmailNotification email = EmailNotification.builder()
                        .destinataire(admin.getAdresseMail())
                        .sujet("[URGENT] " + notification.getTitle())
                        .contenu(buildEmailContent(notification))
                        .build();

                emailService.sendAsync(email);

                // Marquer email envoyé
                notification.setEmailSent(true);
                notificationRepository.save(notification);

                log.info("📧 Email critique envoyé à: {}", admin.getAdresseMail());
            }
        } catch (Exception e) {
            log.error("❌ Erreur envoi email critique: {}", e.getMessage(), e);
        }
    }

    // ===== NIVEAU 2: DASHBOARD ADMIN (POLLING) - AMÉLIORÉ =====

    /**
     * Dashboard admin avec cache intelligent
     */
    @Cacheable(value = "admin-dashboard", key = "#adminId + '-' + #lastMinutes",
            condition = "#lastMinutes <= 60") // Cache seulement les requêtes récentes
    public AdminDashboardActivities getDashboardActivities(Long adminId, int lastMinutes) {
        try {
            log.debug("📊 Calcul dashboard admin: adminId={}, lastMinutes={}", adminId, lastMinutes);

            Long agenceId = getAdminAgenceId(adminId);
            LocalDateTime since = LocalDateTime.now().minusMinutes(lastMinutes);

            // Récupérer les collecteurs de l'agence
            List<Long> collecteurIds = collecteurRepository.findIdsByAgenceId(agenceId);

            // Activités récentes des collecteurs
            List<ActivitySummary> activities = journalActiviteRepository
                    .findRecentActivitiesByCollecteurs(collecteurIds, since);

            // Notifications non lues
            Long unreadCount = notificationRepository.countByAdminIdAndLuFalse(adminId);

            // Notifications urgentes
            Long urgentCount = notificationRepository.countByAdminIdAndPriorityAndLuFalse(
                    adminId, Priority.CRITIQUE);

            // Statistiques rapides
            Map<String, Long> stats = calculateQuickStats(collecteurIds, since);

            return AdminDashboardActivities.builder()
                    .agenceId(agenceId)
                    .adminId(adminId)
                    .lastUpdate(LocalDateTime.now())
                    .activitiesCount((long) activities.size())
                    .unreadNotifications(unreadCount)
                    .urgentNotifications(urgentCount) // ✅ NOUVEAU
                    .activities(activities)
                    .stats(stats)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur dashboard admin: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors du chargement du dashboard", e);
        }
    }

    /**
     * Récupérer notifications critiques non lues
     */
    public List<AdminNotification> getCriticalNotifications(Long adminId) {
        return notificationRepository.findUnreadByAdminIdOrderByPriorityAndDate(adminId);
    }

    /**
     * Marquer notification comme lue
     */
    @Transactional
    public void markAsRead(Long notificationId, Long adminId) {
        AdminNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        // Vérifier autorisation
        if (!notification.getAdminId().equals(adminId)) {
            throw new RuntimeException("Accès non autorisé à cette notification");
        }

        notification.setLu(true);
        notification.setDateLecture(LocalDateTime.now());
        notificationRepository.save(notification);

        log.info("✅ Notification marquée comme lue: id={}, admin={}", notificationId, adminId);
    }

    // ===== MÉTHODES UTILITAIRES =====

    private String getCollecteurNom(Long collecteurId) {
        return collecteurRepository.findById(collecteurId)
                .map(c -> c.getPrenom() + " " + c.getNom())
                .orElse("Collecteur inconnu");
    }

    private Long getAdminAgenceId(Long adminId) {
        return adminRepository.findById(adminId)
                .map(admin -> admin.getAgence().getId())
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));
    }

    /**
     * Statistiques rapides pour le dashboard
     */
    private Map<String, Long> calculateQuickStats(List<Long> collecteurIds, LocalDateTime since) {
        return Map.of(
                "transactions", mouvementRepository.countByCollecteurIdsAndDateAfter(collecteurIds, since),
                "nouveauxClients", clientRepository.countByCollecteurIdsAndDateAfter(collecteurIds, since),
                "collecteursActifs", countByCollecteurIdsAndDateAfter(collecteurIds, since),
        );
    }

    // Méthodes de construction des données JSON
    private String buildTransactionData(ActivityEvent event, Double threshold) {
        try {
            Map<String, Object> data = Map.of(
                    "montant", event.getMontant(),
                    "threshold", threshold,
                    "collecteurId", event.getCollecteurId(),
                    "entityId", event.getEntityId() != null ? event.getEntityId() : 0L,
                    "timestamp", event.getTimestamp()
            );
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildClientData(Client client) {
        try {
            Map<String, Object> data = Map.of(
                    "clientId", client.getId(),
                    "nom", client.getNom(),
                    "prenom", client.getPrenom(),
                    "hasGps", client.getLatitude() != null && client.getLongitude() != null
            );
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildCollecteurData(Collecteur collecteur) {
        try {
            Map<String, Object> data = Map.of(
                    "collecteurId", collecteur.getId(),
                    "nom", collecteur.getNom(),
                    "prenom", collecteur.getPrenom(),
                    "agenceNom", collecteur.getAgence().getNomAgence()
            );
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildSoldeData(ActivityEvent event) {
        try {
            Map<String, Object> data = Map.of(
                    "solde", event.getSolde(),
                    "collecteurId", event.getCollecteurId(),
                    "timestamp", event.getTimestamp()
            );
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildEmailContent(AdminNotification notification) {
        StringBuilder content = new StringBuilder();
        content.append("<h2>Notification Critique - FOCEP Collecte</h2>");
        content.append("<p><strong>Type:</strong> ").append(notification.getType()).append("</p>");
        content.append("<p><strong>Priorité:</strong> ").append(notification.getPriority()).append("</p>");
        content.append("<p><strong>Message:</strong> ").append(notification.getMessage()).append("</p>");
        content.append("<p><strong>Date:</strong> ").append(notification.getDateCreation()).append("</p>");
        content.append("<hr>");
        content.append("<p>Veuillez vous connecter à votre dashboard administrateur pour plus de détails.</p>");
        content.append("<p>FOCEP Collecte - Système de notifications automatiques</p>");
        return content.toString();
    }

    public List<AdminNotification> getAllNotifications(Long adminId) {
        return notificationRepository.findByAdminIdOrderByDateCreationDesc(adminId);
    }

    public Long getUnreadCount(Long adminId) {
        return notificationRepository.countByAdminIdAndLuFalse(adminId);
    }

    @Transactional
    public void markAllAsRead(Long adminId) {
        List<AdminNotification> unreadNotifications = notificationRepository
                .findByAdminIdAndLuFalse(adminId);

        unreadNotifications.forEach(notification -> {
            notification.setLu(true);
            notification.setDateLecture(LocalDateTime.now());
        });

        notificationRepository.saveAll(unreadNotifications);
        log.info("✅ Toutes les notifications marquées comme lues pour admin: {}", adminId);
    }

    // Méthode pour nettoyer les anciennes notifications (à appeler périodiquement)
    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90); // Garder 90 jours
        notificationRepository.deleteOldReadNotifications(cutoff);
        log.info("🧹 Nettoyage notifications anciennes > 90 jours effectué");
    }
}