package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ValidationResult;
import org.example.collectfocep.repositories.MouvementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoldeCollecteurValidationService {

    private final MouvementRepository mouvementRepository;

    public ValidationResult validateRetraitPossible(Long collecteurId, Double montantRetrait) {
        log.info("Validation retrait: collecteur={}, montant={}", collecteurId, montantRetrait);

        // 1. Calculer le solde journalier du collecteur
        Double soldeJournalier = calculateSoldeJournalierCollecteur(collecteurId);
        log.info("Solde journalier collecteur: {}", soldeJournalier);

        // 2. Vérifier si le collecteur a assez d'espèces
        if (montantRetrait > soldeJournalier) {
            return ValidationResult.failure(
                    "SOLDE_COLLECTEUR_INSUFFISANT",
                    String.format("Solde disponible: %.2f FCFA, Demandé: %.2f FCFA",
                            soldeJournalier, montantRetrait)
            );
        }

        // 3. Vérifier si le collecteur a fait au moins une épargne aujourd'hui
        boolean hasEpargneToday = hasEpargneAujourdhui(collecteurId);
        if (!hasEpargneToday) {
            return ValidationResult.failure(
                    "AUCUNE_EPARGNE_JOURNEE",
                    "Vous devez effectuer au moins une épargne avant tout retrait"
            );
        }

        return ValidationResult.success();
    }

    private Double calculateSoldeJournalierCollecteur(Long collecteurId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        // Somme des épargnes de la journée
        Double totalEpargne = mouvementRepository.sumByCollecteurAndSensAndPeriod(
                collecteurId, "EPARGNE", startOfDay, endOfDay
        );

        // Somme des retraits de la journée
        Double totalRetrait = mouvementRepository.sumByCollecteurAndSensAndPeriod(
                collecteurId, "RETRAIT", startOfDay, endOfDay
        );

        double solde = (totalEpargne != null ? totalEpargne : 0.0) -
                (totalRetrait != null ? totalRetrait : 0.0);

        log.debug("Collecteur {}: épargnes={}, retraits={}, solde={}",
                collecteurId, totalEpargne, totalRetrait, solde);

        return solde;
    }

    private boolean hasEpargneAujourdhui(Long collecteurId) {
        LocalDate today = LocalDate.now();
        Long countEpargne = mouvementRepository.countByCollecteurAndSensAndDate(
                collecteurId, "EPARGNE", today
        );
        return countEpargne != null && countEpargne > 0;
    }
}