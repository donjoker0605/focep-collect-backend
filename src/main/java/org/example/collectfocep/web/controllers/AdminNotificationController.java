package org.example.collectfocep.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.AdminNotification;
import org.example.collectfocep.entities.enums.NotificationType;
import org.example.collectfocep.entities.enums.Priority;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.impl.AdminNotificationService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Notifications", description = "Gestion des notifications administrateur")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;
    private final SecurityService securityService;

    private Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return securityService.getCurrentUserId(authentication);
    }

    // =====================================
    // ENDPOINTS DASHBOARD
    // =====================================

    /**
     * 📊 Dashboard principal des notifications
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard des notifications", description = "Récupère le résumé des notifications pour l'admin connecté")
    public ResponseEntity<ApiResponse<NotificationDashboardDTO>> getDashboard() {
        try {
            Long adminId = getCurrentAdminId();
            log.info("📊 Dashboard notifications: adminId={}", adminId);

            NotificationDashboardDTO dashboard = adminNotificationService.getDashboard(adminId);

            return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard récupéré avec succès"));

        } catch (Exception e) {
            log.error("❌ Erreur dashboard notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération du dashboard"));
        }
    }

    /**
     * 📋 Liste des notifications avec pagination
     */
    @GetMapping
    @Operation(summary = "Liste des notifications", description = "Récupère toutes les notifications avec pagination")
    public ResponseEntity<ApiResponse<Page<AdminNotificationDTO>>> getAllNotifications(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") @Min(1) int size,
            @Parameter(description = "Tri") @RequestParam(defaultValue = "dateCreation") String sort,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Filtre par type") @RequestParam(required = false) NotificationType type,
            @Parameter(description = "Filtre par priorité") @RequestParam(required = false) Priority priority,
            @Parameter(description = "Filtre non lues seulement") @RequestParam(defaultValue = "false") boolean unreadOnly) {

        try {
            Long adminId = getCurrentAdminId();
            log.debug("📋 Liste notifications: adminId={}, page={}, size={}", adminId, page, size);

            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<AdminNotificationDTO> notifications = adminNotificationService.getAllNotifications(
                    adminId, type, priority, unreadOnly, pageable);

            return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications récupérées avec succès"));

        } catch (Exception e) {
            log.error("❌ Erreur récupération notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des notifications"));
        }
    }

    /**
     * 🚨 Notifications critiques non lues
     */
    @GetMapping("/critical")
    @Operation(summary = "Notifications critiques", description = "Récupère uniquement les notifications critiques non lues")
    public ResponseEntity<ApiResponse<List<AdminNotification>>> getCriticalNotifications() {
        try {
            Long adminId = getCurrentAdminId();
            
            if (adminId == null) {
                log.warn("⚠️ Admin ID non trouvé dans le contexte de sécurité");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("UNAUTHORIZED", "Utilisateur non authentifié"));
            }
            
            log.info("🚨 Notifications critiques: adminId={}", adminId);

            List<AdminNotification> notifications = adminNotificationService.getCriticalNotifications(adminId);
            
            if (notifications == null) {
                notifications = Collections.emptyList();
            }

            return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications critiques récupérées"));

        } catch (Exception e) {
            log.error("❌ Erreur notifications critiques: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CRITICAL_FETCH_ERROR", "Erreur lors de la récupération des notifications critiques"));
        }
    }

    /**
     * 🔢 Statistiques des notifications
     */
    @GetMapping("/stats")
    @Operation(summary = "Statistiques", description = "Récupère les statistiques des notifications")
    public ResponseEntity<ApiResponse<NotificationStatsDTO>> getStats() {
        try {
            Long adminId = getCurrentAdminId();
            log.debug("🔢 Stats notifications: adminId={}", adminId);

            NotificationStatsDTO stats = adminNotificationService.getStats(adminId);

            return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques récupérées"));

        } catch (Exception e) {
            log.error("❌ Erreur stats notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des statistiques"));
        }
    }

    // =====================================
    // ACTIONS SUR NOTIFICATIONS
    // =====================================

    /**
     * Marquer une notification comme lue
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "Marquer comme lue", description = "Marque une notification comme lue")
    public ResponseEntity<ApiResponse<String>> markAsRead(@Parameter(description = "ID de la notification") @PathVariable @NotNull Long id) {
        try {
            Long adminId = getCurrentAdminId();
            log.info("✅ Marquer notification comme lue: notificationId={}, adminId={}", id, adminId);

            boolean success = adminNotificationService.markAsRead(id, adminId);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("OK", "Notification marquée comme lue"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Notification non trouvée ou accès refusé"));
            }

        } catch (Exception e) {
            log.error("❌ Erreur marquage lecture: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors du marquage de la notification"));
        }
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    @PutMapping("/read-all")
    @Operation(summary = "Marquer toutes comme lues", description = "Marque toutes les notifications comme lues")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        try {
            Long adminId = getCurrentAdminId();
            log.info("✅ Marquer toutes notifications comme lues: adminId={}", adminId);

            int count = adminNotificationService.markAllAsRead(adminId);

            return ResponseEntity.ok(ApiResponse.success("OK",
                    String.format("%d notifications marquées comme lues", count)));

        } catch (Exception e) {
            log.error("❌ Erreur marquage global: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors du marquage global"));
        }
    }

    /**
     * 🗑️ Supprimer une notification
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer notification", description = "Supprime une notification")
    public ResponseEntity<ApiResponse<String>> deleteNotification(@Parameter(description = "ID de la notification") @PathVariable @NotNull Long id) {
        try {
            Long adminId = getCurrentAdminId();
            log.info("🗑️ Supprimer notification: notificationId={}, adminId={}", id, adminId);

            boolean success = adminNotificationService.deleteNotification(id, adminId);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("OK", "Notification supprimée"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Notification non trouvée ou accès refusé"));
            }

        } catch (Exception e) {
            log.error("❌ Erreur suppression notification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la suppression"));
        }
    }

    // =====================================
    // COMPTEURS ET MÉTRIQUES
    // =====================================

    /**
     * 🔢 Compter notifications non lues
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Nombre non lues", description = "Récupère le nombre de notifications non lues")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        try {
            Long adminId = getCurrentAdminId();
            log.debug("🔢 Comptage notifications non lues: adminId={}", adminId);

            Long count = adminNotificationService.getUnreadCount(adminId);

            return ResponseEntity.ok(ApiResponse.success(count, "Nombre de notifications non lues"));

        } catch (Exception e) {
            log.error("❌ Erreur comptage notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du comptage"));
        }
    }

    /**
     * 🔢 Compter notifications critiques
     */
    @GetMapping("/critical-count")
    @Operation(summary = "Nombre critiques", description = "Récupère le nombre de notifications critiques non lues")
    public ResponseEntity<ApiResponse<Long>> getCriticalCount() {
        try {
            Long adminId = getCurrentAdminId();
            log.debug("🔢 Comptage notifications critiques: adminId={}", adminId);

            Long count = adminNotificationService.getCriticalCount(adminId);

            return ResponseEntity.ok(ApiResponse.success(count, "Nombre de notifications critiques"));

        } catch (Exception e) {
            log.error("❌ Erreur comptage critiques: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du comptage"));
        }
    }

    // =====================================
    // FILTRES ET RECHERCHE
    // =====================================

    /**
     * 🔍 Notifications par collecteur
     */
    @GetMapping("/collecteur/{collecteurId}")
    @Operation(summary = "Notifications par collecteur", description = "Récupère les notifications d'un collecteur spécifique")
    public ResponseEntity<ApiResponse<List<AdminNotificationDTO>>> getNotificationsByCollecteur(
            @Parameter(description = "ID du collecteur") @PathVariable @NotNull Long collecteurId) {

        try {
            Long adminId = getCurrentAdminId();
            log.debug("🔍 Notifications par collecteur: adminId={}, collecteurId={}", adminId, collecteurId);

            List<AdminNotificationDTO> notifications = adminNotificationService.getNotificationsByCollecteur(adminId, collecteurId);

            return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications du collecteur récupérées"));

        } catch (Exception e) {
            log.error("❌ Erreur notifications collecteur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération"));
        }
    }

    /**
     * 🔍 Notifications par période
     */
    @GetMapping("/period")
    @Operation(summary = "Notifications par période", description = "Récupère les notifications d'une période donnée")
    public ResponseEntity<ApiResponse<List<AdminNotificationDTO>>> getNotificationsByPeriod(
            @Parameter(description = "Date de début (ISO)") @RequestParam String dateDebut,
            @Parameter(description = "Date de fin (ISO)") @RequestParam String dateFin) {

        try {
            Long adminId = getCurrentAdminId();
            LocalDateTime debut = LocalDateTime.parse(dateDebut);
            LocalDateTime fin = LocalDateTime.parse(dateFin);

            log.debug("🔍 Notifications par période: adminId={}, debut={}, fin={}", adminId, debut, fin);

            List<AdminNotificationDTO> notifications = adminNotificationService.getNotificationsByPeriod(adminId, debut, fin);

            return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications de la période récupérées"));

        } catch (Exception e) {
            log.error("❌ Erreur notifications période: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la récupération ou format de date invalide"));
        }
    }

    // =====================================
    // CONFIGURATION
    // =====================================

    /**
     * ⚙️ Récupérer configuration des notifications
     */
    @GetMapping("/settings")
    @Operation(summary = "Configuration", description = "Récupère la configuration des notifications")
    public ResponseEntity<ApiResponse<List<NotificationSettingsDTO>>> getSettings() {
        try {
            Long adminId = getCurrentAdminId();
            log.debug("⚙️ Configuration notifications: adminId={}", adminId);

            List<NotificationSettingsDTO> settings = adminNotificationService.getSettings(adminId);

            return ResponseEntity.ok(ApiResponse.success(settings, "Configuration récupérée"));

        } catch (Exception e) {
            log.error("❌ Erreur récupération config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération de la configuration"));
        }
    }

    /**
     * ⚙️ Mettre à jour configuration
     */
    @PutMapping("/settings")
    @Operation(summary = "Mettre à jour configuration", description = "Met à jour la configuration des notifications")
    public ResponseEntity<ApiResponse<String>> updateSettings(@Valid @RequestBody List<NotificationSettingsDTO> settings) {
        try {
            Long adminId = getCurrentAdminId();
            log.info("⚙️ Mise à jour config notifications: adminId={}", adminId);

            adminNotificationService.updateSettings(adminId, settings);

            return ResponseEntity.ok(ApiResponse.success("OK", "Configuration mise à jour"));

        } catch (Exception e) {
            log.error("❌ Erreur mise à jour config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la mise à jour de la configuration"));
        }
    }

    // =====================================
    // ACTIONS ADMIN AVANCÉES
    // =====================================

    /**
     * 📧 Renvoyer email pour notification critique
     */
    @PostMapping("/{id}/resend-email")
    @Operation(summary = "Renvoyer email", description = "Renvoie l'email pour une notification critique")
    public ResponseEntity<ApiResponse<String>> resendEmail(@Parameter(description = "ID de la notification") @PathVariable @NotNull Long id) {
        try {
            Long adminId = getCurrentAdminId();
            log.info("📧 Renvoyer email notification: notificationId={}, adminId={}", id, adminId);

            boolean success = adminNotificationService.resendEmail(id, adminId);

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("OK", "Email renvoyé"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Impossible de renvoyer l'email"));
            }

        } catch (Exception e) {
            log.error("❌ Erreur renvoi email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors du renvoi de l'email"));
        }
    }

    /**
     * 🧹 Nettoyer anciennes notifications
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "Nettoyer notifications", description = "Supprime les anciennes notifications lues")
    public ResponseEntity<ApiResponse<String>> cleanupOldNotifications(@RequestParam(defaultValue = "30") int daysOld) {
        try {
            Long adminId = getCurrentAdminId();
            log.info("🧹 Nettoyage notifications: adminId={}, daysOld={}", adminId, daysOld);

            int deletedCount = adminNotificationService.cleanupOldNotifications(adminId, daysOld);

            return ResponseEntity.ok(ApiResponse.success("OK",
                    String.format("%d notifications supprimées", deletedCount)));

        } catch (Exception e) {
            log.error("❌ Erreur nettoyage: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors du nettoyage"));
        }
    }

    // =====================================
    // ENDPOINTS DE TEST (à supprimer en production)
    // =====================================

    /**
     * 🧪 Créer notification de test
     */
    @PostMapping("/test")
    @Operation(summary = "Test notification", description = "Crée une notification de test (dev uniquement)")
    public ResponseEntity<ApiResponse<String>> createTestNotification(@Valid @RequestBody CreateTestNotificationRequest request) {
        try {
            Long adminId = getCurrentAdminId();
            log.info("🧪 Créer notification test: adminId={}, type={}", adminId, request.getType());

            adminNotificationService.createTestNotification(adminId, request);

            return ResponseEntity.ok(ApiResponse.success("OK", "Notification de test créée"));

        } catch (Exception e) {
            log.error("❌ Erreur création test: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la création de la notification test"));
        }
    }
}