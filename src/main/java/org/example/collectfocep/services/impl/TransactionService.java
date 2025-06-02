package org.example.collectfocep.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.services.interfaces.DateTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TransactionService {

    private final TransactionTemplate transactionTemplate;
    private final DateTimeService dateTimeService;

    @Autowired
    public TransactionService(
            PlatformTransactionManager transactionManager,
            DateTimeService dateTimeService) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.dateTimeService = dateTimeService;
    }

    /**
     * ✅ CORRECTION: Exécute une action dans une transaction avec logging amélioré
     */
    public <T> T executeInTransaction(TransactionCallback<T> action) {
        LocalDateTime startTime = dateTimeService.getCurrentDateTime();
        String transactionId = generateTransactionId();

        log.info("🔄 [{}] Début transaction à {}", transactionId, startTime);

        try {
            T result = transactionTemplate.execute(action);

            LocalDateTime endTime = dateTimeService.getCurrentDateTime();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            log.info("✅ [{}] Transaction réussie en {}ms (fin: {})",
                    transactionId, durationMs, endTime);

            return result;
        } catch (Exception e) {
            LocalDateTime errorTime = dateTimeService.getCurrentDateTime();
            long durationMs = java.time.Duration.between(startTime, errorTime).toMillis();

            log.error("❌ [{}] Erreur transaction après {}ms: {}",
                    transactionId, durationMs, e.getMessage(), e);
            throw e;
        }
        // ✅ SUPPRESSION du finally qui référençait originalPropagation non déclarée
    }

    /**
     * ✅ NOUVELLE MÉTHODE: Exécute une action dans une nouvelle transaction
     */
    public <T> T executeInNewTransaction(TransactionCallback<T> action) {
        LocalDateTime startTime = dateTimeService.getCurrentDateTime();
        String transactionId = generateTransactionId() + "-NEW";

        log.info("🔄 [{}] Début nouvelle transaction à {}", transactionId, startTime);

        int originalPropagation = transactionTemplate.getPropagationBehavior();
        try {
            // Configurer pour une nouvelle transaction
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            T result = transactionTemplate.execute(action);

            LocalDateTime endTime = dateTimeService.getCurrentDateTime();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            log.info("✅ [{}] Nouvelle transaction réussie en {}ms (fin: {})",
                    transactionId, durationMs, endTime);

            return result;
        } catch (Exception e) {
            LocalDateTime errorTime = dateTimeService.getCurrentDateTime();
            long durationMs = java.time.Duration.between(startTime, errorTime).toMillis();

            log.error("❌ [{}] Erreur nouvelle transaction après {}ms: {}",
                    transactionId, durationMs, e.getMessage(), e);
            throw e;
        } finally {
            // Restaurer la propagation d'origine
            transactionTemplate.setPropagationBehavior(originalPropagation);
        }
    }

    /**
     * ✅ MÉTHODE: Exécute une transaction avec timeout personnalisé
     */
    public <T> T executeInTransactionWithTimeout(TransactionCallback<T> action, int timeoutSeconds) {
        LocalDateTime startTime = dateTimeService.getCurrentDateTime();
        String transactionId = generateTransactionId() + "-TIMEOUT";

        log.info("🔄 [{}] Début transaction avec timeout {}s à {}",
                transactionId, timeoutSeconds, startTime);

        int originalTimeout = transactionTemplate.getTimeout();
        try {
            transactionTemplate.setTimeout(timeoutSeconds);

            T result = transactionTemplate.execute(action);

            LocalDateTime endTime = dateTimeService.getCurrentDateTime();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            log.info("✅ [{}] Transaction avec timeout réussie en {}ms (fin: {})",
                    transactionId, durationMs, endTime);

            return result;
        } catch (Exception e) {
            LocalDateTime errorTime = dateTimeService.getCurrentDateTime();
            long durationMs = java.time.Duration.between(startTime, errorTime).toMillis();

            log.error("❌ [{}] Erreur transaction avec timeout après {}ms: {}",
                    transactionId, durationMs, e.getMessage(), e);
            throw e;
        } finally {
            // Restaurer le timeout d'origine
            transactionTemplate.setTimeout(originalTimeout);
        }
    }

    /**
     * ✅ MÉTHODE: Exécute une transaction en lecture seule
     */
    public <T> T executeInReadOnlyTransaction(TransactionCallback<T> action) {
        LocalDateTime startTime = dateTimeService.getCurrentDateTime();
        String transactionId = generateTransactionId() + "-RO";

        log.info("🔄 [{}] Début transaction lecture seule à {}", transactionId, startTime);

        boolean originalReadOnly = transactionTemplate.isReadOnly();
        try {
            transactionTemplate.setReadOnly(true);

            T result = transactionTemplate.execute(action);

            LocalDateTime endTime = dateTimeService.getCurrentDateTime();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            log.info("✅ [{}] Transaction lecture seule réussie en {}ms (fin: {})",
                    transactionId, durationMs, endTime);

            return result;
        } catch (Exception e) {
            LocalDateTime errorTime = dateTimeService.getCurrentDateTime();
            long durationMs = java.time.Duration.between(startTime, errorTime).toMillis();

            log.error("❌ [{}] Erreur transaction lecture seule après {}ms: {}",
                    transactionId, durationMs, e.getMessage(), e);
            throw e;
        } finally {
            // Restaurer le mode lecture/écriture d'origine
            transactionTemplate.setReadOnly(originalReadOnly);
        }
    }

    /**
     * Vérifie si une transaction est active
     */
    public boolean isTransactionActive() {
        try {
            return org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive();
        } catch (Exception e) {
            log.debug("Erreur lors de la vérification du statut de transaction: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtient le nom de la transaction courante
     */
    public String getCurrentTransactionName() {
        try {
            return org.springframework.transaction.support.TransactionSynchronizationManager.getCurrentTransactionName();
        } catch (Exception e) {
            log.debug("Erreur lors de la récupération du nom de transaction: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * Log des informations de transaction
     */
    public void logTransactionInfo() {
        try {
            boolean isActive = isTransactionActive();
            String name = getCurrentTransactionName();
            boolean isReadOnly = org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly();

            log.info("📊 Transaction Info - Active: {}, Nom: {}, Lecture seule: {}, Timestamp: {}",
                    isActive, name, isReadOnly, dateTimeService.getCurrentDateTime());
        } catch (Exception e) {
            log.debug("Erreur lors du logging des infos de transaction: {}", e.getMessage());
        }
    }

    /**
     * Génère un ID unique pour le suivi des transactions
     */
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" +
                Thread.currentThread().getId() + "-" +
                Integer.toHexString(System.identityHashCode(this)).substring(0, 4).toUpperCase();
    }

    /**
     * Configure le timeout par défaut des transactions
     */
    public void setDefaultTimeout(int timeoutSeconds) {
        transactionTemplate.setTimeout(timeoutSeconds);
        log.info("⚙️ Timeout de transaction configuré à {}s à {}",
                timeoutSeconds, dateTimeService.getCurrentDateTime());
    }

    /**
     * Configure le niveau d'isolation par défaut
     */
    public void setDefaultIsolationLevel(int isolationLevel) {
        transactionTemplate.setIsolationLevel(isolationLevel);
        log.info("⚙️ Niveau d'isolation configuré à {} à {}",
                isolationLevel, dateTimeService.getCurrentDateTime());
    }

    /**
     * Configure le comportement de propagation par défaut
     */
    public void setDefaultPropagationBehavior(int propagationBehavior) {
        transactionTemplate.setPropagationBehavior(propagationBehavior);
        log.info("⚙️ Comportement de propagation configuré à {} à {}",
                propagationBehavior, dateTimeService.getCurrentDateTime());
    }

    /**
     * Affiche les statistiques d'utilisation des transactions
     */
    public void logTransactionStats() {
        log.info("📈 Stats Transaction à {} - Template: {}",
                dateTimeService.getCurrentDateTime(), transactionTemplate.toString());
    }

    /**
     * Teste la connectivité de la base de données dans une transaction
     */
    public boolean testDatabaseConnectivity() {
        try {
            return executeInReadOnlyTransaction(status -> {
                log.info("🔍 Test connectivité DB à {}", dateTimeService.getCurrentDateTime());
                // Le simple fait d'exécuter cette transaction teste la connectivité
                return true;
            });
        } catch (Exception e) {
            log.error("❌ Échec test connectivité DB à {}: {}",
                    dateTimeService.getCurrentDateTime(), e.getMessage());
            return false;
        }
    }
}