package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.NotificationDTO;
import org.example.collectfocep.entities.Notification; // ✅ IMPORT CORRIGÉ
import org.example.collectfocep.mappers.NotificationMapper; // ✅ IMPORT AJOUTÉ
import org.example.collectfocep.services.interfaces.NotificationService; // ✅ IMPORT CORRIGÉ
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        log.info("Récupération des notifications pour: {}", authentication.getName());

        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("dateCreation").descending());
            Page<Notification> notifications = notificationService.findByUser(authentication.getName(), pageRequest);
            Page<NotificationDTO> dtoPage = notifications.map(notificationMapper::toDTO);

            ApiResponse<Page<NotificationDTO>> response = ApiResponse.success(dtoPage);
            response.addMeta("unreadCount", notificationService.getUnreadCount(authentication.getName()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        log.info("Marquage de la notification {} comme lue", id);

        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Notification marquée comme lue"));
        } catch (Exception e) {
            log.error("Erreur lors du marquage de la notification", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @PatchMapping("/mark-all-read")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        log.info("Marquage de toutes les notifications comme lues pour: {}", authentication.getName());

        try {
            notificationService.markAllAsRead(authentication.getName());
            return ResponseEntity.ok(ApiResponse.success(null, "Toutes les notifications ont été marquées comme lues"));
        } catch (Exception e) {
            log.error("Erreur lors du marquage de toutes les notifications", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        try {
            Long count = notificationService.getUnreadCount(authentication.getName());
            return ResponseEntity.ok(ApiResponse.success(count, "Nombre de notifications non lues récupéré"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du nombre de notifications non lues", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }
}