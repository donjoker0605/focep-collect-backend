package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.JournalActiviteDTO;
import org.example.collectfocep.services.interfaces.JournalActiviteService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/journal-activite")
@RequiredArgsConstructor
@Slf4j
public class JournalActiviteController {

    private final JournalActiviteService journalActiviteService;

    /**
     * Récupérer les activités d'un utilisateur pour une date
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("@securityService.canAccessUserActivities(authentication, #userId)")
    public ResponseEntity<ApiResponse<Page<JournalActiviteDTO>>> getUserActivities(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Récupération des activités pour l'utilisateur {} à la date {}", userId, date);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<JournalActiviteDTO> activites = journalActiviteService.getActivitesByUser(
                userId, date, pageRequest);

        ApiResponse<Page<JournalActiviteDTO>> response = ApiResponse.success(activites);
        response.addMeta("date", date.toString());
        response.addMeta("userId", userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer les activités d'une agence pour une date
     */
    @GetMapping("/agence/{agenceId}")
    @PreAuthorize("@securityService.canAccessAgenceActivities(authentication, #agenceId)")
    public ResponseEntity<ApiResponse<Page<JournalActiviteDTO>>> getAgenceActivities(
            @PathVariable Long agenceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Récupération des activités pour l'agence {} à la date {}", agenceId, date);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<JournalActiviteDTO> activites = journalActiviteService.getActivitesByAgence(
                agenceId, date, pageRequest);

        ApiResponse<Page<JournalActiviteDTO>> response = ApiResponse.success(activites);
        response.addMeta("date", date.toString());
        response.addMeta("agenceId", agenceId);

        return ResponseEntity.ok(response);
    }

    /**
     * Recherche avancée avec filtres multiples
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<JournalActiviteDTO>>> searchActivities(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long agenceId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Recherche d'activités avec filtres: userId={}, agenceId={}, action={}, période={} à {}",
                userId, agenceId, action, dateDebut, dateFin);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<JournalActiviteDTO> activites = journalActiviteService.getActivitesWithFilters(
                userId, agenceId, action, entityType, dateDebut, dateFin, pageRequest);

        ApiResponse<Page<JournalActiviteDTO>> response = ApiResponse.success(activites);
        response.addMeta("filters", Map.of(
                "userId", userId,
                "agenceId", agenceId,
                "action", action,
                "entityType", entityType,
                "dateDebut", dateDebut,
                "dateFin", dateFin
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Statistiques d'activité pour un utilisateur
     */
    @GetMapping("/stats/user/{userId}")
    @PreAuthorize("@securityService.canAccessUserActivities(authentication, #userId)")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUserActivityStats(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("Récupération des statistiques d'activité pour l'utilisateur {} du {} au {}",
                userId, dateDebut, dateFin);

        Map<String, Long> stats = journalActiviteService.getActivityStats(userId, dateDebut, dateFin);

        ApiResponse<Map<String, Long>> response = ApiResponse.success(stats);
        response.addMeta("userId", userId);
        response.addMeta("dateDebut", dateDebut);
        response.addMeta("dateFin", dateFin);

        return ResponseEntity.ok(response);
    }

    /**
     * Actions disponibles pour les filtres
     */
    @GetMapping("/actions")
    @PreAuthorize("hasAnyRole('COLLECTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAvailableActions() {
        Map<String, String> actions = Map.of(
                "CREATE_CLIENT", "Création client",
                "MODIFY_CLIENT", "Modification client",
                "DELETE_CLIENT", "Suppression client",
                "LOGIN", "Connexion",
                "LOGOUT", "Déconnexion",
                "TRANSACTION_EPARGNE", "Épargne",
                "TRANSACTION_RETRAIT", "Retrait",
                "VALIDATE_TRANSACTION", "Validation transaction"
        );

        return ResponseEntity.ok(ApiResponse.success(actions));
    }
}