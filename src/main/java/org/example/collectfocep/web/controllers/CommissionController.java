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
 * Combine création, calcul et consultation
 */
@RestController
@RequestMapping("/api/commissions")
@Slf4j
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionProcessingService processingService;
    private final CommissionService commissionService;
    private final CommissionParameterMapper parameterMapper;

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

    /**
     * Traitement asynchrone des commissions
     */
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

    /**
     * Traitement batch pour une agence
     */
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

    /**
     * Création/modification des paramètres de commission
     */
    @PostMapping("/parameters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createCommissionParameter(
            @Valid @RequestBody CommissionParameterDTO parameterDTO) {

        log.info("Création paramètre commission: type={}, scope={}",
                parameterDTO.getTypeCommission(), parameterDTO.getScope());

        try {
            // Validation du DTO
            if (!parameterDTO.isValid()) {
                return ResponseEntity.badRequest()
                        .body("Un seul scope doit être défini (client, collecteur, ou agence)");
            }

            // Mapping vers entité
            CommissionParameter parameter = parameterMapper.toEntity(parameterDTO);

            // Sauvegarde avec validation
            CommissionParameter saved = commissionService.saveCommissionParameter(parameter);

            // Retour du DTO enrichi
            CommissionParameterDTO result = parameterMapper.toDTO(saved);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Erreur création paramètre commission: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Mise à jour des paramètres de commission
     */
    @PutMapping("/parameters/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateCommissionParameter(
            @PathVariable Long id,
            @Valid @RequestBody CommissionParameterDTO parameterDTO) {

        log.info("Mise à jour paramètre commission: {}", id);

        try {
            parameterDTO.setId(id);

            if (!parameterDTO.isValid()) {
                return ResponseEntity.badRequest()
                        .body("Un seul scope doit être défini");
            }

            CommissionParameter parameter = parameterMapper.toEntity(parameterDTO);
            CommissionParameter saved = commissionService.saveCommissionParameter(parameter);

            return ResponseEntity.ok(parameterMapper.toDTO(saved));

        } catch (Exception e) {
            log.error("Erreur mise à jour paramètre: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Consultation des commissions par collecteur
     */
    @GetMapping("/collecteur/{collecteurId}")
    @CollecteurManagement
    public ResponseEntity<List<CommissionCalculation>> getCommissionsByCollecteur(
            @PathVariable Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Consultation commissions collecteur: {}", collecteurId);

        // Si pas de dates spécifiées, utiliser le mois courant
        if (startDate == null || endDate == null) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }

        try {
            // Utilisation du moteur de calcul pour récupérer l'historique
            CommissionContext context = CommissionContext.of(collecteurId, startDate, endDate);
            var calculations = processingService.getCalculationEngine().calculateBatch(context);

            return ResponseEntity.ok(calculations);

        } catch (Exception e) {
            log.error("Erreur consultation commissions: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Consultation des commissions par agence
     */
    @GetMapping("/agence/{agenceId}")
    @AgenceAccess
    public ResponseEntity<?> getCommissionsByAgence(@PathVariable Long agenceId) {

        log.info("Consultation commissions agence: {}", agenceId);

        try {
            var commissions = commissionService.findByAgenceId(agenceId);
            return ResponseEntity.ok(commissions);

        } catch (Exception e) {
            log.error("Erreur consultation agence: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Simulation de calcul de commission
     */
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

    /**
     * Validation des paramètres de commission
     */
    @PostMapping("/parameters/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> validateCommissionParameter(
            @Valid @RequestBody CommissionParameterDTO parameterDTO) {

        try {
            CommissionParameter parameter = parameterMapper.toEntity(parameterDTO);
            var validationService = new CommissionValidationService();
            var validationResult = validationService.validateCommissionParameters(parameter);

            return ResponseEntity.ok(Map.of(
                    "valid", validationResult.isValid(),
                    "errors", validationResult.getErrors(),
                    "warnings", validationResult.getWarnings()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}