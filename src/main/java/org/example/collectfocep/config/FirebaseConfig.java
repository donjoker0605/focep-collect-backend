package org.example.collectfocep.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔥 Configuration Firebase pour l'application FOCEP
 *
 * FONCTIONNALITÉS :
 * - Initialisation Firebase App avec les credentials
 * - Configuration FirebaseMessaging pour les notifications push
 * - Gestion des erreurs de configuration
 * - Support multi-environnements (dev, prod)
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.service-account-path}")
    private Resource serviceAccountResource;

    @Value("${app.firebase.database-url:}")
    private String databaseUrl;

    @Value("${app.firebase.project-id:}")
    private String projectId;

    private static final String FIREBASE_APP_NAME = "FOCEP-App";

    /**
     * 🚀 Initialise Firebase lors du démarrage de l'application
     */
    @PostConstruct
    public void initializeFirebase() {
        try {
            log.info("🔥 Initialisation Firebase...");

            // Vérifier si Firebase est déjà initialisé
            if (FirebaseApp.getApps().stream().anyMatch(app -> FIREBASE_APP_NAME.equals(app.getName()))) {
                log.info("✅ Firebase déjà initialisé");
                return;
            }

            // Charger les credentials depuis le fichier de service
            InputStream serviceAccount = serviceAccountResource.getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

            // Configuration Firebase
            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(credentials);

            // URL de base de données si spécifiée
            if (databaseUrl != null && !databaseUrl.trim().isEmpty()) {
                optionsBuilder.setDatabaseUrl(databaseUrl);
            }

            // ID du projet si spécifié
            if (projectId != null && !projectId.trim().isEmpty()) {
                optionsBuilder.setProjectId(projectId);
            }

            FirebaseOptions options = optionsBuilder.build();

            // Initialisation de l'app Firebase
            FirebaseApp firebaseApp = FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);

            log.info("✅ Firebase initialisé avec succès - App: {}, Projet: {}",
                    firebaseApp.getName(),
                    firebaseApp.getOptions().getProjectId());

        } catch (IOException e) {
            log.error("❌ Erreur lors du chargement du fichier de service Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de charger les credentials Firebase", e);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initialisation Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de l'initialisation Firebase", e);
        }
    }

    /**
     * 📤 Bean FirebaseMessaging pour l'injection de dépendances
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            FirebaseApp firebaseApp = FirebaseApp.getInstance(FIREBASE_APP_NAME);
            FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseApp);

            log.info("📤 Bean FirebaseMessaging créé avec succès");
            return messaging;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du bean FirebaseMessaging: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible de créer FirebaseMessaging", e);
        }
    }

    /**
     * 🔧 Bean pour la validation de configuration Firebase
     */
    @Bean
    public FirebaseConfigValidator firebaseConfigValidator() {
        return new FirebaseConfigValidator();
    }

    /**
     * ✅ Classe interne pour valider la configuration Firebase
     */
    public static class FirebaseConfigValidator {

        /**
         * Valide que Firebase est correctement configuré
         */
        public boolean validateConfiguration() {
            try {
                FirebaseApp app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
                FirebaseMessaging messaging = FirebaseMessaging.getInstance(app);

                // Test simple pour vérifier la connectivité
                // messaging.send() nécessiterait un message valide, on vérifie juste l'instance
                boolean isValid = app != null && messaging != null;

                log.info("🔍 Validation configuration Firebase: {}", isValid ? "SUCCÈS" : "ÉCHEC");
                return isValid;

            } catch (Exception e) {
                log.warn("⚠️ Validation configuration Firebase échouée: {}", e.getMessage());
                return false;
            }
        }

        /**
         * Retourne des informations sur la configuration Firebase
         */
        public FirebaseInfo getFirebaseInfo() {
            try {
                FirebaseApp app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
                return FirebaseInfo.builder()
                        .appName(app.getName())
                        .projectId(app.getOptions().getProjectId())
                        .databaseUrl(app.getOptions().getDatabaseUrl())
                        .isInitialized(true)
                        .build();

            } catch (Exception e) {
                return FirebaseInfo.builder()
                        .isInitialized(false)
                        .errorMessage(e.getMessage())
                        .build();
            }
        }
    }

    /**
     * 📊 Classe pour les informations Firebase
     */
    @lombok.Data
    @lombok.Builder
    public static class FirebaseInfo {
        private String appName;
        private String projectId;
        private String databaseUrl;
        private boolean isInitialized;
        private String errorMessage;
    }
}

