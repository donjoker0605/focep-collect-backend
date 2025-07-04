package org.example.collectfocep.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.AdminNotification;
import org.example.collectfocep.services.impl.AdminNotificationService;
import org.example.collectfocep.dto.AdminDashboardActivities;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Slf4j
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;
    private final SecurityService securityService;

    @Autowired
    public AdminNotificationController(AdminNotificationService adminNotificationService,
                                       SecurityService securityService) {
        this.adminNotificationService = adminNotificationService;
        this.securityService = securityService;
    }

    /**
     * 📊 Dashboard notifications avec ton service getDashboardActivities
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardActivities>> getDashboard(
            @RequestParam(defaultValue = "60") int lastMinutes) {
        try {
            Long adminId = securityService.getCurrentUserId();
            log.info("📊 Récupération dashboard admin: adminId={}, lastMinutes={}", adminId, lastMinutes);

            AdminDashboardActivities dashboard = adminNotificationService.getDashboardActivities(adminId, lastMinutes);

            return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard récupéré avec succès"));

        } catch (Exception e) {
            log.error("❌ Erreur dashboard admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du chargement du dashboard"));
        }
    }

    /**
     * 🚨 Notifications critiques avec ton service getCriticalNotifications
     */
    @GetMapping("/critical")
    public ResponseEntity<ApiResponse<List<AdminNotification>>> getCriticalNotifications() {
        try {
            Long adminId = securityService.getCurrentUserId();
            log.info("🚨 Récupération notifications critiques: adminId={}", adminId);

            List<AdminNotification> notifications = adminNotificationService.getCriticalNotifications(adminId);

            return ResponseEntity.ok(ApiResponse.success(notifications, "Notifications critiques récupérées"));

        } catch (Exception e) {
            log.error("❌ Erreur notifications critiques: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des notifications critiques"));
        }
    }

    /**
     * ✅ Marquer notification comme lue avec ton service markAsRead
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        try {
            Long adminId = securityService.getCurrentUserId();
            log.info("✅ Marquer notification comme lue: notificationId={}, adminId={}", id, adminId);

            adminNotificationService.markAsRead(id, adminId);

            return ResponseEntity.ok(ApiResponse.success("OK", "Notification marquée comme lue"));

        } catch (Exception e) {
            log.error("❌ Erreur marquage lecture: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors du marquage de la notification"));
        }
    }

    /**
     * 📋 Toutes les notifications d'un admin (avec pagination future si besoin)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<AdminNotification>>> getAllNotifications() {
        try {
            Long adminId = securityService.getCurrentUserId();
            log.info("📋 Récupération toutes notifications: adminId={}", adminId);

            // Tu peux ajouter cette méthode à ton service
            List<AdminNotification> notifications = adminNotificationService.getAllNotifications(adminId);

            return ResponseEntity.ok(ApiResponse.success(notifications, "Toutes les notifications récupérées"));

        } catch (Exception e) {
            log.error("❌ Erreur récupération notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des notifications"));
        }
    }

    /**
     * 🔢 Compter notifications non lues
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        try {
            Long adminId = securityService.getCurrentUserId();
            log.debug("🔢 Comptage notifications non lues: adminId={}", adminId);

            // Tu peux ajouter cette méthode à ton service
            Long count = adminNotificationService.getUnreadCount(adminId);

            return ResponseEntity.ok(ApiResponse.success(count, "Nombre de notifications non lues"));

        } catch (Exception e) {
            log.error("❌ Erreur comptage notifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du comptage"));
        }
    }

    /**
     * 🗑️ Marquer toutes les notifications comme lues
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        try {
            Long adminId = securityService.getCurrentUserId();
            log.info("🗑️ Marquer toutes notifications comme lues: adminId={}", adminId);

            // Tu peux ajouter cette méthode à ton service
            adminNotificationService.markAllAsRead(adminId);

            return ResponseEntity.ok(ApiResponse.success("OK", "Toutes les notifications marquées comme lues"));

        } catch (Exception e) {
            log.error("❌ Erreur marquage global: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors du marquage global"));
        }
    }
}