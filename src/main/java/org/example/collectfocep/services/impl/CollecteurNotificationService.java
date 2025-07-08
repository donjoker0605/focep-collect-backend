package org.example.collectfocep.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.CollecteurNotification;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.repositories.CollecteurNotificationRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 🔔 Service pour la gestion des notifications des collecteurs
 *
 * FONCTIONNALITÉS PRINCIPALES :
 * 1. Envoi de notifications critiques aux collecteurs
 * 2. Gestion du cycle de vie des notifications
 * 3. Notifications automatiques basées sur les événements
 * 4. Nettoyage automatique des notifications expirées
 * 5. Statistiques et métriques de notifications
 *
 * TYPES DE NOTIFICATIONS SUPPORTÉES :
 * - Alertes critiques (solde insuffisant, erreurs système)
 * - Rappels (clôture journal, synchronisation)
 * - Messages administrateurs
 * - Informations système
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CollecteurNotificationService {

    private final CollecteurNotificationRepository notificationRepository;
    private final CollecteurRepository collecteurRepository;
    private final ObjectMapper objectMapper;

    // Configuration
    private static final int MAX_TENTATIVES_ENVOI = 3;
    private static final int NOTIFICATION_BATCH_SIZE = 100;

    // =====================================
    // ENVOI DE NOTIFICATIONS CRITIQUES
    // =====================================

    /**
     * 🚨 Envoie une notification critique à un collecteur
     */
    @Async
    @CacheEvict(value = "collecteurNotifications", key = "#collecteurId")
    public CompletableFuture<Boolean> sendCriticalNotificationToCollecteur(
            Long collecteurId, String titre, String message, Map<String, Object> donnees) {

        log.info("🚨 Envoi notification critique au collecteur {}: {}", collecteurId, titre);

        try {
            // Vérification que le collecteur existe
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new IllegalArgumentException("Collecteur non trouvé: " + collecteurId));

            // Création de la notification
            CollecteurNotification notification = CollecteurNotification.urgent()
                    .collecteurId(collecteurId)
                    .titre(titre)
                    .message(message)
                    .donnees(donnees != null ? serializeData(donnees) : null)
                    .icone("🚨")
                    .couleur("#F44336")
                    .categorie("CRITICAL")
                    .build();

            // Sauvegarde en base
            notification = notificationRepository.save(notification);

            // Tentative d'envoi push
            boolean envoiReussi = envoyerNotificationPush(notification);

            if (envoiReussi) {
                notification.marquerCommeEnvoye();
                notificationRepository.save(notification);
                log.info("✅ Notification critique envoyée avec succès au collecteur {}", collecteurId);
            } else {
                log.warn("⚠️ Échec envoi push notification critique au collecteur {}", collecteurId);
            }

            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de notification critique au collecteur {}: {}",
                    collecteurId, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 📋 Rappel de clôture de journal
     */
    @Async
    public CompletableFuture<Void> sendJournalClotureReminder(Long collecteurId, Long journalId) {

        log.info("📋 Envoi rappel clôture journal {} au collecteur {}", journalId, collecteurId);

        try {
            Map<String, Object> donnees = Map.of(
                    "journalId", journalId,
                    "action", "CLOTURER_JOURNAL",
                    "url", "/collecteur/journal/" + journalId + "/cloture"
            );

            CollecteurNotification notification = CollecteurNotification.rappel()
                    .collecteurId(collecteurId)
                    .titre("Rappel : Clôture de journal requise")
                    .message("Votre journal de collecte est ouvert depuis plus de 24h. Veuillez procéder à sa clôture.")
                    .donnees(serializeData(donnees))
                    .entityType("JOURNAL")
                    .entityId(journalId)
                    .icone("📋")
                    .couleur("#FF9800")
                    .categorie("JOURNAL")
                    .build();

            notificationRepository.save(notification);
            envoyerNotificationPush(notification);

            log.info("✅ Rappel clôture journal envoyé au collecteur {}", collecteurId);

        } catch (Exception e) {
            log.error("❌ Erreur envoi rappel clôture journal: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 💰 Alerte de solde insuffisant
     */
    @Async
    public CompletableFuture<Void> sendSoldeInsuffisantAlert(Long collecteurId, Double soldeActuel, Double montantDemande) {

        log.warn("💰 Envoi alerte solde insuffisant au collecteur {}: solde={}, demandé={}",
                collecteurId, soldeActuel, montantDemande);

        try {
            Map<String, Object> donnees = Map.of(
                    "soldeActuel", soldeActuel,
                    "montantDemande", montantDemande,
                    "deficit", montantDemande - soldeActuel,
                    "action", "RECHARGER_SOLDE"
            );

            String message = String.format(
                    "Solde insuffisant pour cette opération. Solde actuel: %.2f FCFA, Montant requis: %.2f FCFA. " +
                            "Veuillez recharger votre compte ou contacter votre administrateur.",
                    soldeActuel, montantDemande
            );

            CollecteurNotification notification = CollecteurNotification.urgent()
                    .collecteurId(collecteurId)
                    .titre("⚠️ Solde insuffisant")
                    .message(message)
                    .donnees(serializeData(donnees))
                    .type(CollecteurNotification.NotificationType.SOLDE_ALERT)
                    .icone("💰")
                    .couleur("#F44336")
                    .categorie("FINANCE")
                    .build();

            notificationRepository.save(notification);
            envoyerNotificationPush(notification);

            log.info("✅ Alerte solde insuffisant envoyée au collecteur {}", collecteurId);

        } catch (Exception e) {
            log.error("❌ Erreur envoi alerte solde: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 📨 Message d'un administrateur vers un collecteur
     */
    @Async
    @CacheEvict(value = "collecteurNotifications", key = "#collecteurId")
    public CompletableFuture<Boolean> sendAdminMessage(Long adminId, String adminNom,
                                                       Long collecteurId, String titre, String message,
                                                       CollecteurNotification.Priorite priorite) {

        log.info("📨 Admin {} envoie message au collecteur {}: {}", adminNom, collecteurId, titre);

        try {
            CollecteurNotification notification = CollecteurNotification.messageAdmin(adminId, adminNom)
                    .collecteurId(collecteurId)
                    .titre(titre)
                    .message(message)
                    .priorite(priorite != null ? priorite : CollecteurNotification.Priorite.NORMAL)
                    .icone("📨")
                    .categorie("ADMIN_MESSAGE")
                    .persistante(true) // Les messages admin persistent après lecture
                    .build();

            notification = notificationRepository.save(notification);
            boolean envoiReussi = envoyerNotificationPush(notification);

            log.info("✅ Message admin envoyé au collecteur {}, push: {}", collecteurId, envoiReussi);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("❌ Erreur envoi message admin: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * ℹ️ Notification d'information générale
     */
    @Async
    public CompletableFuture<Void> sendInfoNotification(Long collecteurId, String titre, String message,
                                                        String categorie, Map<String, Object> donnees) {

        log.debug("ℹ️ Envoi notification info au collecteur {}: {}", collecteurId, titre);

        try {
            CollecteurNotification notification = CollecteurNotification.info()
                    .collecteurId(collecteurId)
                    .titre(titre)
                    .message(message)
                    .donnees(donnees != null ? serializeData(donnees) : null)
                    .categorie(categorie)
                    .icone("ℹ️")
                    .build();

            notificationRepository.save(notification);
            envoyerNotificationPush(notification);

            log.debug("✅ Notification info envoyée au collecteur {}", collecteurId);

        } catch (Exception e) {
            log.error("❌ Erreur envoi notification info: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    // =====================================
    // GESTION DES NOTIFICATIONS
    // =====================================

    /**
     * 📋 Récupère les notifications d'un collecteur avec pagination
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "collecteurNotifications", key = "#collecteurId + '_' + #pageable.pageNumber")
    public Page<CollecteurNotification> getNotificationsByCollecteur(Long collecteurId, Pageable pageable) {

        log.debug("📋 Récupération notifications collecteur {}, page {}", collecteurId, pageable.getPageNumber());
        return notificationRepository.findByCollecteurId(collecteurId, pageable);
    }

    /**
     * 📮 Récupère les notifications non lues d'un collecteur
     */
    @Transactional(readOnly = true)
    public List<CollecteurNotification> getUnreadNotifications(Long collecteurId) {

        log.debug("📮 Récupération notifications non lues collecteur {}", collecteurId);
        return notificationRepository.findUnreadByCollecteurId(collecteurId);
    }

    /**
     * 🚨 Récupère les notifications urgentes non lues
     */
    @Transactional(readOnly = true)
    public List<CollecteurNotification> getUrgentUnreadNotifications(Long collecteurId) {

        log.debug("🚨 Récupération notifications urgentes collecteur {}", collecteurId);
        return notificationRepository.findUrgentUnreadByCollecteurId(collecteurId);
    }

    /**
     * 📊 Nombre de notifications non lues
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "collecteurNotificationCount", key = "#collecteurId")
    public Long getUnreadCount(Long collecteurId) {

        return notificationRepository.countUnreadByCollecteurId(collecteurId);
    }

    /**
     * ✅ Marquer une notification comme lue
     */
    @CacheEvict(value = {"collecteurNotifications", "collecteurNotificationCount"}, key = "#collecteurId")
    public boolean markAsRead(Long notificationId, Long collecteurId) {

        log.debug("✅ Marquage notification {} comme lue pour collecteur {}", notificationId, collecteurId);

        int updated = notificationRepository.markAsRead(notificationId, collecteurId, LocalDateTime.now());
        return updated > 0;
    }

    /**
     * ✅ Marquer toutes les notifications comme lues
     */
    @CacheEvict(value = {"collecteurNotifications", "collecteurNotificationCount"}, key = "#collecteurId")
    public int markAllAsRead(Long collecteurId) {

        log.info("✅ Marquage toutes notifications comme lues pour collecteur {}", collecteurId);
        return notificationRepository.markAllAsRead(collecteurId, LocalDateTime.now());
    }

    /**
     * 🗑️ Supprimer une notification (si suppressible)
     */
    @CacheEvict(value = {"collecteurNotifications", "collecteurNotificationCount"}, key = "#collecteurId")
    public boolean deleteNotification(Long notificationId, Long collecteurId) {

        log.debug("🗑️ Suppression notification {} pour collecteur {}", notificationId, collecteurId);

        try {
            Optional<CollecteurNotification> notificationOpt = notificationRepository.findById(notificationId);

            if (notificationOpt.isPresent()) {
                CollecteurNotification notification = notificationOpt.get();

                // Vérifier que la notification appartient au collecteur et est suppressible
                if (notification.getCollecteurId().equals(collecteurId) &&
                        Boolean.TRUE.equals(notification.getSuppressible())) {

                    notificationRepository.delete(notification);
                    return true;
                } else {
                    log.warn("⚠️ Tentative de suppression notification non autorisée: {}", notificationId);
                }
            }

            return false;

        } catch (Exception e) {
            log.error("❌ Erreur suppression notification {}: {}", notificationId, e.getMessage(), e);
            return false;
        }
    }

    // =====================================
    // STATISTIQUES ET MÉTRIQUES
    // =====================================

    /**
     * 📊 Statistiques des notifications d'un collecteur
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStatistics(Long collecteurId) {

        log.debug("📊 Calcul statistiques notifications collecteur {}", collecteurId);

        try {
            Map<String, Object> stats = new HashMap<>();

            // Stats générales
            Long totalCount = notificationRepository.count();
            Long unreadCount = notificationRepository.countUnreadByCollecteurId(collecteurId);
            Long urgentCount = notificationRepository.countUrgentUnreadByCollecteurId(collecteurId);

            stats.put("total", totalCount);
            stats.put("nonLues", unreadCount);
            stats.put("urgentes", urgentCount);

            // Stats par type
            List<Object[]> statsByType = notificationRepository.getStatistiquesByType(collecteurId);
            Map<String, Map<String, Long>> typeStats = new HashMap<>();

            for (Object[] row : statsByType) {
                CollecteurNotification.NotificationType type = (CollecteurNotification.NotificationType) row[0];
                Long total = (Long) row[1];
                Long nonLues = (Long) row[2];

                typeStats.put(type.name(), Map.of("total", total, "nonLues", nonLues));
            }
            stats.put("parType", typeStats);

            // Stats par priorité
            List<Object[]> statsByPriority = notificationRepository.getStatistiquesByPriorite(collecteurId);
            Map<String, Map<String, Long>> priorityStats = new HashMap<>();

            for (Object[] row : statsByPriority) {
                CollecteurNotification.Priorite priorite = (CollecteurNotification.Priorite) row[0];
                Long total = (Long) row[1];
                Long nonLues = (Long) row[2];

                priorityStats.put(priorite.name(), Map.of("total", total, "nonLues", nonLues));
            }
            stats.put("parPriorite", priorityStats);

            return stats;

        } catch (Exception e) {
            log.error("❌ Erreur calcul statistiques notifications: {}", e.getMessage(), e);
            return Map.of("error", "Erreur lors du calcul des statistiques");
        }
    }

    // =====================================
    // NOTIFICATIONS AUTOMATIQUES
    // =====================================

    /**
     * 🔄 Détecte et envoie des notifications automatiques basées sur l'activité
     */
    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes
    public void detectAndSendAutomaticNotifications() {

        log.debug("🔄 Détection notifications automatiques...");

        try {
            // Détection des collecteurs inactifs
            detectInactiveCollecteurs();

            // Détection des journaux non clôturés
            detectUnclosedJournals();

            // Détection des erreurs de synchronisation
            detectSyncErrors();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la détection de notifications automatiques: {}", e.getMessage(), e);
        }
    }

    /**
     * 🧹 Nettoyage automatique des notifications expirées
     */
    @Scheduled(cron = "0 0 2 * * *") // Tous les jours à 2h du matin
    public void cleanupExpiredNotifications() {

        log.info("🧹 Début nettoyage notifications expirées...");

        try {
            LocalDateTime maintenant = LocalDateTime.now();

            // Compter les notifications à nettoyer
            Long toCleanup = notificationRepository.countNotificationsToCleanup(maintenant);

            if (toCleanup > 0) {
                // Supprimer les notifications expirées et lues
                int deleted = notificationRepository.deleteExpiredAndRead(maintenant);

                log.info("✅ Nettoyage terminé: {} notifications supprimées", deleted);

                // Vider les caches après nettoyage
                clearNotificationCaches();
            } else {
                log.debug("✅ Aucune notification à nettoyer");
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage des notifications: {}", e.getMessage(), e);
        }
    }

    // =====================================
    // MÉTHODES PRIVÉES
    // =====================================

    /**
     * 📤 Envoi effectif de la notification push
     */
    private boolean envoyerNotificationPush(CollecteurNotification notification) {

        try {
            // TODO: Intégration avec service de push notifications (Firebase, etc.)
            // Pour l'instant, simulation d'envoi

            log.debug("📤 Simulation envoi push notification {}", notification.getId());

            // Simuler délai d'envoi
            Thread.sleep(100);

            // Simuler succès d'envoi (95% de réussite)
            boolean success = Math.random() > 0.05;

            if (success) {
                notification.marquerCommeEnvoye();
            } else {
                notification.incrementerTentativesEnvoi();
                notification.setErreurEnvoi("Erreur simulée de test");
            }

            return success;

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi push: {}", e.getMessage(), e);
            notification.incrementerTentativesEnvoi();
            notification.setErreurEnvoi(e.getMessage());
            return false;
        }
    }

    /**
     * 📋 Sérialise les données en JSON
     */
    private String serializeData(Map<String, Object> donnees) {
        try {
            return objectMapper.writeValueAsString(donnees);
        } catch (Exception e) {
            log.error("❌ Erreur sérialisation données notification: {}", e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * 👤 Détecte les collecteurs inactifs
     */
    private void detectInactiveCollecteurs() {

        // TODO: Implémenter détection basée sur JournalActivite
        log.debug("🔍 Détection collecteurs inactifs...");

        // Logique à implémenter:
        // 1. Récupérer collecteurs sans activité depuis 24h
        // 2. Envoyer notification de rappel
        // 3. Alerter les admins si inactivité > 48h
    }

    /**
     * 📋 Détecte les journaux non clôturés
     */
    private void detectUnclosedJournals() {

        // TODO: Implémenter détection basée sur Journal
        log.debug("🔍 Détection journaux non clôturés...");

        // Logique à implémenter:
        // 1. Récupérer journaux ouverts depuis > 24h
        // 2. Envoyer rappel de clôture
        // 3. Escalade vers admin si > 48h
    }

    /**
     * 🔄 Détecte les erreurs de synchronisation
     */
    private void detectSyncErrors() {

        // TODO: Implémenter détection erreurs sync
        log.debug("🔍 Détection erreurs synchronisation...");

        // Logique à implémenter:
        // 1. Vérifier dernière sync réussie
        // 2. Détecter données en attente
        // 3. Alerter si problème persistant
    }

    /**
     * 🧹 Vide les caches de notifications
     */
    private void clearNotificationCaches() {

        // TODO: Implémenter vidage cache avec CacheManager
        log.debug("🧹 Vidage caches notifications...");
    }

    // =====================================
    // NOTIFICATIONS SPÉCIALISÉES
    // =====================================

    /**
     * 🎯 Notification de commission calculée
     */
    @Async
    public CompletableFuture<Void> sendCommissionNotification(Long collecteurId, Double montantCommission,
                                                              LocalDateTime periode) {

        log.info("🎯 Envoi notification commission {} FCFA au collecteur {}", montantCommission, collecteurId);

        try {
            Map<String, Object> donnees = Map.of(
                    "montantCommission", montantCommission,
                    "periode", periode.toString(),
                    "action", "CONSULTER_COMMISSIONS"
            );

            String message = String.format(
                    "Votre commission pour la période du %s a été calculée: %.2f FCFA. " +
                            "Consultez le détail dans votre espace commissions.",
                    periode.toLocalDate(), montantCommission
            );

            CollecteurNotification notification = CollecteurNotification.info()
                    .collecteurId(collecteurId)
                    .titre("💰 Commission calculée")
                    .message(message)
                    .donnees(serializeData(donnees))
                    .type(CollecteurNotification.NotificationType.COMMISSION_INFO)
                    .icone("💰")
                    .couleur("#4CAF50")
                    .categorie("COMMISSION")
                    .build();

            notificationRepository.save(notification);
            envoyerNotificationPush(notification);

        } catch (Exception e) {
            log.error("❌ Erreur envoi notification commission: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 👥 Notification de mise à jour client
     */
    @Async
    public CompletableFuture<Void> sendClientUpdateNotification(Long collecteurId, Long clientId,
                                                                String clientNom, String typeModification) {

        log.debug("👥 Notification mise à jour client {} pour collecteur {}", clientNom, collecteurId);

        try {
            Map<String, Object> donnees = Map.of(
                    "clientId", clientId,
                    "clientNom", clientNom,
                    "typeModification", typeModification,
                    "action", "CONSULTER_CLIENT"
            );

            String message = String.format(
                    "Le client %s a été %s. Consultez les détails dans votre liste de clients.",
                    clientNom, typeModification
            );

            CollecteurNotification notification = CollecteurNotification.info()
                    .collecteurId(collecteurId)
                    .titre("👥 Mise à jour client")
                    .message(message)
                    .donnees(serializeData(donnees))
                    .type(CollecteurNotification.NotificationType.CLIENT_UPDATE)
                    .entityType("CLIENT")
                    .entityId(clientId)
                    .icone("👥")
                    .categorie("CLIENT")
                    .build();

            notificationRepository.save(notification);
            envoyerNotificationPush(notification);

        } catch (Exception e) {
            log.error("❌ Erreur envoi notification client: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }
}