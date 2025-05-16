package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionProcessingResult;
import org.example.collectfocep.entities.CommissionRepartition;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.services.CommissionProcessingService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * Service asynchrone pour le traitement des commissions
 * Utilise la nouvelle architecture avec CommissionProcessingService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncCommissionService {

    private final CommissionProcessingService commissionProcessingService;
    private final CollecteurRepository collecteurRepository;

    /**
     * Traitement asynchrone des commissions pour un collecteur
     */
    @Async
    public CompletableFuture<CommissionProcessingResult> processCommissions(
            Long collecteurId, LocalDate startDate, LocalDate endDate) {

        log.info("Début traitement asynchrone des commissions - Collecteur: {}, Période: {} à {}",
                collecteurId, startDate, endDate);

        try {
            // Vérifier que le collecteur existe
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new RuntimeException("Collecteur non trouvé: " + collecteurId));

            // Déléguer le traitement au service principal
            CommissionProcessingResult result = commissionProcessingService
                    .processCommissionsForPeriod(collecteurId, startDate, endDate);

            log.info("Traitement asynchrone terminé avec succès pour collecteur {}", collecteurId);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Erreur lors du traitement asynchrone des commissions pour collecteur {}: {}",
                    collecteurId, e.getMessage(), e);

            return CompletableFuture.completedFuture(
                    CommissionProcessingResult.failure(collecteurId, e.getMessage())
            );
        }
    }

    /**
     * Recalcul forcé des commissions en mode asynchrone
     */
    @Async
    public CompletableFuture<CommissionProcessingResult> recalculateCommissions(
            Long collecteurId, LocalDate startDate, LocalDate endDate) {

        log.info("Début recalcul asynchrone des commissions - Collecteur: {}", collecteurId);

        try {
            CommissionProcessingResult result = commissionProcessingService
                    .recalculateCommissions(collecteurId, startDate, endDate, true);

            log.info("Recalcul asynchrone terminé avec succès pour collecteur {}", collecteurId);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Erreur lors du recalcul asynchrone pour collecteur {}: {}",
                    collecteurId, e.getMessage(), e);

            return CompletableFuture.completedFuture(
                    CommissionProcessingResult.failure(collecteurId, e.getMessage())
            );
        }
    }
}