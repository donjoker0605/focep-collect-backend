package org.example.collectfocep.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CollecteurActivitySummaryDTO;
import org.example.collectfocep.dto.JournalActiviteDTO;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.CollecteurActivityService;
import org.example.collectfocep.services.interfaces.JournalActiviteService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 🎯 Contrôleur pour la supervision des activités des collecteurs par les admins
 *
 * PERMISSIONS :
 * - ADMIN : Peut consulter les activités des collecteurs de son agence uniquement
 * - SUPER_ADMIN : Peut consulter les activités de tous les collecteurs
 *
 * ENDPOINTS :
 * 1. GET /{collecteurId}/activites - Journal d'activité d'un collecteur spécifique
 * 2. GET /activites/resume - Résumé des activités de tous les collecteurs accessibles
 * 3. GET /{collecteurId}/activites/stats - Statistiques détaillées d'un collecteur
 * 4. GET /{collecteurId}/activites/critiques - Activités critiques détectées
 */
@RestController
@RequestMapping("/api/admin/collecteurs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Collecteur Supervision", description = "Supervision des activités des collecteurs par les admins")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminCollecteurActivityController {

    private final CollecteurActivityService collecteurActivityService;
    private final JournalActiviteService journalActiviteService;
    private final SecurityService securityService;

    // =====================================
    // MÉTHODES UTILITAIRES
    // =====================================

    /**
     * Récupère l'ID de l'admin connecté pour les logs et permissions
     */
    private Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return securityService.getCurrentUserId(authentication);
    }

    /**
     * Vérifie les permissions avant d'accéder aux données d'un collecteur
     */
    private void validateCollecteurAccess(Long collecteurId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!securityService.canAccessUserActivities(authentication, collecteurId)) {
            throw new org.example.collectfocep.exceptions.UnauthorizedException(
                    "Accès non autorisé aux activités du collecteur " + collecteurId);
        }
    }

    // =====================================
    // ENDPOINTS PRINCIPAUX
    // =====================================

    /**
     * 📋 Récupère le journal d'activité d'un collecteur spécifique
     *
     * @param collecteurId ID du collecteur
     * @param date Date pour filtrer les activités (optionnel, défaut = aujourd'hui)
     * @param page Numéro de page (défaut = 0)
     * @param size Taille de page (défaut = 20)
     * @param sortBy Champ de tri (défaut = timestamp)
     * @param sortDir Direction du tri (défaut = desc)
     */
    @GetMapping("/{collecteurId}/activites")
    @Operation(summary = "Journal d'activité d'un collecteur",
            description = "Récupère les activités d'un collecteur pour une date donnée avec pagination")
    public ResponseEntity<ApiResponse<Page<JournalActiviteDTO>>> getCollecteurActivities(
            @PathVariable @NotNull @Parameter(description = "ID du collecteur") Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date pour filtrer les activités (défaut = aujourd'hui)") LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0)
            @Parameter(description = "Numéro de page") int page,
            @RequestParam(defaultValue = "20") @Min(1)
            @Parameter(description = "Taille de page") int size,
            @RequestParam(defaultValue = "timestamp")
            @Parameter(description = "Champ de tri") String sortBy,
            @RequestParam(defaultValue = "desc")
            @Parameter(description = "Direction du tri (asc/desc)") String sortDir) {

        log.info("🔍 Admin {} consulte les activités du collecteur {} pour la date {}",
                getCurrentAdminId(), collecteurId, date != null ? date : "aujourd'hui");

        try {
            // 🔒 Vérification des permissions
            validateCollecteurAccess(collecteurId);

            // 📅 Date par défaut = aujourd'hui
            LocalDate targetDate = date != null ? date : LocalDate.now();

            // 🔄 Configuration du tri
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

            // 📊 Récupération des activités
            Page<JournalActiviteDTO> activites = journalActiviteService.getActivitesByUser(
                    collecteurId, targetDate, pageRequest);

            // 📤 Réponse avec métadonnées
            ApiResponse<Page<JournalActiviteDTO>> response = ApiResponse.success(activites);
            response.addMeta("collecteurId", collecteurId);
            response.addMeta("date", targetDate.toString());
            response.addMeta("totalActivites", activites.getTotalElements());

            log.info("✅ {} activités trouvées pour le collecteur {} à la date {}",
                    activites.getTotalElements(), collecteurId, targetDate);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des activités du collecteur {}: {}",
                    collecteurId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 📊 Résumé des activités de tous les collecteurs accessibles à l'admin
     * Retourne une vue d'ensemble pour le dashboard de supervision
     */
    @GetMapping("/activites/resume")
    @Operation(summary = "Résumé des activités des collecteurs",
            description = "Vue d'ensemble des activités de tous les collecteurs accessibles")
    public ResponseEntity<ApiResponse<List<CollecteurActivitySummaryDTO>>> getCollecteursActivitySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date de début (défaut = aujourd'hui)") LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date de fin (défaut = dateDebut)") LocalDate dateFin) {

        Long adminId = getCurrentAdminId();
        log.info("📈 Admin {} demande le résumé des activités collecteurs du {} au {}",
                adminId, dateDebut != null ? dateDebut : "aujourd'hui",
                dateFin != null ? dateFin : "aujourd'hui");

        try {
            // 📅 Dates par défaut
            LocalDate startDate = dateDebut != null ? dateDebut : LocalDate.now();
            LocalDate endDate = dateFin != null ? dateFin : startDate;

            // 🔍 Récupération du résumé basé sur les permissions de l'admin
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            List<CollecteurActivitySummaryDTO> summary =
                    collecteurActivityService.getCollecteursActivitySummary(authentication, startDate, endDate);

            // 📤 Réponse avec métadonnées
            ApiResponse<List<CollecteurActivitySummaryDTO>> response = ApiResponse.success(summary);
            response.addMeta("dateDebut", startDate.toString());
            response.addMeta("dateFin", endDate.toString());
            response.addMeta("nombreCollecteurs", summary.size());
            response.addMeta("adminId", adminId);

            log.info("✅ Résumé généré pour {} collecteurs du {} au {}",
                    summary.size(), startDate, endDate);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé d'activités: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 📈 Statistiques détaillées d'un collecteur
     * Retourne des métriques avancées sur les performances et activités
     */
    @GetMapping("/{collecteurId}/activites/stats")
    @Operation(summary = "Statistiques détaillées d'un collecteur",
            description = "Métriques avancées sur les performances et activités d'un collecteur")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCollecteurDetailedStats(
            @PathVariable @NotNull @Parameter(description = "ID du collecteur") Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date de début (défaut = il y a 7 jours)") LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date de fin (défaut = aujourd'hui)") LocalDate dateFin) {

        log.info("📊 Admin {} demande les stats détaillées du collecteur {} du {} au {}",
                getCurrentAdminId(), collecteurId, dateDebut, dateFin);

        try {
            // 🔒 Vérification des permissions
            validateCollecteurAccess(collecteurId);

            // 📅 Dates par défaut (7 derniers jours)
            LocalDate endDate = dateFin != null ? dateFin : LocalDate.now();
            LocalDate startDate = dateDebut != null ? dateDebut : endDate.minusDays(7);

            // 📈 Récupération des statistiques
            Map<String, Object> stats = collecteurActivityService.getCollecteurDetailedStats(
                    collecteurId, startDate, endDate);

            // 📤 Réponse avec métadonnées
            ApiResponse<Map<String, Object>> response = ApiResponse.success(stats);
            response.addMeta("collecteurId", collecteurId);
            response.addMeta("dateDebut", startDate.toString());
            response.addMeta("dateFin", endDate.toString());
            response.addMeta("periodeJours", java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);

            log.info("✅ Stats générées pour le collecteur {} sur {} jours",
                    collecteurId, java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération des stats du collecteur {}: {}",
                    collecteurId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 🚨 Activités critiques détectées pour un collecteur
     * Retourne les activités suspectes ou nécessitant une attention particulière
     */
    @GetMapping("/{collecteurId}/activites/critiques")
    @Operation(summary = "Activités critiques d'un collecteur",
            description = "Activités suspectes ou nécessitant une attention particulière")
    public ResponseEntity<ApiResponse<List<JournalActiviteDTO>>> getCriticalActivities(
            @PathVariable @NotNull @Parameter(description = "ID du collecteur") Long collecteurId,
            @RequestParam(defaultValue = "7") @Min(1)
            @Parameter(description = "Nombre de jours à analyser") int dernierJours,
            @RequestParam(defaultValue = "20") @Min(1)
            @Parameter(description = "Limite de résultats") int limit) {

        log.info("🚨 Admin {} consulte les activités critiques du collecteur {} (derniers {} jours)",
                getCurrentAdminId(), collecteurId, dernierJours);

        try {
            // 🔒 Vérification des permissions
            validateCollecteurAccess(collecteurId);

            // 📅 Période d'analyse
            LocalDate dateDebut = LocalDate.now().minusDays(dernierJours);
            LocalDate dateFin = LocalDate.now();

            // 🔍 Détection des activités critiques
            List<JournalActiviteDTO> criticalActivities =
                    collecteurActivityService.getCriticalActivities(collecteurId, dateDebut, dateFin, limit);

            // 📤 Réponse avec métadonnées
            ApiResponse<List<JournalActiviteDTO>> response = ApiResponse.success(criticalActivities);
            response.addMeta("collecteurId", collecteurId);
            response.addMeta("dernierJours", dernierJours);
            response.addMeta("nombreCritiques", criticalActivities.size());
            response.addMeta("dateAnalyse", LocalDate.now().toString());

            if (criticalActivities.size() > 0) {
                log.warn("⚠️ {} activités critiques détectées pour le collecteur {}",
                        criticalActivities.size(), collecteurId);
            } else {
                log.info("✅ Aucune activité critique pour le collecteur {}", collecteurId);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'analyse des activités critiques du collecteur {}: {}",
                    collecteurId, e.getMessage(), e);
            throw e;
        }
    }

    // =====================================
    // ENDPOINTS UTILITAIRES
    // =====================================

    /**
     * 🔍 Recherche d'activités avec filtres avancés
     */
    @GetMapping("/{collecteurId}/activites/search")
    @Operation(summary = "Recherche d'activités avec filtres",
            description = "Recherche avancée dans les activités d'un collecteur")
    public ResponseEntity<ApiResponse<Page<JournalActiviteDTO>>> searchActivities(
            @PathVariable @NotNull Long collecteurId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {

        log.info("🔍 Recherche d'activités pour collecteur {} avec filtres: action={}, entityType={}",
                collecteurId, action, entityType);

        try {
            // 🔒 Vérification des permissions
            validateCollecteurAccess(collecteurId);

            // 📅 Dates par défaut (30 derniers jours)
            LocalDate endDate = dateFin != null ? dateFin : LocalDate.now();
            LocalDate startDate = dateDebut != null ? dateDebut : endDate.minusDays(30);

            // 📄 Configuration pagination
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

            // 🔍 Recherche avec filtres
            Page<JournalActiviteDTO> activites = journalActiviteService.getActivitesWithFilters(
                    collecteurId, null, action, entityType, startDate, endDate, pageRequest);

            // 📤 Réponse
            ApiResponse<Page<JournalActiviteDTO>> response = ApiResponse.success(activites);
            response.addMeta("collecteurId", collecteurId);
            response.addMeta("filtres", Map.of(
                    "action", action != null ? action : "tous",
                    "entityType", entityType != null ? entityType : "tous",
                    "dateDebut", startDate.toString(),
                    "dateFin", endDate.toString()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la recherche d'activités: {}", e.getMessage(), e);
            throw e;
        }
    }
}