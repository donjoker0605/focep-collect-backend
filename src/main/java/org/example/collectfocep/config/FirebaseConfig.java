package org.example.collectfocep.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;

/**
 * 🔥 Configuration Firebase pour l'application FOCEP
 *
 * STRATÉGIE PROGRESSIVE :
 * - Phase 1-2 : Firebase DÉSACTIVÉ (développement rapide)
 * - Phase 3+ : Firebase ACTIVÉ (notifications temps réel)
 *
 * ACTIVATION FUTURE :
 * - Changer app.firebase.enabled=true dans application.properties
 * - Ajouter firebase-service-account.json dans resources/
 * - Configurer les propriétés Firebase
 */
@Configuration
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() {
        log.info("🔥 Firebase sera initialisé en Phase 3+ pour les notifications temps réel");
        // Configuration Firebase complète sera ajoutée en Phase 3
    }
}

/**
 * 🔧 Configuration par défaut pour les phases sans Firebase
 */
@Configuration
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
class FirebaseDisabledConfig {

    @PostConstruct
    public void logFirebaseDisabled() {
        log.info("🔥 Firebase DÉSACTIVÉ - Développement Phase 1-2");
        log.info("📋 Fonctionnalités disponibles : Journal d'activité, Géolocalisation, Validation solde");
        log.info("🚀 Firebase sera activé en Phase 3 pour les notifications temps réel");
    }

    /**
     * 📱 Service de notifications par défaut (no-op pour développement)
     */
    @Bean
    public NotificationService notificationService() {
        return new NotificationService() {
            @Override
            public void sendNotification(String message) {
                log.debug("🔔 Notification simulée : {}", message);
            }
        };
    }

    public interface NotificationService {
        void sendNotification(String message);
    }
}