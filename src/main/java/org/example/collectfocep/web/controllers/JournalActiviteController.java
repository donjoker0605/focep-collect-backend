package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.JournalActiviteDTO;
import org.example.collectfocep.services.impl.AuditService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
public class JournalActiviteController {

    private final AuditService auditService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("@securityService.canAccessUserData(authentication, #userId)")
    public ResponseEntity<ApiResponse<Page<JournalActiviteDTO>>> getUserActivities(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Récupération des activités utilisateur {} pour le {}", userId, date);

        // CORRECTION: Convertir userId en String pour correspondre à la signature de getActivitesByUser
        String username = getUsernameFromUserId(userId);
        Page<JournalActiviteDTO> activities = auditService.getActivitesByUser(username, date, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(activities, "Activités récupérées avec succès")
        );
    }

    private String getUsernameFromUserId(Long userId) {
        // Implémenter cette méthode pour récupérer le username à partir de l'userId
        // Par exemple, en utilisant un service utilisateur ou en interrogeant la base de données
        return "user_" + userId;
    }

    @GetMapping("/agence/{agenceId}")
    @PreAuthorize("hasRole('ADMIN') and @securityService.belongsToAgence(authentication, #agenceId)")
    public ResponseEntity<ApiResponse<Page<JournalActiviteDTO>>> getAgenceActivities(
            @PathVariable Long agenceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Récupération des activités agence {} pour le {}", agenceId, date);

        Page<JournalActiviteDTO> activities = auditService.getActivitesByAgence(agenceId, date, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(activities, "Activités de l'agence récupérées avec succès")
        );
    }

    /**
     * Récupérer les activités de l'utilisateur connecté
     */
    @GetMapping("/mes-activites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<JournalActiviteDTO>>> getMesActivites(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20, sort = "timestamp,desc") Pageable pageable,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Récupération des activités de {} pour le {}", username, date);

        Page<JournalActiviteDTO> activities = auditService.getActivitesByUser(username, date, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(activities, "Activités récupérées avec succès")
        );
    }

    /**
     * Récupérer les activités d'un utilisateur spécifique (admin only)
     */
    @GetMapping("/user/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<JournalActiviteDTO>>> getUserActivities(
            @PathVariable String username,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20, sort = "timestamp,desc") Pageable pageable) {

        log.info("Admin: Récupération des activités de {} pour le {}", username, date);

        Page<JournalActiviteDTO> activities = auditService.getActivitesByUser(username, date, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(activities, "Activités utilisateur récupérées avec succès")
        );
    }


    /**
     * Obtenir les statistiques d'activité
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getActivityStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Récupération des stats d'activité pour {} du {} au {}",
                username, startDate, endDate);

        Map<String, Long> stats = auditService.getUserActivityStats(username, startDate, endDate);

        return ResponseEntity.ok(
                ApiResponse.success(stats, "Statistiques récupérées avec succès")
        );
    }
}