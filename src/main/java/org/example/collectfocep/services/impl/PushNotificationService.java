package org.example.collectfocep.services.impl;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.CollecteurNotification;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 🔔 Service pour l'envoi de notifications push via Firebase Cloud Messaging
 *
 * FONCTIONNALITÉS :
 * - Envoi de notifications push individuelles et en masse
 * - Gestion des tokens FCM des collecteurs
 * - Retry automatique avec backoff exponentiel
 * - Support des notifications riches (images, actions)
 * - Statistiques d'envoi et de livraison
 * - Nettoyage automatique des tokens invalides
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PushNotificationService {

    private final CollecteurRepository collecteurRepository;

    @Value("${app.firebase.server-key:}")
    private String firebaseServerKey;

    @Value("${app.push.max-retries:3}")
    private int maxRetries;

    @Value("${app.push.batch-size:500}")
    private int batchSize;

    @Value("${app.push.timeout-seconds:30}")
    private int timeoutSeconds;

    // =====================================
    // ENVOI DE NOTIFICATIONS INDIVIDUELLES
    // =====================================

    /**
     * 📤 Envoie une notification push à un collecteur spécifique
     */
    @Async
    public CompletableFuture<PushNotificationResult> sendToCollecteur(
            Long collecteurId, CollecteurNotification notification) {

        log.info("📤 Envoi notification push au collecteur {}: {}", collecteurId, notification.getTitre());

        try {
            // Récupération du token FCM du collecteur
            Optional<Collecteur> collecteurOpt = collecteurRepository.findById(collecteurId);
            if (collecteurOpt.isEmpty()) {
                throw new IllegalArgumentException("Collecteur non trouvé: " + collecteurId);
            }

            Collecteur collecteur = collecteurOpt.get();
            String fcmToken = collecteur.getFcmToken();

            if (fcmToken == null || fcmToken.trim().isEmpty()) {
                log.warn("⚠️ Aucun token FCM pour le collecteur {}", collecteurId);
                return CompletableFuture.completedFuture(
                        PushNotificationResult.failure("Aucun token FCM disponible")
                );
            }

            // Construction du message FCM
            Message message = buildFCMMessage(fcmToken, notification);

            // Envoi avec retry
            PushNotificationResult result = sendWithRetry(message, 0);

            // Mise à jour des statistiques
            updateNotificationStats(notification, result);

            log.info("✅ Notification envoyée au collecteur {}: {}", collecteurId, result.isSuccess());
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("❌ Erreur envoi notification au collecteur {}: {}", collecteurId, e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    PushNotificationResult.failure("Erreur d'envoi: " + e.getMessage())
            );
        }
    }

    /**
     * 📤 Envoie une notification push à plusieurs collecteurs
     */
    @Async
    public CompletableFuture<BatchPushResult> sendToMultipleCollecteurs(
            List<Long> collecteurIds, CollecteurNotification notification) {

        log.info("📤 Envoi notification en masse à {} collecteurs: {}",
                collecteurIds.size(), notification.getTitre());

        try {
            // Récupération des tokens FCM
            List<Collecteur> collecteurs = collecteurRepository.findAllById(collecteurIds);
            List<String> validTokens = new ArrayList<>();
            Map<String, Long> tokenToCollecteurMap = new HashMap<>();

            for (Collecteur collecteur : collecteurs) {
                String token = collecteur.getFcmToken();
                if (token != null && !token.trim().isEmpty()) {
                    validTokens.add(token);
                    tokenToCollecteurMap.put(token, collecteur.getId());
                }
            }

            if (validTokens.isEmpty()) {
                log.warn("⚠️ Aucun token FCM valide trouvé pour les collecteurs");
                return CompletableFuture.completedFuture(
                        BatchPushResult.builder()
                                .totalRequested(collecteurIds.size())
                                .totalSent(0)
                                .successCount(0)
                                .failureCount(collecteurIds.size())
                                .build()
                );
            }

            // Envoi par batch pour éviter les limitations Firebase
            BatchPushResult finalResult = BatchPushResult.builder()
                    .totalRequested(collecteurIds.size())
                    .totalSent(validTokens.size())
                    .build();

            List<List<String>> batches = partitionList(validTokens, batchSize);

            for (List<String> batch : batches) {
                BatchPushResult batchResult = sendBatch(batch, notification);
                finalResult.merge(batchResult);
            }

            log.info("✅ Envoi en masse terminé: {}/{} succès",
                    finalResult.getSuccessCount(), finalResult.getTotalSent());

            return CompletableFuture.completedFuture(finalResult);

        } catch (Exception e) {
            log.error("❌ Erreur envoi en masse: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    BatchPushResult.builder()
                            .totalRequested(collecteurIds.size())
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }

    // =====================================
    // GESTION DES TOKENS FCM
    // =====================================

    /**
     * 🔑 Met à jour le token FCM d'un collecteur
     */
    public void updateCollecteurFCMToken(Long collecteurId, String newToken) {
        try {
            Optional<Collecteur> collecteurOpt = collecteurRepository.findById(collecteurId);
            if (collecteurOpt.isPresent()) {
                Collecteur collecteur = collecteurOpt.get();
                collecteur.setFcmToken(newToken);
                collecteur.setFcmTokenUpdatedAt(LocalDateTime.now());
                collecteurRepository.save(collecteur);

                log.info("🔑 Token FCM mis à jour pour collecteur {}", collecteurId);
            } else {
                log.warn("⚠️ Collecteur non trouvé pour mise à jour token: {}", collecteurId);
            }
        } catch (Exception e) {
            log.error("❌ Erreur mise à jour token FCM: {}", e.getMessage(), e);
        }
    }

    /**
     * 🧹 Nettoie les tokens FCM invalides
     */
    @Async
    public CompletableFuture<Integer> cleanupInvalidTokens() {
        log.info("🧹 Début nettoyage tokens FCM invalides...");

        try {
            List<Collecteur> collecteurs = collecteurRepository.findCollecteursWithFCMToken();
            int cleanedCount = 0;

            for (Collecteur collecteur : collecteurs) {
                String token = collecteur.getFcmToken();
                if (token != null && !isTokenValid(token)) {
                    collecteur.setFcmToken(null);
                    collecteur.setFcmTokenUpdatedAt(LocalDateTime.now());
                    collecteurRepository.save(collecteur);
                    cleanedCount++;

                    log.debug("🗑️ Token invalide supprimé pour collecteur {}", collecteur.getId());
                }
            }

            log.info("✅ Nettoyage terminé: {} tokens invalides supprimés", cleanedCount);
            return CompletableFuture.completedFuture(cleanedCount);

        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage des tokens: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(0);
        }
    }

    // =====================================
    // MÉTHODES PRIVÉES
    // =====================================

    /**
     * 🏗️ Construit un message FCM à partir d'une notification
     */
    private Message buildFCMMessage(String fcmToken, CollecteurNotification notification) {
        // Construction des données personnalisées
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", notification.getId().toString());
        data.put("type", notification.getType().name());
        data.put("priority", notification.getPriorite().name());

        if (notification.getEntityType() != null) {
            data.put("entityType", notification.getEntityType());
            data.put("entityId", notification.getEntityId().toString());
        }

        if (notification.getActionUrl() != null) {
            data.put("actionUrl", notification.getActionUrl());
        }

        if (notification.getDonnees() != null) {
            data.put("extraData", notification.getDonnees());
        }

        // Configuration de la notification visuelle
        Notification.Builder notificationBuilder = Notification.builder()
                .setTitle(notification.getTitre())
                .setBody(notification.getMessage());

        // Icône et couleur si disponibles
        if (notification.getIconeEffective() != null) {
            // Note: FCM ne supporte pas directement les emojis comme icône
            // Il faudrait mapper vers des icônes de l'app
            data.put("icon", notification.getIconeEffective());
        }

        if (notification.getCouleurEffective() != null) {
            data.put("color", notification.getCouleurEffective());
        }

        // Configuration Android spécifique
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setNotification(AndroidNotification.builder()
                        .setTitle(notification.getTitre())
                        .setBody(notification.getMessage())
                        .setColor(notification.getCouleurEffective())
                        .setPriority(getAndroidPriority(notification.getPriorite()))
                        .setChannelId(getNotificationChannel(notification.getType()))
                        .build())
                .putAllData(data)
                .build();

        // Configuration iOS spécifique
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setAlert(ApsAlert.builder()
                                .setTitle(notification.getTitre())
                                .setBody(notification.getMessage())
                                .build())
                        .setBadge(1) // TODO: Calculer le vrai nombre de notifications
                        .setSound("default")
                        .build())
                .putAllCustomData(data)
                .build();

        return Message.builder()
                .setToken(fcmToken)
                .setNotification(notificationBuilder.build())
                .setAndroidConfig(androidConfig)
                .setApnsConfig(apnsConfig)
                .putAllData(data)
                .build();
    }

    /**
     * 🔄 Envoie un message avec retry automatique
     */
    private PushNotificationResult sendWithRetry(Message message, int attempt) {
        try {
            String messageId = FirebaseMessaging.getInstance().send(message);

            return PushNotificationResult.builder()
                    .success(true)
                    .messageId(messageId)
                    .attempts(attempt + 1)
                    .build();

        } catch (FirebaseMessagingException e) {
            log.warn("⚠️ Échec envoi notification (tentative {}): {}", attempt + 1, e.getMessage());

            // Vérifier si le token est invalide
            if (isTokenError(e)) {
                return PushNotificationResult.builder()
                        .success(false)
                        .errorCode(e.getErrorCode())
                        .errorMessage("Token invalide: " + e.getMessage())
                        .tokenInvalid(true)
                        .attempts(attempt + 1)
                        .build();
            }

            // Retry si possible
            if (attempt < maxRetries && shouldRetry(e)) {
                try {
                    // Backoff exponentiel
                    long delay = (long) Math.pow(2, attempt) * 1000;
                    Thread.sleep(delay);

                    return sendWithRetry(message, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            return PushNotificationResult.builder()
                    .success(false)
                    .errorCode(e.getErrorCode())
                    .errorMessage(e.getMessage())
                    .attempts(attempt + 1)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de l'envoi: {}", e.getMessage(), e);

            return PushNotificationResult.builder()
                    .success(false)
                    .errorMessage("Erreur inattendue: " + e.getMessage())
                    .attempts(attempt + 1)
                    .build();
        }
    }

    /**
     * 📦 Envoie un batch de notifications
     */
    private BatchPushResult sendBatch(List<String> tokens, CollecteurNotification notification) {
        try {
            // Construction des messages pour le batch
            List<Message> messages = new ArrayList<>();
            for (String token : tokens) {
                messages.add(buildFCMMessage(token, notification));
            }

            // Envoi en lot
            BatchResponse batchResponse = FirebaseMessaging.getInstance().sendEach(messages);

            int successCount = batchResponse.getSuccessCount();
            int failureCount = batchResponse.getFailureCount();

            // Traitement des réponses individuelles pour identifier les tokens invalides
            List<SendResponse> responses = batchResponse.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse response = responses.get(i);
                if (!response.isSuccessful() && isTokenError(response.getException())) {
                    // Token invalide, le marquer pour nettoyage
                    String invalidToken = tokens.get(i);
                    markTokenForCleanup(invalidToken);
                }
            }

            return BatchPushResult.builder()
                    .totalSent(tokens.size())
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur envoi batch: {}", e.getMessage(), e);

            return BatchPushResult.builder()
                    .totalSent(tokens.size())
                    .failureCount(tokens.size())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 🔍 Vérifie si un token FCM est valide
     */
    private boolean isTokenValid(String token) {
        try {
            // Test avec un message minimal
            Message testMessage = Message.builder()
                    .setToken(token)
                    .putData("test", "validation")
                    .build();

            FirebaseMessaging.getInstance().send(testMessage, true); // dry run
            return true;

        } catch (FirebaseMessagingException e) {
            return !isTokenError(e);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 🏷️ Détermine si l'erreur est liée à un token invalide
     */
    private boolean isTokenError(Exception e) {
        if (e instanceof FirebaseMessagingException) {
            FirebaseMessagingException fme = (FirebaseMessagingException) e;
            String errorCode = fme.getErrorCode();
            return "registration-token-not-registered".equals(errorCode) ||
                    "invalid-registration-token".equals(errorCode) ||
                    "mismatched-credential".equals(errorCode);
        }
        return false;
    }

    /**
     * 🔄 Détermine si l'erreur justifie un retry
     */
    private boolean shouldRetry(FirebaseMessagingException e) {
        String errorCode = e.getErrorCode();
        return "unavailable".equals(errorCode) ||
                "internal-error".equals(errorCode) ||
                "quota-exceeded".equals(errorCode);
    }

    /**
     * 📊 Met à jour les statistiques de notification
     */
    private void updateNotificationStats(CollecteurNotification notification, PushNotificationResult result) {
        // TODO: Implémenter la mise à jour des statistiques
        // Peut être stocké dans une table séparée ou Redis
    }

    /**
     * 🗑️ Marque un token pour nettoyage
     */
    private void markTokenForCleanup(String token) {
        // TODO: Implémenter le marquage pour nettoyage différé
    }

    /**
     * ⚙️ Obtient la priorité Android en fonction de la priorité de notification
     */
    private AndroidNotification.Priority getAndroidPriority(CollecteurNotification.Priorite priorite) {
        switch (priorite) {
            case URGENT:
                return AndroidNotification.Priority.HIGH;
            case HIGH:
                return AndroidNotification.Priority.DEFAULT_PRIORITY;
            case NORMAL:
                return AndroidNotification.Priority.DEFAULT_PRIORITY;
            case LOW:
                return AndroidNotification.Priority.MIN;
            default:
                return AndroidNotification.Priority.DEFAULT_PRIORITY;
        }
    }

    /**
     * 📱 Détermine le canal de notification Android
     */
    private String getNotificationChannel(CollecteurNotification.NotificationType type) {
        switch (type) {
            case SYSTEM_ALERT:
                return "system_alerts";
            case WARNING:
                return "warnings";
            case REMINDER:
                return "reminders";
            case ADMIN_MESSAGE:
                return "admin_messages";
            case SOLDE_ALERT:
                return "financial_alerts";
            default:
                return "general";
        }
    }

    /**
     * 📋 Divise une liste en sous-listes de taille fixe
     */
    private <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    // =====================================
    // CLASSES DE RÉSULTATS
    // =====================================

    @lombok.Data
    @lombok.Builder
    public static class PushNotificationResult {
        private boolean success;
        private String messageId;
        private String errorCode;
        private String errorMessage;
        private boolean tokenInvalid;
        private int attempts;

        public static PushNotificationResult failure(String errorMessage) {
            return PushNotificationResult.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .attempts(1)
                    .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchPushResult {
        private int totalRequested;
        private int totalSent;
        private int successCount;
        private int failureCount;
        private String errorMessage;

        public void merge(BatchPushResult other) {
            this.totalSent += other.totalSent;
            this.successCount += other.successCount;
            this.failureCount += other.failureCount;
        }
    }
}