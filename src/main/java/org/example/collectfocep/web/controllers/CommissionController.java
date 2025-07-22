package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.exceptions.CommissionProcessingException;
import org.example.collectfocep.mappers.CommissionParameterMapper;
import org.example.collectfocep.security.annotations.AgenceAccess;
import org.example.collectfocep.security.annotations.CollecteurManagement;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.CommissionProcessingService;
import org.example.collectfocep.services.CommissionValidationService;
import org.example.collectfocep.services.impl.CommissionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur unifié pour la gestion des commissions
 */
@RestController
@RequestMapping("/api/commissions")
@Slf4j
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionProcessingService processingService;
    private final CommissionService commissionService;
    private final CommissionParameterMapper parameterMapper;
    private final SecurityService securityService;


    /**
     * Lance le traitement des commissions pour un collecteur
     */
    @PostMapping("/process")
    @CollecteurManagement
    public ResponseEntity<CommissionProcessingResult> processCommissions(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean forceRecalculation) {

        log.info("Traitement commissions - Collecteur: {}, Période: {} à {}",
                collecteurId, startDate, endDate);

        try {
            CommissionProcessingResult result;

            if (forceRecalculation) {
                result = processingService.recalculateCommissions(collecteurId, startDate, endDate, true);
            } else {
                result = processingService.processCommissionsForPeriod(collecteurId, startDate, endDate);
            }

            return ResponseEntity.ok(result);

        } catch (CommissionProcessingException e) {
            log.error("Erreur traitement commissions: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommissionProcessingResult.failure(collecteurId, e.getMessage()));
        }
    }

    // Pour compatibilité frontend

    /**
     * Endpoint attendu par le frontend React Native
     * POST /commissions/calculate
     */
    @PostMapping("/calculate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> calculateCommissions(@Valid @RequestBody CalculateCommissionRequest request) {
        log.info("Calcul commissions via /calculate - Période: {} à {}",
                request.getDateDebut(), request.getDateFin());

        try {
            // Utiliser l'agence de l'utilisateur connecté
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Agence utilisateur non trouvée"));
            }

            List<CommissionProcessingResult> results = processingService
                    .processCommissionsForAgence(agenceId, request.getDateDebut(), request.getDateFin());

            // Agréger les résultats pour l'affichage
            CommissionAggregateResult aggregate = aggregateResults(results);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", aggregate,
                    "message", "Commissions calculées avec succès"
            ));

        } catch (Exception e) {
            log.error("Erreur calcul commissions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * Calcul commissions agence
     * POST /commissions/agence/calculate
     */
    @PostMapping("/agence/calculate")
    @AgenceAccess
    public ResponseEntity<?> calculateAgenceCommissions(@Valid @RequestBody CalculateCommissionRequest request) {
        log.info("Calcul commissions agence - Période: {} à {}",
                request.getDateDebut(), request.getDateFin());

        try {
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Agence non identifiée"));
            }

            List<CommissionProcessingResult> results = processingService
                    .processCommissionsForAgence(agenceId, request.getDateDebut(), request.getDateFin());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", aggregateResults(results),
                    "agenceId", agenceId
            ));

        } catch (Exception e) {
            log.error("Erreur calcul agence: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Calcul commissions collecteur spécifique
     * POST /commissions/collecteur/{collecteurId}/calculate
     */
    @PostMapping("/collecteur/{collecteurId}/calculate")
    @CollecteurManagement
    public ResponseEntity<?> calculateCollecteurCommissions(
            @PathVariable Long collecteurId,
            @Valid @RequestBody CalculateCommissionRequest request) {

        log.info("Calcul commissions collecteur {} - Période: {} à {}",
                collecteurId, request.getDateDebut(), request.getDateFin());

        try {
            CommissionProcessingResult result = processingService
                    .processCommissionsForPeriod(collecteurId, request.getDateDebut(), request.getDateFin());

            return ResponseEntity.ok(Map.of(
                    "success", result.isSuccess(),
                    "data", result,
                    "collecteurId", collecteurId
            ));

        } catch (Exception e) {
            log.error("Erreur calcul collecteur {}: {}", collecteurId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "collecteurId", collecteurId
                    ));
        }
    }

    // Pas de modification

    @PostMapping("/process/async")
    @CollecteurManagement
    public ResponseEntity<CompletableFuture<CommissionProcessingResult>> processCommissionsAsync(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Traitement asynchrone commissions - Collecteur: {}", collecteurId);

        CompletableFuture<CommissionProcessingResult> future =
                processingService.processCommissionsAsync(collecteurId, startDate, endDate);

        return ResponseEntity.accepted().body(future);
    }

    @PostMapping("/process/batch/agence/{agenceId}")
    @AgenceAccess
    public ResponseEntity<List<CommissionProcessingResult>> processCommissionsBatch(
            @PathVariable Long agenceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Traitement batch commissions pour agence: {}", agenceId);

        List<CommissionProcessingResult> results =
                processingService.processCommissionsForAgence(agenceId, startDate, endDate);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/collecteur/{collecteurId}")
    @CollecteurManagement
    public ResponseEntity<List<CommissionCalculation>> getCommissionsByCollecteur(
            @PathVariable Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Consultation commissions collecteur: {}", collecteurId);

        if (startDate == null || endDate == null) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }

        try {
            CommissionContext context = CommissionContext.of(collecteurId, startDate, endDate);
            var calculations = processingService.getCalculationEngine().calculateBatch(context);

            return ResponseEntity.ok(calculations);

        } catch (Exception e) {
            log.error("Erreur consultation commissions: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/simulate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COLLECTEUR')")
    public ResponseEntity<CommissionResult> simulateCommission(
            @RequestBody CommissionSimulationRequest request) {

        log.info("Simulation commission - Type: {}, Montant: {}",
                request.getType(), request.getMontant());

        try {
            CommissionResult result = commissionService.calculateCommission(
                    request.getMontant(),
                    request.getType(),
                    request.getValeur(),
                    request.getTiers()
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Erreur simulation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommissionResult.failure(null, null, e.getMessage()));
        }
    }

    // MÉTHODES UTILITAIRES PRIVÉES

    /**
     * Agrège les résultats de plusieurs collecteurs pour l'affichage
     */
    private CommissionAggregateResult aggregateResults(List<CommissionProcessingResult> results) {
        return CommissionAggregateResult.builder()
                .totalCollecteurs((long) results.size())
                .collecteursTraites(results.stream().mapToLong(r -> r.isSuccess() ? 1L : 0L).sum())
                .totalCommissions(results.stream()
                        .filter(CommissionProcessingResult::isSuccess)
                        .map(CommissionProcessingResult::getTotalCommissions)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
                .totalClients(results.stream()
                        .filter(CommissionProcessingResult::isSuccess)
                        .mapToInt(CommissionProcessingResult::getNombreClients)
                        .sum())
                .details(results)
                .build();
    }
}