package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionContext;
import org.example.collectfocep.dto.CommissionDistribution;
import org.example.collectfocep.dto.CommissionProcessingResult;
import org.example.collectfocep.dto.CommissionRules;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.*;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service principal pour le traitement des commissions
 * Orchestre les opérations de calcul et de distribution
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CommissionProcessingService {

    private final CommissionCalculationEngine calculationEngine;
    private final CommissionDistributionEngine distributionEngine;
    private final CommissionValidationService validationService;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;

    /**
     * Traite les commissions pour un collecteur sur une période
     */
    @Retryable(value = {DataIntegrityViolationException.class}, maxAttempts = 3)
    public CommissionProcessingResult processCommissionsForPeriod(
            Long collecteurId,
            LocalDate startDate,
            LocalDate endDate) {

        log.info("Début traitement commissions - Collecteur: {}, Période: {} à {}",
                collecteurId, startDate, endDate);

        try {
            // 1. Validation et création du contexte
            var context = createAndValidateContext(collecteurId, startDate, endDate);

            // 2. Calcul des commissions en batch
            var calculations = calculationEngine.calculateBatch(context);
            log.info("Calcul terminé - {} commissions calculées", calculations.size());

            // 3. Distribution des commissions
            var distribution = distributionEngine.distribute(calculations, context);
            log.info("Distribution terminée - Rémunération collecteur: {}", distribution.getRemunerationCollecteur());

            // 4. Persistance atomique avec compensation
            var result = persistWithCompensation(distribution, context);

            log.info("Traitement commissions terminé avec succès pour collecteur {}", collecteurId);
            return result;

        } catch (Exception e) {
            log.error("Erreur lors du traitement des commissions pour collecteur {}: {}",
                    collecteurId, e.getMessage(), e);
            throw new CommissionProcessingException("Échec du traitement des commissions", e);
        }
    }

    /**
     * Traitement asynchrone des commissions
     */
    public CompletableFuture<CommissionProcessingResult> processCommissionsAsync(
            Long collecteurId, LocalDate startDate, LocalDate endDate) {

        return CompletableFuture.supplyAsync(() ->
                processCommissionsForPeriod(collecteurId, startDate, endDate));
    }

    /**
     * Traitement batch pour plusieurs collecteurs
     */
    public List<CommissionProcessingResult> processCommissionsForAgence(
            Long agenceId, LocalDate startDate, LocalDate endDate) {

        log.info("Traitement batch commissions pour agence {}", agenceId);

        List<Collecteur> collecteurs = collecteurRepository.findByAgenceId(agenceId);

        return collecteurs.parallelStream()
                .map(collecteur -> {
                    try {
                        return processCommissionsForPeriod(collecteur.getId(), startDate, endDate);
                    } catch (Exception e) {
                        log.error("Erreur pour collecteur {}: {}", collecteur.getId(), e.getMessage());
                        return CommissionProcessingResult.failure(collecteur.getId(), e.getMessage());
                    }
                })
                .toList();
    }

    /**
     * Recalcul des commissions avec validation préalable
     */
    @Transactional
    public CommissionProcessingResult recalculateCommissions(
            Long collecteurId, LocalDate startDate, LocalDate endDate, boolean forceRecalculation) {

        if (!forceRecalculation) {
            // Vérifier si des commissions existent déjà
            boolean hasExistingCommissions = calculationEngine.hasExistingCommissions(
                    collecteurId, startDate, endDate);

            if (hasExistingCommissions) {
                throw new CommissionAlreadyProcessedException(
                        "Des commissions existent déjà pour cette période. Utilisez forceRecalculation=true pour recalculer.");
            }
        }

        return processCommissionsForPeriod(collecteurId, startDate, endDate);
    }

    private CommissionContext createAndValidateContext(Long collecteurId, LocalDate startDate, LocalDate endDate) {
        // Validation des paramètres
        if (collecteurId == null) {
            throw new IllegalArgumentException("L'ID du collecteur est requis");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Les dates de début et fin sont requises");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin");
        }

        // Vérification que le collecteur existe
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        // Vérification que le collecteur est actif
        if (!collecteur.isActive()) {
            throw new InactiveCollecteurException("Le collecteur " + collecteurId + " est inactif");
        }

        // Création du contexte avec règles appropriées
        CommissionRules rules = CommissionRules.defaultRules();

        return CommissionContext.withRules(collecteurId, startDate, endDate, rules);
    }

    private CommissionProcessingResult persistWithCompensation(
            CommissionDistribution distribution, CommissionContext context) {

        try {
            // Début de la transaction compensation
            log.debug("Début persistance avec compensation");

            // Enregistrement des mouvements comptables
            distributionEngine.persistMovements(distribution);

            // Mise à jour des soldes des comptes
            distributionEngine.updateAccountBalances(distribution);

            // Enregistrement des commissions calculées
            calculationEngine.persistCalculations(distribution.getCalculations(), context);

            // Création du résultat
            return CommissionProcessingResult.success(
                    context.getCollecteurId(),
                    distribution.getCalculations(),
                    distribution.getTotalCommissions(),
                    distribution.getRemunerationCollecteur(),
                    distribution.getPartEMF(),
                    distribution.getTotalTVA()
            );

        } catch (Exception e) {
            log.error("Erreur lors de la persistance, début compensation: {}", e.getMessage());

            // Logique de compensation (rollback personnalisé si nécessaire)
            try {
                compensateFailedPersistence(distribution, context);
            } catch (Exception compensationError) {
                log.error("Erreur lors de la compensation: {}", compensationError.getMessage());
                // Dans ce cas, la transaction Spring va faire le rollback
            }

            throw new CommissionPersistenceException("Échec de la persistance des commissions", e);
        }
    }

    private void compensateFailedPersistence(CommissionDistribution distribution, CommissionContext context) {
        // Logique de compensation si nécessaire
        // Par exemple: annuler des opérations externes, notifier des services, etc.
        log.info("Compensation des opérations échouées pour collecteur {}", context.getCollecteurId());
    }

    /**
     * Expose le moteur de calcul pour les contrôleurs
     */
    public CommissionCalculationEngine getCalculationEngine() {
        return calculationEngine;
    }
}