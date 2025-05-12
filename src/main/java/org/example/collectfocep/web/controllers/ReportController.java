package org.example.collectfocep.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.security.annotations.AgenceAccess;
import org.example.collectfocep.services.ReportService;
import org.example.collectfocep.security.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@Slf4j
public class ReportController {
    private final ReportService reportService;
    private final SecurityService securityService;

    @Autowired
    public ReportController(ReportService reportService, SecurityService securityService) {
        this.reportService = reportService;
        this.securityService = securityService;
    }

    @GetMapping("/collecteur/{collecteurId}")
    @PreAuthorize("@securityService.canAccessCollecteur(authentication, #collecteurId)")
    public ResponseEntity<byte[]> getCollecteurReport(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        log.info("Génération du rapport pour le collecteur: {} du {} au {}",
                collecteurId, dateDebut, dateFin);

        try {
            byte[] report = reportService.generateCollecteurReport(collecteurId, dateDebut, dateFin);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=rapport_collecteur_" + collecteurId + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(report);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    @GetMapping("/agence/{agenceId}")
    @AgenceAccess
    public ResponseEntity<byte[]> getAgenceReport(
            @PathVariable Long agenceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        log.info("Génération du rapport pour l'agence: {} du {} au {}",
                agenceId, dateDebut, dateFin);

        try {
            byte[] report = reportService.generateAgenceReport(agenceId, dateDebut, dateFin);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=rapport_agence_" + agenceId + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(report);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    @GetMapping("/admin/global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> getGlobalReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        log.info("Génération du rapport global du {} au {}", dateDebut, dateFin);

        try {
            byte[] report = reportService.generateGlobalReport(dateDebut, dateFin);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport_global.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(report);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport global", e);
            throw new RuntimeException("Erreur lors de la génération du rapport global", e);
        }
    }
}