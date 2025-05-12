package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.services.impl.AsyncCommissionService;
import org.example.collectfocep.services.impl.AsyncReportService;
import org.example.collectfocep.dto.CommissionResult;
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

        CompletableFuture<CommissionResult> future = asyncCommissionService.processCommissions(
                collecteurId, startDate, endDate);

        // Return immediately with tracking ID
        String trackingId = UUID.randomUUID().toString();
        return ResponseEntity.accepted()
                .body("Processing started. Track with ID: " + trackingId);
    }

    @GetMapping("/status/{trackingId}")
    public ResponseEntity<CommissionResult> getProcessingStatus(@PathVariable String trackingId) {
        // Implement status tracking logic
        return ResponseEntity.ok(new CommissionResult());
    }
}
