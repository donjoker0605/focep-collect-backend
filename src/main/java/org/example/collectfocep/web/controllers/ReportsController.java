package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.ReportsService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportsController {

    private final ReportsService reportsService;
    private final SecurityService securityService;

    /**
     * Générer un rapport
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReportDTO>> generateReport(@Valid @RequestBody ReportRequestDTO request) {
        log.info("📊 Génération de rapport: type={}, période={} à {}",
                request.getType(), request.getDateDebut(), request.getDateFin());

        try {
            // Sécurité: vérifier l'accès à l'agence
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            // Forcer l'agence de l'admin
            request.setAgenceId(agenceId);

            ReportDTO report = reportsService.generateReport(request);
            log.info("✅ Rapport généré avec succès: {}", report.getId());

            return ResponseEntity.ok(ApiResponse.success(
                    report,
                    "Rapport généré avec succès"
            ));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du rapport", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la génération: " + e.getMessage()));
        }
    }

    /**
     * Récupérer la liste des rapports
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ReportDTO>>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("📋 Récupération des rapports: page={}, size={}, type={}", page, size, type);

        try {
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            List<ReportDTO> reports = reportsService.getReportsByAgence(
                    agenceId, type, dateDebut, dateFin, page, size
            );

            return ResponseEntity.ok(ApiResponse.success(
                    reports,
                    "Rapports récupérés avec succès"
            ));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des rapports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur serveur"));
        }
    }

    /**
     * Télécharger un rapport
     */
    @GetMapping("/{reportId}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> downloadReport(@PathVariable Long reportId) {
        log.info("📥 Téléchargement du rapport: {}", reportId);

        try {
            // Vérifier l'accès au rapport
            if (!reportsService.hasAccessToReport(reportId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé à ce rapport"));
            }

            byte[] reportData = reportsService.getReportData(reportId);
            String filename = reportsService.getReportFilename(reportId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new ByteArrayResource(reportData));

        } catch (Exception e) {
            log.error("❌ Erreur lors du téléchargement du rapport", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du téléchargement"));
        }
    }

    /**
     * Rapport collecteur
     */
    @GetMapping("/collecteur/{collecteurId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> generateCollecteurReport(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "json") String format) {

        log.info("📊 Génération rapport collecteur: {}, période: {} à {}",
                collecteurId, dateDebut, dateFin);

        try {
            // Vérifier l'accès au collecteur
            if (!securityService.hasPermissionForCollecteur(collecteurId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé à ce collecteur"));
            }

            if ("pdf".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
                byte[] reportData = reportsService.generateCollecteurReportFile(
                        collecteurId, dateDebut, dateFin, format
                );

                String filename = String.format("rapport_collecteur_%d_%s_%s.%s",
                        collecteurId, dateDebut, dateFin, format);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .contentType("excel".equalsIgnoreCase(format) ?
                                MediaType.parseMediaType("application/vnd.ms-excel") :
                                MediaType.APPLICATION_PDF)
                        .body(new ByteArrayResource(reportData));
            } else {
                CollecteurReportDTO report = reportsService.generateCollecteurReport(
                        collecteurId, dateDebut, dateFin
                );
                return ResponseEntity.ok(ApiResponse.success(report));
            }

        } catch (Exception e) {
            log.error("❌ Erreur génération rapport collecteur", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Rapport commissions
     */
    @GetMapping("/commissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CommissionReportDTO>> generateCommissionReport(
            @RequestParam(required = false) Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("💰 Génération rapport commissions: collecteur={}, période: {} à {}",
                collecteurId, dateDebut, dateFin);

        try {
            Long agenceId = securityService.getCurrentUserAgenceId();

            CommissionReportDTO report;
            if (collecteurId != null) {
                // Vérifier l'accès au collecteur
                if (!securityService.hasPermissionForCollecteur(collecteurId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Accès non autorisé"));
                }
                report = reportsService.generateCommissionReportForCollecteur(
                        collecteurId, dateDebut, dateFin
                );
            } else {
                // Rapport pour toute l'agence
                report = reportsService.generateCommissionReportForAgence(
                        agenceId, dateDebut, dateFin
                );
            }

            return ResponseEntity.ok(ApiResponse.success(report));

        } catch (Exception e) {
            log.error("❌ Erreur génération rapport commissions", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Rapport agence
     */
    @GetMapping("/agence")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<AgenceReportDTO>> generateAgenceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("🏢 Génération rapport agence: période: {} à {}", dateDebut, dateFin);

        try {
            Long agenceId = securityService.getCurrentUserAgenceId();

            AgenceReportDTO report = reportsService.generateAgenceReport(
                    agenceId, dateDebut, dateFin
            );

            return ResponseEntity.ok(ApiResponse.success(report));

        } catch (Exception e) {
            log.error("❌ Erreur génération rapport agence", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Supprimer un rapport
     */
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long reportId) {
        log.info("🗑️ Suppression du rapport: {}", reportId);

        try {
            if (!reportsService.hasAccessToReport(reportId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            reportsService.deleteReport(reportId);

            return ResponseEntity.ok(ApiResponse.success(
                    null,
                    "Rapport supprimé avec succès"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur suppression rapport", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }
}