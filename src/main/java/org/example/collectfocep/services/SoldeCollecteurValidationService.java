package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ValidationResult;
import org.example.collectfocep.dto.ActivityEvent;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.impl.AdminNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoldeCollecteurValidationService {

    private final MouvementRepository mouvementRepository;
    private final CollecteurRepository collecteurRepository;

    @Autowired
    private AdminNotificationService adminNotificationService;

    @Autowired
    private SecurityService securityService;


    public ValidationResult validateRetraitPossible(Long collecteurId, Double montantRetrait) {
        try {
            log.debug("🔍 Validation retrait: collecteur={}, montant={}", collecteurId, montantRetrait);

            // 1. Calcul solde corrigé
            Double soldeJournalier = calculateSoldeJournalierCollecteur(collecteurId); // ✅ CORRIGÉ NOM

            // 2. Vérification épargne
            if (!hasEpargneAujourdhui(collecteurId)) {
                return ValidationResult.error(
                        "AUCUNE_EPARGNE_JOURNEE",
                        "Vous devez effectuer au moins une épargne avant tout retrait"
                );
            }

            // 3. Vérification solde
            if (montantRetrait > soldeJournalier) {

                // Déclencher notification solde négatif si applicable
                Double soldeApresRetrait = soldeJournalier - montantRetrait;
                if (soldeApresRetrait < 0) {
                    // Notification asynchrone pour ne pas impacter performance
                    CompletableFuture.runAsync(() -> {
                        try {
                            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                            Long agenceId = getAgenceIdFromCollecteur(collecteurId);

                            //utiliser new ActivityEvent() + setters
                            ActivityEvent event = new ActivityEvent();
                            event.setType("SOLDE_COLLECTEUR_CHECK");
                            event.setCollecteurId(collecteurId);
                            event.setMontant(soldeApresRetrait);
                            event.setAgenceId(agenceId);
                            event.setTimestamp(LocalDateTime.now());

                            adminNotificationService.evaluateAndNotify(event);
                        } catch (Exception e) {
                            log.error("❌ Erreur notification solde: {}", e.getMessage());
                        }
                    });
                }

                return ValidationResult.error(
                        "SOLDE_COLLECTEUR_INSUFFISANT",
                        String.format("Solde insuffisant. Disponible: %,.0f FCFA, Demandé: %,.0f FCFA",
                                soldeJournalier, montantRetrait)
                );
            }

            return ValidationResult.success();

        } catch (Exception e) {
            log.error("❌ Erreur validation retrait: {}", e.getMessage(), e);
            return ValidationResult.error("VALIDATION_ERROR", "Erreur lors de la validation");
        }
    }

    private Long getAgenceIdFromCollecteur(Long collecteurId) {
        try {
            return collecteurRepository.findById(collecteurId)
                    .map(collecteur -> collecteur.getAgence().getId())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("⚠️ Impossible de récupérer agenceId pour collecteur: {}", collecteurId);
            return null;
        }
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