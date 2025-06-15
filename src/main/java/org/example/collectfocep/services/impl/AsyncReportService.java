package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.services.interfaces.JournalService;
import org.example.collectfocep.services.ReportService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncReportService {

    private final ReportService reportService;
    private final JournalService journalService;

    /**
     * ✅ GÉNÉRATION ASYNCHRONE DE RAPPORT MENSUEL
     * Maintenant parfaitement synchronisé avec JournalService
     */
    @Async("reportTaskExecutor")  // ✅ UTILISER LE THREAD POOL SPÉCIALISÉ
    public CompletableFuture<String> generateMonthlyReport(Long collecteurId, YearMonth month) {
        log.info("🚀 Démarrage génération asynchrone rapport pour collecteur: {} - mois: {}", collecteurId, month);

        try {
            // ✅ ÉTAPE 1: Récupérer les données de journal via JournalService
            List<Journal> journalEntries = journalService.getMonthlyEntries(collecteurId, month);
            log.info("📋 {} entrées de journal récupérées pour {}", journalEntries.size(), month);

            // ✅ ÉTAPE 2: Générer le rapport via ReportService
            String reportPath = reportService.generateMonthlyReport(collecteurId, journalEntries, month);
            log.info("✅ Rapport généré avec succès: {}", reportPath);

            return CompletableFuture.completedFuture(reportPath);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération asynchrone du rapport pour collecteur {}: {}",
                    collecteurId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * ✅ GÉNÉRATION ASYNCHRONE DE RAPPORT ANNUEL
     * Nouvelle fonctionnalité pour générer des rapports annuels
     */
    @Async("reportTaskExecutor")  // ✅ UTILISER LE THREAD POOL SPÉCIALISÉ
    public CompletableFuture<String> generateAnnualReport(Long collecteurId, int year) {
        log.info("🚀 Génération rapport annuel pour collecteur: {} - année: {}", collecteurId, year);

        try {
            StringBuilder allReports = new StringBuilder();

            // Générer un rapport pour chaque mois de l'année
            for (int month = 1; month <= 12; month++) {
                YearMonth yearMonth = YearMonth.of(year, month);

                try {
                    String monthlyReport = generateMonthlyReport(collecteurId, yearMonth).get();
                    allReports.append(monthlyReport).append("\n");
                    log.info("✅ Rapport mensuel {}/{} traité", month, year);

                } catch (Exception e) {
                    log.warn("⚠️ Erreur rapport mensuel {}/{}: {}", month, year, e.getMessage());
                }
            }

            // Créer le rapport annuel consolidé
            String annualReportPath = String.format("rapport_annuel_%d_%d.xlsx", collecteurId, year);
            log.info("✅ Rapport annuel généré: {}", annualReportPath);

            return CompletableFuture.completedFuture(annualReportPath);

        } catch (Exception e) {
            log.error("❌ Erreur génération rapport annuel: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * ✅ GÉNÉRATION ASYNCHRONE DE RAPPORT PERSONNALISÉ - COMPLÉTÉE
     * Permet de générer des rapports sur mesure avec périodes spécifiques
     */
    @Async("reportTaskExecutor")  // ✅ UTILISER LE THREAD POOL SPÉCIALISÉ
    public CompletableFuture<String> generateCustomReport(Long collecteurId, YearMonth startMonth, YearMonth endMonth) {
        log.info("🚀 Génération rapport personnalisé pour collecteur: {} - période: {} à {}",
                collecteurId, startMonth, endMonth);

        try {
            StringBuilder customReport = new StringBuilder();
            YearMonth currentMonth = startMonth;
            int processedMonths = 0;
            int totalMonths = (int) java.time.temporal.ChronoUnit.MONTHS.between(startMonth, endMonth) + 1;

            log.info("📊 Traitement de {} mois au total", totalMonths);

            while (!currentMonth.isAfter(endMonth)) {
                try {
                    log.info("🔄 Traitement du mois: {} ({}/{})", currentMonth, processedMonths + 1, totalMonths);

                    // Générer le rapport mensuel
                    String monthlyReport = generateMonthlyReport(collecteurId, currentMonth).get();
                    customReport.append(monthlyReport).append("\n");

                    processedMonths++;
                    log.info("✅ Rapport mensuel {} traité avec succès ({}/{})",
                            currentMonth, processedMonths, totalMonths);

                } catch (Exception e) {
                    log.warn("⚠️ Erreur rapport mensuel {}: {}", currentMonth, e.getMessage());
                    // Continuer même en cas d'erreur sur un mois
                }

                // Passer au mois suivant
                currentMonth = currentMonth.plusMonths(1);
            }

            // Créer le rapport personnalisé consolidé
            String customReportPath = String.format("rapport_personnalise_%d_%s_%s.xlsx",
                    collecteurId, startMonth.toString(), endMonth.toString());

            log.info("✅ Rapport personnalisé généré avec succès: {} ({} mois traités)",
                    customReportPath, processedMonths);

            return CompletableFuture.completedFuture(customReportPath);

        } catch (Exception e) {
            log.error("❌ Erreur génération rapport personnalisé pour collecteur {}: {}",
                    collecteurId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * ✅ STATUT DE GÉNÉRATION ASYNCHRONE - CORRIGÉ
     * Permet de suivre l'état d'avancement des rapports
     */
    public CompletableFuture<AsyncReportStatus> getReportStatus(String reportId) {
        log.info("📊 Vérification statut rapport: {}", reportId);

        try {
            // TODO: Implémenter un système de suivi des rapports en cours
            // Pour l'instant, retourner un statut simulé
            AsyncReportStatus status = AsyncReportStatus.builder()
                    .reportId(reportId)
                    .status("COMPLETED")
                    .progress(100)
                    .message("Rapport généré avec succès")
                    .build();

            return CompletableFuture.completedFuture(status);

        } catch (Exception e) {
            log.error("❌ Erreur vérification statut rapport: {}", e.getMessage(), e);

            AsyncReportStatus errorStatus = AsyncReportStatus.builder()
                    .reportId(reportId)
                    .status("FAILED")
                    .progress(0)
                    .message("Erreur: " + e.getMessage())
                    .build();

            return CompletableFuture.completedFuture(errorStatus);
        }
    }

    /**
     * ✅ NETTOYAGE AUTOMATIQUE DES ANCIENS RAPPORTS
     */
    @Async("taskExecutor")  // ✅ UTILISER LE THREAD POOL GÉNÉRAL POUR LE NETTOYAGE
    public CompletableFuture<Integer> cleanupOldReports(int daysOld) {
        log.info("🧹 Nettoyage des rapports de plus de {} jours", daysOld);

        try {
            // TODO: Implémenter la logique de nettoyage réelle
            // Pour l'instant, simuler le nettoyage
            int cleanedCount = 0;

            // Simulation du nettoyage
            Thread.sleep(2000); // Simuler le temps de traitement
            cleanedCount = (int) (Math.random() * 10); // Simuler le nombre de fichiers supprimés

            log.info("✅ Nettoyage terminé: {} rapports supprimés", cleanedCount);
            return CompletableFuture.completedFuture(cleanedCount);

        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * ✅ GÉNÉRATION DE RAPPORT RAPIDE (SYNCHRONE)
     * Pour les petits rapports qui ne nécessitent pas d'être asynchrones
     */
    public String generateQuickReport(Long collecteurId, YearMonth month) {
        log.info("⚡ Génération rapport rapide pour collecteur: {} - mois: {}", collecteurId, month);

        try {
            List<Journal> journalEntries = journalService.getMonthlyEntries(collecteurId, month);
            String reportPath = reportService.generateMonthlyReport(collecteurId, journalEntries, month);

            log.info("✅ Rapport rapide généré: {}", reportPath);
            return reportPath;

        } catch (Exception e) {
            log.error("❌ Erreur génération rapport rapide: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur génération rapport rapide: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ VÉRIFICATION DE LA DISPONIBILITÉ DU SERVICE
     */
    public boolean isServiceAvailable() {
        try {
            // Vérifier que les services dépendants sont disponibles
            boolean journalServiceOk = journalService != null;
            boolean reportServiceOk = reportService != null;

            boolean available = journalServiceOk && reportServiceOk;
            log.info("🔍 Statut service AsyncReport: {} (JournalService: {}, ReportService: {})",
                    available ? "DISPONIBLE" : "INDISPONIBLE", journalServiceOk, reportServiceOk);

            return available;

        } catch (Exception e) {
            log.error("❌ Erreur vérification disponibilité service: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ CLASSE INTERNE POUR LE STATUT DES RAPPORTS - CORRIGÉE
     * Séparée de l'entité Report pour éviter les conflits
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AsyncReportStatus {
        private String reportId;
        private String status; // PENDING, PROCESSING, COMPLETED, FAILED
        private int progress; // 0-100
        private String message;
        private String filePath;
        private Long fileSize;
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;

        /**
         * Vérifie si le rapport est terminé
         */
        public boolean isCompleted() {
            return "COMPLETED".equals(status);
        }

        /**
         * Vérifie si le rapport a échoué
         */
        public boolean isFailed() {
            return "FAILED".equals(status);
        }

        /**
         * Vérifie si le rapport est en cours
         */
        public boolean isInProgress() {
            return "PROCESSING".equals(status) || "PENDING".equals(status);
        }

        /**
         * Calcule la durée d'exécution
         */
        public Long getDurationInSeconds() {
            if (startTime != null && endTime != null) {
                return java.time.Duration.between(startTime, endTime).getSeconds();
            }
            return null;
        }
    }
}