/**
 * 🛡️ Configuration de sécurité pour les endpoints Firebase
 */
@Configuration
@Slf4j
class FirebaseSecurityConfig {

    /**
     * 🔐 Endpoint pour vérifier le statut Firebase (admin seulement)
     */
    @org.springframework.web.bind.annotation.RestController
    @org.springframework.web.bind.annotation.RequestMapping("/api/admin/firebase")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    static class FirebaseStatusController {

        private final FirebaseConfig.FirebaseConfigValidator validator;

        public FirebaseStatusController(FirebaseConfig.FirebaseConfigValidator validator) {
            this.validator = validator;
        }

        @org.springframework.web.bind.annotation.GetMapping("/status")
        public org.springframework.http.ResponseEntity<?> getFirebaseStatus() {
            try {
                FirebaseConfig.FirebaseInfo info = validator.getFirebaseInfo();
                boolean isValid = validator.validateConfiguration();

                Map<String, Object> response = new HashMap<>();
                response.put("status", isValid ? "OK" : "ERROR");
                response.put("firebase", info);
                response.put("timestamp", java.time.LocalDateTime.now());

                return org.springframework.http.ResponseEntity.ok(response);

            } catch (Exception e) {
                log.error("❌ Erreur lors de la vérification du statut Firebase: {}", e.getMessage(), e);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "ERROR");
                errorResponse.put("error", e.getMessage());
                errorResponse.put("timestamp", java.time.LocalDateTime.now());

                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            }
        }

        @org.springframework.web.bind.annotation.PostMapping("/test-notification")
        public org.springframework.http.ResponseEntity<?> testNotification(
                @org.springframework.web.bind.annotation.RequestParam String fcmToken,
                @org.springframework.web.bind.annotation.RequestParam(defaultValue = "Test FOCEP") String title,
                @org.springframework.web.bind.annotation.RequestParam(defaultValue = "Ceci est un test de notification") String message) {

            try {
                FirebaseMessaging messaging = FirebaseMessaging.getInstance(FirebaseApp.getInstance(FIREBASE_APP_NAME));

                com.google.firebase.messaging.Message testMessage = com.google.firebase.messaging.Message.builder()
                        .setToken(fcmToken)
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(message)
                                .build())
                        .putData("test", "true")
                        .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                        .build();

                String messageId = messaging.send(testMessage);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "SUCCESS");
                response.put("messageId", messageId);
                response.put("timestamp", java.time.LocalDateTime.now());

                log.info("✅ Notification de test envoyée - ID: {}", messageId);
                return org.springframework.http.ResponseEntity.ok(response);

            } catch (Exception e) {
                log.error("❌ Erreur lors de l'envoi de la notification de test: {}", e.getMessage(), e);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "ERROR");
                errorResponse.put("error", e.getMessage());
                errorResponse.put("timestamp", java.time.LocalDateTime.now());

                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorResponse);
            }
        }
    }
}

/**
 * 📊 Configuration des métriques Firebase pour Actuator
 */
@org.springframework.stereotype.Component
@lombok.RequiredArgsConstructor
class FirebaseHealthIndicator implements org.springframework.boot.actuator.health.HealthIndicator {

    private final FirebaseConfig.FirebaseConfigValidator validator;

    @Override
    public org.springframework.boot.actuator.health.Health health() {
        try {
            boolean isValid = validator.validateConfiguration();
            FirebaseConfig.FirebaseInfo info = validator.getFirebaseInfo();

            if (isValid) {
                return org.springframework.boot.actuator.health.Health.up()
                        .withDetail("firebase", info)
                        .withDetail("status", "Firebase opérationnel")
                        .build();
            } else {
                return org.springframework.boot.actuator.health.Health.down()
                        .withDetail("firebase", info)
                        .withDetail("status", "Firebase non opérationnel")
                        .build();
            }

        } catch (Exception e) {
            return org.springframework.boot.actuator.health.Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Erreur de vérification Firebase")
                    .build();
        }
    }
}