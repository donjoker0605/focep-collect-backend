package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.ReportsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@Slf4j
@RequiredArgsConstructor
public class EnhancedReportController {

    private final ReportsService reportService;
    private final SecurityService securityService;

    /**
     * Génère un rapport mensuel détaillé pour un collecteur avec les 31 colonnes
     * représentant les jours du mois comme spécifié dans le cahier des charges.
     */
    @GetMapping("/collecteur/{collecteurId}/monthly")
    @PreAuthorize("@securityService.canAccessCollecteur(authentication, #collecteurId)")
    public ResponseEntity<byte[]> getMonthlyCollecteurReport(
            @PathVariable Long collecteurId,
            @RequestParam int month,
            @RequestParam int year) {

        log.info("Génération du rapport mensuel pour le collecteur: {} - {}/{}",
                collecteurId, month, year);

        try {
            byte[] report = reportService.generateCollecteurMonthlyReport(collecteurId, month, year);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=rapport_collecteur_" + collecteurId + "_" + month + "_" + year + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(report);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport mensuel", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }
}
