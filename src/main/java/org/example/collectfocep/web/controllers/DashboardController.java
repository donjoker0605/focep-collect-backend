package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.DashboardDTO;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.services.interfaces.CollecteurService;
import org.example.collectfocep.services.interfaces.DashboardService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/collecteurs")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CollecteurService collecteurService;

    @GetMapping("/{collecteurId}/dashboard")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboard(@PathVariable Long collecteurId) {
        log.info("Récupération du dashboard pour le collecteur: {}", collecteurId);

        // Vérifier que le collecteur existe
        Collecteur collecteur = collecteurService.getCollecteurById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé avec l'ID: " + collecteurId));

        // Construire les données du dashboard
        DashboardDTO dashboard = dashboardService.buildDashboard(collecteur);

        return ResponseEntity.ok(
                ApiResponse.success(dashboard, "Dashboard récupéré avec succès")
        );
    }
}