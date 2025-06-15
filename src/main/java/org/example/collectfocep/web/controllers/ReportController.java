package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ReportDTO;
import org.example.collectfocep.dto.ReportRequestDTO;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.ReportService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;

/**
 * ✅ CONTRÔLEUR POUR CORRIGER L'ERREUR 404 /api/reports
 */
@RestController
@RequestMapping("/api/reports")
@Slf4j
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final SecurityService securityService;

    /**
     * ✅ ENDPOINT PRINCIPAL - CORRIGER LE 404
     * GET /api/reports - Récupérer les rapports récents pour l'admin connecté
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getRecentReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("📋 Récupération des rapports récents - page: {}, size: {}", page, size);

        try {
            // ✅ RÉCUPÉRER L'AGENCE DE L'ADMIN CONNECTÉ
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                log.warn("❌ Impossible de déterminer l'agence de l'utilisateur connecté");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            PageRequest pageRequest = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.DESC, "dateCreation"));

            Page<ReportDTO> reportsPage = reportService.getRecentReportsByAgence(agenceId, pageRequest);

            ApiResponse<List<ReportDTO>> response = ApiResponse.success(reportsPage.getContent());
            response.addMeta("totalElements", reportsPage.getTotalElements());
            response.addMeta("totalPages", reportsPage.getTotalPages());
            response.addMeta("currentPage", page);
            response.addMeta("hasNext", reportsPage.hasNext());

            log.info("✅ {} rapports récupérés pour l'agence {}", reportsPage.getContent().size(), agenceId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des rapports récents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des rapports: " + e.getMessage()));
        }
    }

    /**
     * ✅ GÉNÉRER UN NOUVEAU RAPPORT
     * POST /api/reports/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReportDTO>> generateReport(@Valid @RequestBody ReportRequestDTO request) {

        log.info("📊 Génération d'un rapport de type: {} pour la période: {} - {}",
                request.getType(), request.getDateDebut(), request.getDateFin());

        try {
            // ✅ SÉCURITÉ: VÉRIFIER L'AGENCE
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            // ✅ VALIDATION DES PARAMÈTRES
            if (request.getType() == null || request.getType().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Type de rapport requis"));
            }

            if (request.getDateDebut() == null || request.getDateFin() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Période de rapport requise"));
            }

            // ✅ VALIDATION SPÉCIFIQUE POUR LES RAPPORTS DE COLLECTEUR
            if ("collecteur".equals(request.getType()) && request.getCollecteurId() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("ID collecteur requis pour un rapport de collecteur"));
            }

            // ✅ SÉCURITÉ: VÉRIFIER QUE LE COLLECTEUR APPARTIENT À L'AGENCE
            if (request.getCollecteurId() != null) {
                boolean hasAccess = securityService.isAdminOfCollecteur(
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication(),
                        request.getCollecteurId()
                );

                if (!hasAccess) {
                    log.warn("❌ Admin {} tente d'accéder au collecteur {} sans autorisation",
                            securityService.getCurrentUserEmail(), request.getCollecteurId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Accès non autorisé à ce collecteur"));
                }
            }

            // ✅ GÉNÉRER LE RAPPORT
            ReportDTO generatedReport = reportService.generateReport(request, agenceId);

            log.info("✅ Rapport généré avec succès: {}", generatedReport.getId());
            return ResponseEntity.ok(ApiResponse.success(generatedReport, "Rapport généré avec succès"));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du rapport", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la génération du rapport: " + e.getMessage()));
        }
    }

    /**
     * ✅ RÉCUPÉRER UN RAPPORT SPÉCIFIQUE
     * GET /api/reports/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReportDTO>> getReportById(@PathVariable Long id) {

        log.info("📋 Récupération du rapport: {}", id);

        try {
            // ✅ SÉCURITÉ: VÉRIFIER L'AGENCE
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            ReportDTO report = reportService.getReportById(id, agenceId);

            if (report == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Rapport non trouvé"));
            }

            return ResponseEntity.ok(ApiResponse.success(report));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du rapport {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération du rapport: " + e.getMessage()));
        }
    }

    /**
     * ✅ TÉLÉCHARGER UN RAPPORT
     * GET /api/reports/{id}/download
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> downloadReport(@PathVariable Long id) {

        log.info("⬇️ Téléchargement du rapport: {}", id);

        try {
            // ✅ SÉCURITÉ: VÉRIFIER L'AGENCE
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            // TODO: Implémenter la logique de téléchargement
            // byte[] reportData = reportService.downloadReport(id, agenceId);

            // Pour l'instant, retourner une réponse temporaire
            return ResponseEntity.ok(ApiResponse.success(null, "Téléchargement en cours de développement"));

        } catch (Exception e) {
            log.error("❌ Erreur lors du téléchargement du rapport {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du téléchargement: " + e.getMessage()));
        }
    }

    /**
     * ✅ SUPPRIMER UN RAPPORT
     * DELETE /api/reports/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {

        log.info("🗑️ Suppression du rapport: {}", id);

        try {
            // ✅ SÉCURITÉ: VÉRIFIER L'AGENCE
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            boolean deleted = reportService.deleteReport(id, agenceId);

            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Rapport non trouvé"));
            }

            log.info("✅ Rapport {} supprimé avec succès", id);
            return ResponseEntity.ok(ApiResponse.success(null, "Rapport supprimé avec succès"));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression du rapport {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la suppression: " + e.getMessage()));
        }
    }

    /**
     * ✅ OBTENIR LES TYPES DE RAPPORTS DISPONIBLES
     * GET /api/reports/types
     */
    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getReportTypes() {

        log.info("📝 Récupération des types de rapports disponibles");

        try {
            List<String> reportTypes = reportService.getAvailableReportTypes();
            return ResponseEntity.ok(ApiResponse.success(reportTypes));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des types de rapports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des types: " + e.getMessage()));
        }
    }
}