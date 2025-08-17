package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.HistoriqueCalculCommission;
import org.example.collectfocep.dto.HistoriqueCalculCommissionDTO;
import org.example.collectfocep.repositories.HistoriqueCalculCommissionRepository;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔥 NOUVEAU CONTROLLER: Historique des calculs de commission
 * 
 * Endpoints pour:
 * - Consulter l'historique des calculs par collecteur
 * - Récupérer les calculs non rémunérés
 * - Statistiques des calculs
 */
@RestController
@RequestMapping("/api/v2/historique-commissions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HistoriqueCommissionController {

    private final HistoriqueCalculCommissionRepository historiqueRepository;

    /**
     * Récupère l'historique des calculs pour un collecteur
     * USAGE: Affichage des calculs précédents dans l'interface admin
     */
    @GetMapping("/collecteur/{collecteurId}")
    public ResponseEntity<ApiResponse<List<HistoriqueCalculCommissionDTO>>> getHistoriqueCollecteur(
            @PathVariable Long collecteurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            log.info("📊 Récupération historique collecteur: {}", collecteurId);
            
            if (page < 0) {
                // Récupération complète sans pagination
                List<HistoriqueCalculCommission> historique = historiqueRepository
                        .findByCollecteurIdOrderByDateCalculDesc(collecteurId);
                
                List<HistoriqueCalculCommissionDTO> historiqueDTO = historique.stream()
                        .map(HistoriqueCalculCommissionDTO::fromEntity)
                        .collect(Collectors.toList());
                
                return ResponseEntity.ok(ApiResponse.success(
                    historiqueDTO,
                    String.format("Historique complet trouvé (%d calculs)", historiqueDTO.size())
                ));
            } else {
                // Récupération paginée
                Pageable pageable = PageRequest.of(page, size);
                Page<HistoriqueCalculCommission> historiquePage = historiqueRepository
                        .findByCollecteurId(collecteurId, pageable);
                
                List<HistoriqueCalculCommissionDTO> historiqueDTO = historiquePage.getContent().stream()
                        .map(HistoriqueCalculCommissionDTO::fromEntity)
                        .collect(Collectors.toList());
                
                return ResponseEntity.ok(ApiResponse.success(
                    historiqueDTO,
                    String.format("Page %d/%d trouvée (%d/%d calculs)", 
                                  page + 1, 
                                  historiquePage.getTotalPages(),
                                  historiqueDTO.size(),
                                  historiquePage.getTotalElements())
                ));
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur récupération historique collecteur {}: {}", collecteurId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la récupération de l'historique"));
        }
    }

    /**
     * Récupère les calculs NON RÉMUNÉRÉS pour un collecteur
     * CRITIQUE: Utilisé par le processus de rémunération
     */
    @GetMapping("/collecteur/{collecteurId}/non-remuneres")
    public ResponseEntity<ApiResponse<List<HistoriqueCalculCommissionDTO>>> getCalculsNonRemuneres(
            @PathVariable Long collecteurId) {
        
        try {
            log.info("💰 Récupération calculs non rémunérés - collecteur: {}", collecteurId);
            
            List<HistoriqueCalculCommission> calculsNonRemuneres = historiqueRepository
                    .findNonRemuneresForCollecteur(collecteurId);
            
            List<HistoriqueCalculCommissionDTO> calculsDTO = calculsNonRemuneres.stream()
                    .map(HistoriqueCalculCommissionDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(
                calculsDTO,
                String.format("%d calculs non rémunérés trouvés", calculsDTO.size())
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur récupération calculs non rémunérés collecteur {}: {}", 
                      collecteurId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la récupération des calculs non rémunérés"));
        }
    }

    /**
     * Récupère un calcul spécifique par ID
     */
    @GetMapping("/{historiqueId}")
    public ResponseEntity<ApiResponse<HistoriqueCalculCommissionDTO>> getCalculById(
            @PathVariable Long historiqueId) {
        
        try {
            log.info("🔍 Récupération calcul ID: {}", historiqueId);
            
            return historiqueRepository.findById(historiqueId)
                .map(historique -> {
                    HistoriqueCalculCommissionDTO dto = HistoriqueCalculCommissionDTO.fromEntity(historique);
                    return ResponseEntity.ok(ApiResponse.success(dto, "Calcul trouvé"));
                })
                .orElse(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            log.error("❌ Erreur récupération calcul {}: {}", historiqueId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la récupération du calcul"));
        }
    }

    /**
     * Statistiques rapides des calculs
     */
    @GetMapping("/stats/collecteur/{collecteurId}")
    public ResponseEntity<ApiResponse<CalculStatsDTO>> getStatsCollecteur(
            @PathVariable Long collecteurId) {
        
        try {
            log.info("📈 Récupération stats collecteur: {}", collecteurId);
            
            List<HistoriqueCalculCommission> tousCalculs = historiqueRepository
                    .findByCollecteurIdOrderByDateCalculDesc(collecteurId);
            
            long totalCalculs = tousCalculs.size();
            long calculsNonRemuneres = historiqueRepository.countNonRemuneresForCollecteur(collecteurId);
            long calculsRemuneres = totalCalculs - calculsNonRemuneres;
            
            HistoriqueCalculCommissionDTO dernierCalculDTO = tousCalculs.isEmpty() ? 
                    null : HistoriqueCalculCommissionDTO.fromEntity(tousCalculs.get(0));

            CalculStatsDTO stats = CalculStatsDTO.builder()
                    .totalCalculs(totalCalculs)
                    .calculsRemuneres(calculsRemuneres)
                    .calculsNonRemuneres(calculsNonRemuneres)
                    .dernierCalcul(dernierCalculDTO)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(
                stats,
                "Statistiques calculées"
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur calcul stats collecteur {}: {}", collecteurId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors du calcul des statistiques"));
        }
    }

    /**
     * Récupère les derniers calculs (dashboard global)
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<HistoriqueCalculCommissionDTO>>> getRecentCalculs(
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            log.info("🕒 Récupération {} derniers calculs", limit);
            
            Pageable pageable = PageRequest.of(0, limit);
            Page<HistoriqueCalculCommission> recentCalculs = historiqueRepository
                    .findLatestCalculs(pageable);
            
            List<HistoriqueCalculCommissionDTO> recentCalculsDTO = recentCalculs.getContent().stream()
                    .map(HistoriqueCalculCommissionDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(
                recentCalculsDTO,
                String.format("%d derniers calculs trouvés", recentCalculsDTO.size())
            ));
            
        } catch (Exception e) {
            log.error("❌ Erreur récupération calculs récents: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la récupération des calculs récents"));
        }
    }

    // =====================================
    // DTO POUR STATISTIQUES
    // =====================================

    @lombok.Builder
    @lombok.Getter
    public static class CalculStatsDTO {
        private long totalCalculs;
        private long calculsRemuneres;
        private long calculsNonRemuneres;
        private HistoriqueCalculCommissionDTO dernierCalcul;
        
        public double getPourcentageRemunere() {
            return totalCalculs > 0 ? (calculsRemuneres * 100.0) / totalCalculs : 0.0;
        }
    }
}