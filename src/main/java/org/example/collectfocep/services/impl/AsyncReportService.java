package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.services.JournalService;
import org.example.collectfocep.services.ReportService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncReportService {

    private final ReportService reportService;
    private final JournalService journalService;

    @Async
    public CompletableFuture<String> generateMonthlyReport(Long collecteurId, YearMonth month) {
        log.info("Starting async report generation for collecteur: {} for month: {}", collecteurId, month);
        try {
            // Gather data
            List<Journal> journalEntries = journalService.getMonthlyEntries(collecteurId, month);

            // Generate report
            String reportPath = reportService.generateMonthlyReport(collecteurId, journalEntries, month);

            return CompletableFuture.completedFuture(reportPath);
        } catch (Exception e) {
            log.error("Error generating report", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}