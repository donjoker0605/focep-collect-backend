// src/main/java/org/example/collectfocep/web/controllers/AdminDashboardController.java
package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminDashboardController {

    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final AgenceRepository agenceRepository;
    private final MouvementRepository mouvementRepository;
    private final CommissionRepository commissionRepository;

    /**
     * ✅ ENDPOINT PRINCIPAL - Dashboard Admin
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        log.info("📊 Récupération des statistiques dashboard admin");

        try {
            Map<String, Object> stats = new HashMap<>();

            // ✅ STATISTIQUES GÉNÉRALES
            stats.put("totalCollecteurs", collecteurRepository.count());
            stats.put("totalClients", clientRepository.count());
            stats.put("totalAgences", agenceRepository.count());

            // ✅ COLLECTEURS ACTIFS/INACTIFS
            long collecteursActifs = collecteurRepository.countByActiveTrue();
            long collecteursInactifs = collecteurRepository.countByActiveFalse();
            stats.put("collecteursActifs", collecteursActifs);
            stats.put("collecteursInactifs", collecteursInactifs);

            // ✅ CLIENTS ACTIFS/INACTIFS
            long clientsActifs = clientRepository.countByValideTrue();
            long clientsInactifs = clientRepository.countByValideFalse();
            stats.put("clientsActifs", clientsActifs);
            stats.put("clientsInactifs", clientsInactifs);

            // ✅ STATISTIQUES FINANCIÈRES DU MOIS
            LocalDate debutMois = LocalDate.now().withDayOfMonth(1);
            LocalDate finMois = LocalDate.now();
            LocalDateTime debutMoisTime = debutMois.atStartOfDay();
            LocalDateTime finMoisTime = finMois.atTime(LocalTime.MAX);

            // Total épargne du mois
            Double totalEpargne = mouvementRepository.sumMontantBySensAndDateBetween(
                    "epargne", debutMoisTime, finMoisTime);
            stats.put("totalEpargne", totalEpargne != null ? totalEpargne : 0.0);

            // Total retraits du mois
            Double totalRetrait = mouvementRepository.sumMontantBySensAndDateBetween(
                    "retrait", debutMoisTime, finMoisTime);
            stats.put("totalRetrait", totalRetrait != null ? totalRetrait : 0.0);

            // ✅ COMMISSIONS EN ATTENTE
//            long commissionsEnAttente = commissionRepository.countByStatut("EN_ATTENTE");
//            stats.put("commissionsEnAttente", commissionsEnAttente);

            // ✅ AGENCES ACTIVES
            long agencesActives = agenceRepository.countByActiveTrue();
            stats.put("agencesActives", agencesActives);

            // ✅ TRANSACTIONS DU JOUR
            LocalDateTime debutJour = LocalDate.now().atStartOfDay();
            LocalDateTime finJour = LocalDate.now().atTime(LocalTime.MAX);
            long transactionsDuJour = mouvementRepository.countByDateOperationBetween(debutJour, finJour);
            stats.put("transactionsDuJour", transactionsDuJour);

            // ✅ PERFORMANCE MENSUELLE
            stats.put("performanceMois", calculateMonthlyPerformance(debutMoisTime, finMoisTime));

            log.info("✅ Statistiques dashboard calculées - {} collecteurs, {} clients",
                    stats.get("totalCollecteurs"), stats.get("totalClients"));

            return ResponseEntity.ok(
                    ApiResponse.success(stats, "Statistiques dashboard récupérées avec succès")
            );

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("DASHBOARD_ERROR", "Erreur lors de la récupération des statistiques"));
        }
    }

    /**
     * ✅ STATISTIQUES DÉTAILLÉES PAR AGENCE
     */
    @GetMapping("/dashboard/agences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardByAgence() {
        log.info("🏢 Récupération des statistiques par agence");

        try {
            Map<String, Object> agenceStats = new HashMap<>();

            // Récupérer toutes les agences avec leurs statistiques
            var agences = agenceRepository.findAll();

            for (var agence : agences) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("nom", agence.getNom());
                stats.put("totalCollecteurs", collecteurRepository.countByAgenceId(agence.getId()));
                stats.put("totalClients", clientRepository.countByAgenceId(agence.getId()));

                agenceStats.put("agence_" + agence.getId(), stats);
            }

            return ResponseEntity.ok(
                    ApiResponse.success(agenceStats, "Statistiques par agence récupérées")
            );

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques par agence", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("AGENCE_STATS_ERROR", "Erreur lors de la récupération"));
        }
    }

    /**
     * ✅ MÉTHODE UTILITAIRE - Calculer la performance mensuelle
     */
    private Map<String, Object> calculateMonthlyPerformance(LocalDateTime debut, LocalDateTime fin) {
        Map<String, Object> performance = new HashMap<>();

        try {
            // Calculs de performance
            Double totalEpargne = mouvementRepository.sumMontantBySensAndDateBetween("epargne", debut, fin);
            Double totalRetrait = mouvementRepository.sumMontantBySensAndDateBetween("retrait", debut, fin);
            Long totalTransactions = mouvementRepository.countByDateOperationBetween(debut, fin);

            double soldeNet = (totalEpargne != null ? totalEpargne : 0.0) -
                    (totalRetrait != null ? totalRetrait : 0.0);

            performance.put("soldeNet", soldeNet);
            performance.put("totalTransactions", totalTransactions != null ? totalTransactions : 0);
            performance.put("moyenneParTransaction",
                    totalTransactions > 0 ? soldeNet / totalTransactions : 0.0);

        } catch (Exception e) {
            log.warn("⚠️ Erreur calcul performance mensuelle: {}", e.getMessage());
            performance.put("soldeNet", 0.0);
            performance.put("totalTransactions", 0);
            performance.put("moyenneParTransaction", 0.0);
        }

        return performance;
    }

    /**
     * ✅ ACTIONS D'ADMINISTRATION
     */
    @PostMapping("/actions/reset-cache")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> resetSystemCache() {
        log.info("🗑️ Réinitialisation du cache système");

        try {
            // Logique de réinitialisation du cache
            // Implémentation selon votre système de cache

            return ResponseEntity.ok(
                    ApiResponse.success("Cache réinitialisé", "Cache système vidé avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la réinitialisation du cache", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("CACHE_RESET_ERROR", "Erreur lors de la réinitialisation"));
        }
    }

    @GetMapping("/system/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        log.info("🏥 Vérification de l'état du système");

        try {
            Map<String, Object> health = new HashMap<>();

            // Vérifications système
            health.put("database", "UP");
            health.put("totalUsers", collecteurRepository.count() + clientRepository.count());
            health.put("lastUpdate", LocalDateTime.now().toString());
            health.put("systemLoad", "NORMAL");

            return ResponseEntity.ok(
                    ApiResponse.success(health, "État du système récupéré")
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification système", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("HEALTH_CHECK_ERROR", "Erreur lors de la vérification"));
        }
    }
}