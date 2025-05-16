package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.services.impl.AsyncCommissionService;
import org.example.collectfocep.services.impl.AsyncReportService;
import org.example.collectfocep.dto.CommissionProcessingResult; // CORRECTION: Type correct
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/commissions")
@Slf4j
@RequiredArgsConstructor
public class AsyncCommissionController {

    private final AsyncCommissionService asyncCommissionService;
    private final AsyncReportService asyncReportService;

    @PostMapping("/process")
    public ResponseEntity<String> processCommissions(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // CORRECTION: Type de retour correct
        CompletableFuture<CommissionProcessingResult> future = asyncCommissionService.processCommissions(
                collecteurId, startDate, endDate);

        // Return immediately with tracking ID
        String trackingId = UUID.randomUUID().toString();
        return ResponseEntity.accepted()
                .body("Processing started. Track with ID: " + trackingId);
    }

    @GetMapping("/status/{trackingId}")
    public ResponseEntity<CommissionProcessingResult> getProcessingStatus(@PathVariable String trackingId) {
        // Implement status tracking logic
        // Pour l'instant, retourner un exemple de base
        CommissionProcessingResult result = CommissionProcessingResult.builder()
                .success(true)
                .build();
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint pour forcer le recalcul des commissions
     */
    @PostMapping("/recalculate")
    public ResponseEntity<String> recalculateCommissions(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        CompletableFuture<CommissionProcessingResult> future = asyncCommissionService.recalculateCommissions(
                collecteurId, startDate, endDate);

        String trackingId = UUID.randomUUID().toString();
        return ResponseEntity.accepted()
                .body("Recalculation started. Track with ID: " + trackingId);
    }
}