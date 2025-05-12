package org.example.collectfocep.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.services.CommissionCalculationService;
import org.example.collectfocep.services.CommissionRepartitionService;
import org.example.collectfocep.services.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AsyncCommissionService {

    private final CommissionCalculationService commissionCalculationService;
    private final CommissionRepartitionService commissionRepartitionService;
    private final ReportService reportService;
    private final CollecteurRepository collecteurRepository;

    @Autowired
    public AsyncCommissionService(
            CommissionCalculationService commissionCalculationService,
            CommissionRepartitionService commissionRepartitionService,
            ReportService reportService,
            CollecteurRepository collecteurRepository) {
        this.commissionCalculationService = commissionCalculationService;
        this.commissionRepartitionService = commissionRepartitionService;
        this.reportService = reportService;
        this.collecteurRepository = collecteurRepository;
    }

    @Async
    public CompletableFuture<CommissionResult> processCommissions(Long collecteurId, LocalDate startDate, LocalDate endDate) {
        log.info("Starting async commission processing for collecteur: {}", collecteurId);
        try {
            // Calculate commissions
            CommissionResult result = commissionCalculationService.calculateCommissions(collecteurId, startDate, endDate);

            // Process repartition
            commissionRepartitionService.processRepartition(result);

            // Pour le rapport, nous pouvons simplement renvoyer le résultat pour l'instant
            // Une méthode complète generateCommissionReport pourrait être implémentée dans ReportService

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error processing commissions", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}