package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.services.CollecteurMonitoringService;
import org.example.collectfocep.services.CollecteurMonitoringService.*;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 📊 Contrôleur pour le monitoring des collecteurs inactifs (SuperAdmin)
 */
@Slf4j
@RestController
@RequestMapping("/api/super-admin/monitoring/collecteurs")
@RequiredArgsConstructor
public class CollecteurMonitoringController {

    private final CollecteurMonitoringService monitoringService;

    /**
     * 📋 Liste des collecteurs inactifs
     */
    @GetMapping("/inactifs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<CollecteurInactiveInfo>>> getCollecteursInactifs() {
        log.info("🔍 [SuperAdmin] Récupération des collecteurs inactifs");
        
        try {
            List<CollecteurInactiveInfo> collecteursInactifs = monitoringService.getCollecteursInactifs();
            
            log.info("📊 Trouvé {} collecteurs inactifs", collecteursInactifs.size());
            
            return ResponseEntity.ok(ApiResponse.success(
                collecteursInactifs,
                String.format("Trouvé %d collecteur(s) inactif(s)", collecteursInactifs.size())
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des collecteurs inactifs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des collecteurs inactifs"));
        }
    }

    /**
     * 📈 Statistiques globales de monitoring
     */
    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MonitoringStatistics>> getMonitoringStatistics() {
        log.info("📈 [SuperAdmin] Récupération des statistiques de monitoring");
        
        try {
            MonitoringStatistics stats = monitoringService.getMonitoringStatistics();
            
            log.info("📊 Statistiques calculées: {}/{} collecteurs inactifs ({}%)", 
                    stats.getCollecteursInactifs(), 
                    stats.getTotalCollecteurs(),
                    String.format("%.1f", stats.getPourcentageInactivite()));
            
            return ResponseEntity.ok(ApiResponse.success(
                stats,
                "Statistiques de monitoring calculées"
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors du calcul des statistiques de monitoring", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du calcul des statistiques"));
        }
    }

    /**
     * 🏢 Collecteurs inactifs par agence
     */
    @GetMapping("/inactifs/agence/{agenceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<CollecteurInactiveInfo>>> getCollecteursInactifsByAgence(
            @PathVariable Long agenceId) {
        log.info("🏢 [SuperAdmin] Récupération des collecteurs inactifs pour l'agence {}", agenceId);
        
        try {
            List<CollecteurInactiveInfo> collecteursInactifs = 
                    monitoringService.getCollecteursInactifsByAgence(agenceId);
            
            log.info("📊 Trouvé {} collecteurs inactifs pour l'agence {}", 
                    collecteursInactifs.size(), agenceId);
            
            return ResponseEntity.ok(ApiResponse.success(
                collecteursInactifs,
                String.format("Trouvé %d collecteur(s) inactif(s) pour l'agence", collecteursInactifs.size())
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des collecteurs inactifs pour l'agence {}", agenceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des collecteurs inactifs"));
        }
    }

    /**
     * 🚨 Exécuter une action corrective
     */
    @PostMapping("/action-corrective/{collecteurId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ActionCorrectiveResult>> executeActionCorrective(
            @PathVariable Long collecteurId,
            @RequestParam TypeActionCorrective typeAction,
            @RequestParam(required = false, defaultValue = "Action manuelle SuperAdmin") String motif) {
        log.info("🚨 [SuperAdmin] Exécution action corrective {} pour collecteur {}", typeAction, collecteurId);
        
        try {
            ActionCorrectiveResult result = monitoringService.executeActionCorrective(
                    collecteurId, typeAction, motif);
            
            if (result.isSuccess()) {
                log.info("✅ Action corrective {} exécutée avec succès pour collecteur {}", 
                        typeAction, collecteurId);
                return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
            } else {
                log.warn("⚠️ Échec de l'action corrective {} pour collecteur {}: {}", 
                        typeAction, collecteurId, result.getMessage());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'exécution de l'action corrective {} pour collecteur {}", 
                    typeAction, collecteurId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de l'exécution de l'action corrective"));
        }
    }

    /**
     * 🔧 Types d'actions correctives disponibles
     */
    @GetMapping("/actions-correctives")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TypeActionCorrective[]>> getActionsCorrectivesDisponibles() {
        log.info("🔧 [SuperAdmin] Récupération des types d'actions correctives");
        
        try {
            TypeActionCorrective[] actions = TypeActionCorrective.values();
            return ResponseEntity.ok(ApiResponse.success(
                actions,
                "Types d'actions correctives récupérés"
            ));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des types d'actions correctives", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des types d'actions"));
        }
    }

    /**
     * 📊 Dashboard de monitoring consolidé
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MonitoringDashboard>> getMonitoringDashboard() {
        log.info("📊 [SuperAdmin] Récupération du dashboard de monitoring");
        
        try {
            MonitoringStatistics stats = monitoringService.getMonitoringStatistics();
            List<CollecteurInactiveInfo> collecteursInactifs = monitoringService.getCollecteursInactifs();
            
            MonitoringDashboard dashboard = MonitoringDashboard.builder()
                    .statistiques(stats)
                    .collecteursInactifs(collecteursInactifs)
                    .nombreAlertes(collecteursInactifs.size())
                    .actionsRecommandees(generateActionsRecommandees(collecteursInactifs))
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(
                dashboard,
                "Dashboard de monitoring généré"
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du dashboard de monitoring", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la génération du dashboard"));
        }
    }

    private List<String> generateActionsRecommandees(List<CollecteurInactiveInfo> collecteursInactifs) {
        return collecteursInactifs.stream()
                .limit(5) // Top 5 des recommandations
                .map(collecteur -> String.format(
                    "Collecteur %s (%s) - Inactif depuis %d jours - Action recommandée: %s",
                    collecteur.getNomComplet(),
                    collecteur.getAgenceNom(),
                    collecteur.getJoursInactivite(),
                    collecteur.getJoursInactivite() > 30 ? "Désactiver" : "Notifier"
                ))
                .toList();
    }

    // ================================
    // DTO pour le dashboard
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MonitoringDashboard {
        private MonitoringStatistics statistiques;
        private List<CollecteurInactiveInfo> collecteursInactifs;
        private int nombreAlertes;
        private List<String> actionsRecommandees;
    }
}