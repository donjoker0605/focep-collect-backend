package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.services.impl.AsyncReportService;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/async-reports")
@Slf4j
@RequiredArgsConstructor
public class AsyncReportController {

    private final AsyncReportService asyncReportService;
    private final SecurityService securityService;

    /**
     * ✅ GÉNÉRATION ASYNCHRONE DE RAPPORT MENSUEL
     */
    @PostMapping("/monthly")
    @PreAuthorize("@securityService.canAccessCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<String>> generateMonthlyReportAsync(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {

        log.info("🚀 Demande génération rapport mensuel asynchrone - Collecteur: {}, Mois: {}", collecteurId, month);

        try {
            // ✅ VÉRIFICATION DE SÉCURITÉ
            Long currentUserAgenceId = securityService.getCurrentUserAgenceId();
            if (currentUserAgenceId == null) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            // ✅ VÉRIFIER LA DISPONIBILITÉ DU SERVICE
            if (!asyncReportService.isServiceAvailable()) {
                return ResponseEntity.status(503)
                        .body(ApiResponse.error("Service de génération de rapports temporairement indisponible"));
            }

            // ✅ GÉNÉRER UN ID DE SUIVI UNIQUE
            String trackingId = UUID.randomUUID().toString();

            // ✅ DÉMARRER LA GÉNÉRATION ASYNCHRONE
            CompletableFuture<String> futureReport = asyncReportService.generateMonthlyReport(collecteurId, month);

            // ✅ GÉRER LE RÉSULTAT DE MANIÈRE ASYNCHRONE
            futureReport.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("❌ Erreur génération rapport {}: {}", trackingId, throwable.getMessage());
                } else {
                    log.info("✅ Rapport {} généré avec succès: {}", trackingId, result);
                }
            });

            // ✅ RETOURNER IMMÉDIATEMENT AVEC L'ID DE SUIVI
            return ResponseEntity.accepted()
                    .body(ApiResponse.success(trackingId, "Génération démarrée. Utilisez l'ID pour suivre le progrès."));

        } catch (Exception e) {
            log.error("❌ Erreur démarrage génération rapport mensuel", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors du démarrage de la génération: " + e.getMessage()));
        }
    }

    /**
     * ✅ GÉNÉRATION ASYNCHRONE DE RAPPORT ANNUEL
     */
    @PostMapping("/annual")
    @PreAuthorize("@securityService.canAccessCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<String>> generateAnnualReportAsync(
            @RequestParam Long collecteurId,
            @RequestParam int year) {

        log.info("🚀 Demande génération rapport annuel asynchrone - Collecteur: {}, Année: {}", collecteurId, year);

        try {
            // ✅ VALIDATION DE L'ANNÉE
            int currentYear = java.time.Year.now().getValue();
            if (year < 2020 || year > currentYear + 1) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Année invalide: " + year + " (autorisée: 2020-" + (currentYear + 1) + ")"));
            }

            String trackingId = UUID.randomUUID().toString();

            // ✅ DÉMARRER LA GÉNÉRATION ASYNCHRONE
            CompletableFuture<String> futureReport = asyncReportService.generateAnnualReport(collecteurId, year);

            futureReport.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("❌ Erreur génération rapport annuel {}: {}", trackingId, throwable.getMessage());
                } else {
                    log.info("✅ Rapport annuel {} généré: {}", trackingId, result);
                }
            });

            return ResponseEntity.accepted()
                    .body(ApiResponse.success(trackingId,
                            String.format("Génération rapport annuel %d démarrée (12 mois)", year)));

        } catch (Exception e) {
            log.error("❌ Erreur démarrage génération rapport annuel", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * ✅ GÉNÉRATION ASYNCHRONE DE RAPPORT PERSONNALISÉ
     */
    @PostMapping("/custom")
    @PreAuthorize("@securityService.canAccessCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<String>> generateCustomReportAsync(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth) {

        log.info("🚀 Génération rapport personnalisé - Collecteur: {}, Période: {} à {}",
                collecteurId, startMonth, endMonth);

        try {
            // ✅ VALIDATION DE LA PÉRIODE
            if (startMonth.isAfter(endMonth)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("La date de début doit être antérieure à la date de fin"));
            }

            // ✅ LIMITATION DE LA PÉRIODE (max 24 mois)
            long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(startMonth, endMonth);
            if (monthsBetween > 24) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Période trop longue (maximum 24 mois, demandé: " + (monthsBetween + 1) + " mois)"));
            }

            String trackingId = UUID.randomUUID().toString();

            // ✅ DÉMARRER LA GÉNÉRATION ASYNCHRONE
            CompletableFuture<String> futureReport = asyncReportService.generateCustomReport(
                    collecteurId, startMonth, endMonth);

            futureReport.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("❌ Erreur génération rapport personnalisé {}: {}", trackingId, throwable.getMessage());
                } else {
                    log.info("✅ Rapport personnalisé {} généré: {}", trackingId, result);
                }
            });

            return ResponseEntity.accepted()
                    .body(ApiResponse.success(trackingId,
                            String.format("Génération rapport personnalisé démarrée (%d mois)", monthsBetween + 1)));

        } catch (Exception e) {
            log.error("❌ Erreur démarrage génération rapport personnalisé", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * ✅ GÉNÉRATION SYNCHRONE RAPIDE (pour petits rapports)
     */
    @PostMapping("/quick")
    @PreAuthorize("@securityService.canAccessCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<String>> generateQuickReport(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {

        log.info("⚡ Génération rapport rapide - Collecteur: {}, Mois: {}", collecteurId, month);

        try {
            // ✅ GÉNÉRER LE RAPPORT DIRECTEMENT (SYNCHRONE)
            String reportPath = asyncReportService.generateQuickReport(collecteurId, month);

            return ResponseEntity.ok()
                    .body(ApiResponse.success(reportPath, "Rapport généré immédiatement"));

        } catch (Exception e) {
            log.error("❌ Erreur génération rapport rapide", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur génération rapport rapide: " + e.getMessage()));
        }
    }

    /**
     * ✅ VÉRIFICATION DU STATUT D'UN RAPPORT - CORRIGÉ
     */
    @GetMapping("/status/{trackingId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<AsyncReportService.AsyncReportStatus>> getReportStatus(
            @PathVariable String trackingId) {

        log.info("📊 Vérification statut rapport: {}", trackingId);

        try {
            // ✅ UTILISER LA BONNE CLASSE AsyncReportStatus
            CompletableFuture<AsyncReportService.AsyncReportStatus> futureStatus =
                    asyncReportService.getReportStatus(trackingId);

            AsyncReportService.AsyncReportStatus status = futureStatus.get();

            return ResponseEntity.ok(ApiResponse.success(status, "Statut récupéré"));

        } catch (Exception e) {
            log.error("❌ Erreur récupération statut rapport {}: {}", trackingId, e.getMessage());

            // ✅ CRÉER UN STATUT D'ERREUR
            AsyncReportService.AsyncReportStatus errorStatus = AsyncReportService.AsyncReportStatus.builder()
                    .reportId(trackingId)
                    .status("ERROR")
                    .progress(0)
                    .message("Erreur lors de la récupération du statut: " + e.getMessage())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(errorStatus, "Statut d'erreur"));
        }
    }

    /**
     * ✅ NETTOYAGE DES ANCIENS RAPPORTS
     */
    @PostMapping("/cleanup")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> cleanupOldReports(
            @RequestParam(defaultValue = "30") int daysOld) {

        log.info("🧹 Demande nettoyage rapports de plus de {} jours", daysOld);

        try {
            if (daysOld < 7) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Minimum 7 jours pour le nettoyage (sécurité)"));
            }

            String cleanupId = UUID.randomUUID().toString();

            // ✅ DÉMARRER LE NETTOYAGE ASYNCHRONE
            CompletableFuture<Integer> futureCleanup = asyncReportService.cleanupOldReports(daysOld);

            futureCleanup.whenComplete((cleanedCount, throwable) -> {
                if (throwable != null) {
                    log.error("❌ Erreur nettoyage {}: {}", cleanupId, throwable.getMessage());
                } else {
                    log.info("✅ Nettoyage {} terminé: {} rapports supprimés", cleanupId, cleanedCount);
                }
            });

            return ResponseEntity.accepted()
                    .body(ApiResponse.success(cleanupId,
                            String.format("Nettoyage des rapports > %d jours démarré", daysOld)));

        } catch (Exception e) {
            log.error("❌ Erreur démarrage nettoyage", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur nettoyage: " + e.getMessage()));
        }
    }

    /**
     * ✅ VÉRIFICATION DE LA SANTÉ DU SERVICE
     */
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> checkServiceHealth() {
        log.info("🔍 Vérification santé du service AsyncReport");

        try {
            boolean available = asyncReportService.isServiceAvailable();

            if (available) {
                return ResponseEntity.ok()
                        .body(ApiResponse.success("HEALTHY", "Service de génération de rapports opérationnel"));
            } else {
                return ResponseEntity.status(503)
                        .body(ApiResponse.error("Service de génération de rapports indisponible"));
            }

        } catch (Exception e) {
            log.error("❌ Erreur vérification santé service", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur vérification: " + e.getMessage()));
        }
    }

    /**
     * ✅ LISTE DES RAPPORTS EN COURS - AMÉLIORÉE
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> getActiveReports() {
        log.info("📋 Récupération des rapports en cours");

        try {
            // TODO: Implémenter la récupération des rapports actifs
            String message = "Fonctionnalité de suivi des rapports actifs en développement.\n" +
                    "Service disponible: " + asyncReportService.isServiceAvailable();

            return ResponseEntity.ok(ApiResponse.success(message));

        } catch (Exception e) {
            log.error("❌ Erreur récupération rapports actifs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }
}