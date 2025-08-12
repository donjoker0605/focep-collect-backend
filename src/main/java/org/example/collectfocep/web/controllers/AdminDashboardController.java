package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.DashboardStatsDTO;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminDashboardController {

    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final AgenceRepository agenceRepository;
    private final MouvementRepository mouvementRepository;
    private final CommissionRepository commissionRepository;
    private final SecurityService securityService;

    /**
     * ENDPOINT PRINCIPAL DU DASHBOARD ADMIN
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        log.info("📊 Récupération des statistiques du dashboard admin");

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Utilisateur connecté: {}, Autorités: {}",
                    auth.getName(), auth.getAuthorities());

            // Déterminer si c'est un admin d'agence ou super admin
            boolean isSuperAdmin = auth.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));

            DashboardStatsDTO stats;

            if (isSuperAdmin) {
                stats = buildGlobalStats();
                log.info("Statistiques globales générées pour super admin");
            } else {
                // GESTION SÉCURISÉE POUR ADMIN D'AGENCE
                try {
                    Long agenceId = securityService.getCurrentUserAgenceId();
                    if (agenceId == null) {
                        log.warn("Admin sans agence associée détecté");
                        // Retourner des stats vides plutôt qu'une erreur
                        stats = createEmptyStats("Admin sans agence");
                    } else {
                        stats = buildAgenceStats(agenceId);
                        log.info("Statistiques d'agence {} générées pour admin", agenceId);
                    }
                } catch (Exception e) {
                    log.error("Erreur récupération agence admin, utilisation stats vides", e);
                    stats = createEmptyStats("Erreur agence");
                }
            }

            return ResponseEntity.ok(
                    ApiResponse.success(stats, "Statistiques récupérées avec succès")
            );

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques dashboard", e);

            // RETOURNER DES STATS VIDES EN CAS D'ERREUR
            DashboardStatsDTO emptyStats = createEmptyStats("Erreur système");
            return ResponseEntity.ok(
                    ApiResponse.success(emptyStats, "Statistiques par défaut (erreur backend)")
            );
        }
    }

    /**
     * STATISTIQUES GLOBALES AVEC GESTION D'ERREURS
     */
    private DashboardStatsDTO buildGlobalStats() {
        log.debug("Construction des statistiques globales");

        try {
            // COLLECTEURS - AVEC GESTION D'ERREURS
            Long totalCollecteurs = safeCount(() -> collecteurRepository.count());
            Long collecteursActifs = safeCount(() -> collecteurRepository.countByActiveTrue());
            Long collecteursInactifs = totalCollecteurs - collecteursActifs;

            // CLIENTS - AVEC GESTION D'ERREURS
            Long totalClients = safeCount(() -> clientRepository.count());
            Long clientsActifs = safeCount(() -> clientRepository.countByValideTrue());
            Long clientsInactifs = totalClients - clientsActifs;

            // AGENCES
            Long agencesActives = safeCount(() -> agenceRepository.count());

            // MOUVEMENTS FINANCIERS - AVEC GESTION D'ERREURS
            Double totalEpargne = safeSum(() -> mouvementRepository.sumBySens("EPARGNE"));
            Double totalRetrait = safeSum(() -> mouvementRepository.sumBySens("RETRAIT"));

            // COMMISSIONS - AVEC GESTION D'ERREURS
            Long commissionsEnAttente = safeCount(() -> commissionRepository.countPendingCommissions());
            Double totalCommissions = safeSum(() -> commissionRepository.sumAllCommissions());

            // ALERTES SYSTÈME - AVEC GESTION D'ERREURS
            Long collecteursSansActivite = safeCount(() -> collecteurRepository.countInactiveCollecteurs());

            return DashboardStatsDTO.builder()
                    .totalCollecteurs(totalCollecteurs)
                    .totalClients(totalClients)
                    .agencesActives(agencesActives)
                    .collecteursActifs(collecteursActifs)
                    .collecteursInactifs(collecteursInactifs)
                    .clientsActifs(clientsActifs)
                    .clientsInactifs(clientsInactifs)
                    .totalEpargne(totalEpargne)
                    .totalRetrait(totalRetrait)
                    .commissionsEnAttente(commissionsEnAttente)
                    .totalCommissions(totalCommissions)
                    .collecteursSansActivite(collecteursSansActivite)
                    .lastUpdate(LocalDateTime.now())
                    .periode("Global")
                    .build();

        } catch (Exception e) {
            log.error("Erreur construction statistiques globales", e);
            return createEmptyStats("Erreur statistiques globales");
        }
    }

    /**
     * STATISTIQUES PAR AGENCE AVEC GESTION D'ERREURS
     */
    private DashboardStatsDTO buildAgenceStats(Long agenceId) {
        log.debug("Construction des statistiques pour l'agence: {}", agenceId);

        try {
            // COLLECTEURS DE L'AGENCE - AVEC GESTION D'ERREURS
            Long totalCollecteurs = safeCount(() -> collecteurRepository.countByAgenceId(agenceId));
            Long collecteursActifs = safeCount(() -> collecteurRepository.countByAgenceIdAndActiveTrue(agenceId));
            Long collecteursInactifs = totalCollecteurs - collecteursActifs;

            // CLIENTS DE L'AGENCE - AVEC GESTION D'ERREURS
            Long totalClients = safeCount(() -> clientRepository.countByAgenceId(agenceId));
            Long clientsActifs = safeCount(() -> clientRepository.countByAgenceIdAndValideTrue(agenceId));
            Long clientsInactifs = totalClients - clientsActifs;

            // MOUVEMENTS FINANCIERS DE L'AGENCE - AVEC GESTION D'ERREURS
            Double totalEpargne = safeSum(() -> mouvementRepository.sumByAgenceIdAndSens(agenceId, "EPARGNE"));
            Double totalRetrait = safeSum(() -> mouvementRepository.sumByAgenceIdAndSens(agenceId, "RETRAIT"));

            // COMMISSIONS DE L'AGENCE - AVEC GESTION D'ERREURS
            Long commissionsEnAttente = safeCount(() -> commissionRepository.countPendingCommissionsByAgence(agenceId));
            Double totalCommissions = safeSum(() -> commissionRepository.sumCommissionsByAgence(agenceId));

            return DashboardStatsDTO.builder()
                    .totalCollecteurs(totalCollecteurs)
                    .totalClients(totalClients)
                    .agencesActives(1L)
                    .collecteursActifs(collecteursActifs)
                    .collecteursInactifs(collecteursInactifs)
                    .clientsActifs(clientsActifs)
                    .clientsInactifs(clientsInactifs)
                    .totalEpargne(totalEpargne)
                    .totalRetrait(totalRetrait)
                    .commissionsEnAttente(commissionsEnAttente)
                    .totalCommissions(totalCommissions)
                    .lastUpdate(LocalDateTime.now())
                    .periode("Agence " + agenceId)
                    .build();

        } catch (Exception e) {
            log.error("Erreur construction statistiques agence {}", agenceId, e);
            return createEmptyStats("Erreur agence " + agenceId);
        }
    }

    /**
     * MÉTHODES UTILITAIRES POUR GESTION D'ERREURS
     */
    private Long safeCount(CountSupplier supplier) {
        try {
            Long result = supplier.get();
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.debug("Erreur lors du comptage: {}", e.getMessage());
            return 0L;
        }
    }

    private Double safeSum(SumSupplier supplier) {
        try {
            Double result = supplier.get();
            return result != null ? result : 0.0;
        } catch (Exception e) {
            log.debug("Erreur lors de la somme: {}", e.getMessage());
            return 0.0;
        }
    }

    private DashboardStatsDTO createEmptyStats(String periode) {
        return DashboardStatsDTO.builder()
                .totalCollecteurs(0L)
                .totalClients(0L)
                .agencesActives(0L)
                .collecteursActifs(0L)
                .collecteursInactifs(0L)
                .clientsActifs(0L)
                .clientsInactifs(0L)
                .totalEpargne(0.0)
                .totalRetrait(0.0)
                .commissionsEnAttente(0L)
                .totalCommissions(0.0)
                .collecteursSansActivite(0L)
                .lastUpdate(LocalDateTime.now())
                .periode(periode)
                .build();
    }

    /**
     * INTERFACES FONCTIONNELLES POUR GESTION D'ERREURS
     */
    @FunctionalInterface
    private interface CountSupplier {
        Long get() throws Exception;
    }

    @FunctionalInterface
    private interface SumSupplier {
        Double get() throws Exception;
    }

    /**
     * ENDPOINT DE DÉCONNEXION DASHBOARD ADMIN
     */
    @PostMapping("/logout")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Object> logoutAdmin() {
        log.info("👋 Demande de déconnexion depuis le dashboard admin");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth != null ? auth.getName() : "Utilisateur inconnu";
        
        log.info("✅ Déconnexion dashboard admin pour: {}", userEmail);
        
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Déconnexion réussie depuis le dashboard admin",
                "status", "success",
                "user", userEmail,
                "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * ENDPOINT DE DEBUG
     */
    @GetMapping("/dashboard-debug")
    public ResponseEntity<Object> getDashboardDebug() {
        log.info("🔧 DEBUG: Vérification de l'accès au dashboard admin");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity.ok(java.util.Map.of(
                "authenticated", auth != null && auth.isAuthenticated(),
                "principal", auth != null ? auth.getName() : "null",
                "authorities", auth != null ? auth.getAuthorities().toString() : "null",
                "details", auth != null ? auth.getDetails() : "null",
                "timestamp", LocalDateTime.now()
        ));
    }
}