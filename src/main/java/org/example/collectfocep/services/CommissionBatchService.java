package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionBatchService {

    private final CommissionCalculationService commissionCalculationService;
    private final CollecteurRepository collecteurRepository;

    @Scheduled(cron = "0 0 1 * * ?") // Premier jour de chaque mois à minuit
    @Transactional
    public void calculerCommissionsMensuelles() {
        log.info("Début du calcul mensuel des commissions");

        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusMonths(1).withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(1).minusDays(1);

        log.info("Calcul des commissions pour la période: {} à {}", startDate, endDate);

        collecteurRepository.findAll().forEach(collecteur -> {
            try {
                log.info("Calcul des commissions pour le collecteur: {}", collecteur.getId());
                commissionCalculationService.calculateCommissions(
                        collecteur.getId(),
                        startDate,
                        endDate
                );
            } catch (Exception e) {
                log.error("Erreur lors du calcul des commissions pour le collecteur {}: {}",
                        collecteur.getId(), e.getMessage());
            }
        });

        log.info("Fin du calcul mensuel des commissions");
    }
}