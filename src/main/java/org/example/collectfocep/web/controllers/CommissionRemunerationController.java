package org.example.collectfocep.web.controllers;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.HistoriqueCalculCommission;
import org.example.collectfocep.repositories.HistoriqueCalculCommissionRepository;
import org.example.collectfocep.services.CommissionOrchestrator;
import org.example.collectfocep.services.ExcelReportGenerator;
import org.example.collectfocep.services.RemunerationProcessor;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur unifié pour le système de commission et rémunération FOCEP
 * Remplace les anciens contrôleurs dupliqués par une API cohérente
 */
@RestController
@RequestMapping("/api/v2/commission-remuneration")
@Slf4j
@RequiredArgsConstructor
public class CommissionRemunerationController {

    private final CommissionOrchestrator commissionOrchestrator;
    private final RemunerationProcessor remunerationProcessor;
    private final ExcelReportGenerator excelReportGenerator;
    private final HistoriqueCalculCommissionRepository historiqueCalculCommissionRepository;

    /**
     * Lance le calcul de commission complet pour un collecteur
     */
    @PostMapping("/collecteur/{collecteurId}/calculer")
    public ResponseEntity<?> calculerCommissions(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        
        log.info("Calcul commission - Collecteur: {}, Période: {} à {}", 
                collecteurId, dateDebut, dateFin);

        try {
            CommissionOrchestrator.CommissionResult result = 
                    commissionOrchestrator.processCommissions(collecteurId, dateDebut, dateFin);

            if (result.isSuccess()) {
                log.info("Commission calculée avec succès - S collecteur: {}", 
                        result.getMontantSCollecteur());
                return ResponseEntity.ok(result);
            } else {
                log.error("Échec calcul commission: {}", result.getErrorMessage());
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("COMMISSION_CALCULATION_FAILED", result.getErrorMessage()));
            }

        } catch (Exception e) {
            log.error("Erreur inattendue lors du calcul commission: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur système lors du calcul"));
        }
    }

    /**
     * Lance la rémunération d'un collecteur basée sur les commissions sélectionnées (NOUVEAU)
     */
    @PostMapping("/collecteur/{collecteurId}/remunerer")
    public ResponseEntity<?> remunererCollecteurAvecCommissions(
            @PathVariable Long collecteurId,
            @RequestBody RemunerationRequest request) {
        
        log.info("Rémunération collecteur V2 - ID: {}, Commissions: {}, Rubriques: {}", 
                 collecteurId, request.getCommissionIds(), request.getRubriques().size());

        try {
            // Calculer le montant S total et récupérer les dates de période
            java.math.BigDecimal montantS = calculateMontantSFromCommissions(request.getCommissionIds());
            PeriodInfo periodInfo = calculatePeriodFromCommissions(request.getCommissionIds());
            String effectuePar = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Utiliser le processeur avec période pour sauvegarder l'historique
            RemunerationProcessor.RemunerationResult result = 
                    remunerationProcessor.processRemunerationWithPeriod(
                        collecteurId, 
                        montantS,
                        periodInfo.dateDebut,
                        periodInfo.dateFin,
                        effectuePar
                    );

            if (result.isSuccess()) {
                // Marquer les commissions comme rémunérées avec l'ID de l'historique
                if (result.getHistoriqueRemunerationId() != null) {
                    markCommissionsAsRemunerated(request.getCommissionIds(), result.getHistoriqueRemunerationId());
                }
                
                log.info("Rémunération V2 effectuée - Vi total: {}, EMF: {}, Période: {} - {}", 
                        result.getTotalRubriqueVi(), result.getMontantEMF(), 
                        periodInfo.dateDebut, periodInfo.dateFin);
                return ResponseEntity.ok(result);
            } else {
                log.error("Échec rémunération V2: {}", result.getErrorMessage());
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("REMUNERATION_FAILED", result.getErrorMessage()));
            }

        } catch (Exception e) {
            log.error("Erreur inattendue lors de la rémunération V2: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur système lors de la rémunération"));
        }
    }

    /**
     * Lance la rémunération d'un collecteur (ancien endpoint)
     */
    @PostMapping("/collecteur/{collecteurId}/remunerer-simple")
    public ResponseEntity<?> remunererCollecteur(
            @PathVariable Long collecteurId,
            @RequestParam java.math.BigDecimal montantS) {
        
        log.info("Rémunération collecteur - ID: {}, S: {}", collecteurId, montantS);

        try {
            RemunerationProcessor.RemunerationResult result = 
                    remunerationProcessor.processRemuneration(collecteurId, montantS);

            if (result.isSuccess()) {
                log.info("Rémunération effectuée - Vi total: {}, EMF: {}", 
                        result.getTotalRubriqueVi(), result.getMontantEMF());
                return ResponseEntity.ok(result);
            } else {
                log.error("Échec rémunération: {}", result.getErrorMessage());
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("REMUNERATION_FAILED", result.getErrorMessage()));
            }

        } catch (Exception e) {
            log.error("Erreur inattendue lors de la rémunération: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur système lors de la rémunération"));
        }
    }

    /**
     * Processus complet : Commission + Rémunération en une seule API
     */
    @PostMapping("/collecteur/{collecteurId}/processus-complet")
    public ResponseEntity<?> processusComplet(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        
        log.info("Processus complet - Collecteur: {}, Période: {} à {}", 
                collecteurId, dateDebut, dateFin);

        try {
            // 1. Calcul des commissions
            CommissionOrchestrator.CommissionResult commissionResult = 
                    commissionOrchestrator.processCommissions(collecteurId, dateDebut, dateFin);

            if (!commissionResult.isSuccess()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("COMMISSION_FAILED", commissionResult.getErrorMessage()));
            }

            // 2. Rémunération basée sur S calculé avec validation de période
            String effectuePar = SecurityContextHolder.getContext().getAuthentication().getName();
            RemunerationProcessor.RemunerationResult remunerationResult = 
                    remunerationProcessor.processRemunerationWithPeriod(
                            collecteurId, 
                            commissionResult.getMontantSCollecteur(), 
                            dateDebut, 
                            dateFin,
                            effectuePar);

            if (!remunerationResult.isSuccess()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("REMUNERATION_FAILED", remunerationResult.getErrorMessage()));
            }

            // 3. Résultat consolidé
            ProcessusCompletResult result = ProcessusCompletResult.builder()
                    .collecteurId(collecteurId)
                    .periode(String.format("%s → %s", dateDebut, dateFin))
                    .commissionResult(commissionResult)
                    .remunerationResult(remunerationResult)
                    .success(true)
                    .build();

            log.info("Processus complet réussi - S: {}, Vi total: {}, EMF: {}", 
                    commissionResult.getMontantSCollecteur(),
                    remunerationResult.getTotalRubriqueVi(), 
                    remunerationResult.getMontantEMF());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Erreur processus complet: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur système"));
        }
    }

    /**
     * Génère le rapport Excel de commission
     */
    @PostMapping("/collecteur/{collecteurId}/rapport-commission")
    public ResponseEntity<byte[]> genererRapportCommission(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("Génération rapport commission Excel - Collecteur: {}", collecteurId);

        try {
            // 1. Calcul des commissions
            CommissionOrchestrator.CommissionResult commissionResult = 
                    commissionOrchestrator.processCommissions(collecteurId, dateDebut, dateFin);

            if (!commissionResult.isSuccess()) {
                return ResponseEntity.badRequest().build();
            }

            // 2. Génération Excel
            byte[] excelData = excelReportGenerator.generateCommissionReport(commissionResult);

            String fileName = String.format("rapport_commission_%d_%s_%s.xlsx", 
                    collecteurId, dateDebut, dateFin);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelData);

        } catch (IOException e) {
            log.error("Erreur génération rapport Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Génère le rapport Excel de rémunération complet
     */
    @PostMapping("/collecteur/{collecteurId}/rapport-remuneration")  
    public ResponseEntity<byte[]> genererRapportRemuneration(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("Génération rapport rémunération Excel - Collecteur: {}", collecteurId);

        try {
            // 1. Processus complet pour avoir toutes les données
            CommissionOrchestrator.CommissionResult commissionResult = 
                    commissionOrchestrator.processCommissions(collecteurId, dateDebut, dateFin);

            if (!commissionResult.isSuccess()) {
                return ResponseEntity.badRequest().build();
            }

            RemunerationProcessor.RemunerationResult remunerationResult = 
                    remunerationProcessor.processRemuneration(collecteurId, commissionResult.getMontantSCollecteur());

            if (!remunerationResult.isSuccess()) {
                return ResponseEntity.badRequest().build();
            }

            // 2. Génération Excel
            byte[] excelData = excelReportGenerator.generateRemunerationReport(remunerationResult, commissionResult);

            String fileName = String.format("rapport_remuneration_%d_%s_%s.xlsx", 
                    collecteurId, dateDebut, dateFin);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelData);

        } catch (IOException e) {
            log.error("Erreur génération rapport rémunération Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Récupère l'historique des rémunérations d'un collecteur
     */
    @GetMapping("/collecteur/{collecteurId}/historique-remuneration")
    public ResponseEntity<?> getHistoriqueRemuneration(@PathVariable Long collecteurId) {
        
        log.info("Récupération historique rémunération collecteur: {}", collecteurId);
        
        try {
            var historique = remunerationProcessor.getHistoriqueRemunerationDTO(collecteurId);
            log.info("Historique récupéré: {} éléments", historique.size());
            
            // Log détaillé pour debug
            for (var h : historique) {
                log.info("Historique ID {}: montantSInitial={}, totalRubriquesVi={}, effectuePar={}", 
                         h.getId(), h.getMontantSInitial(), h.getTotalRubriquesVi(), h.getEffectuePar());
            }
            
            return ResponseEntity.ok(ApiResponse.success(historique, 
                historique.size() + " historiques trouvés"));
            
        } catch (Exception e) {
            log.error("Erreur récupération historique: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur système"));
        }
    }
    
    /**
     * ENDPOINT DE TEST TEMPORAIRE - SANS AUTHENTIFICATION
     */
    @GetMapping("/debug/collecteur/{collecteurId}/historique-remuneration")
    public ResponseEntity<?> getHistoriqueRemunerationDebug(@PathVariable Long collecteurId) {
        
        log.info("🔍 DEBUG: Récupération historique rémunération collecteur: {}", collecteurId);
        
        try {
            var historique = remunerationProcessor.getHistoriqueRemunerationDTO(collecteurId);
            log.info("🔍 DEBUG: Historique récupéré: {} éléments", historique.size());
            
            // Log détaillé pour debug
            for (var h : historique) {
                log.info("🔍 DEBUG: Historique ID {}: montantSInitial={}, totalRubriquesVi={}, effectuePar={}", 
                         h.getId(), h.getMontantSInitial(), h.getTotalRubriquesVi(), h.getEffectuePar());
            }
            
            return ResponseEntity.ok(historique);
            
        } catch (Exception e) {
            log.error("🔍 DEBUG: Erreur récupération historique: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Vérifie si une rémunération existe déjà pour une période
     */
    @GetMapping("/collecteur/{collecteurId}/verifier-periode")
    public ResponseEntity<?> verifierPeriodeRemuneration(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        
        log.info("Vérification période rémunération - Collecteur: {}, Période: {} à {}", 
                collecteurId, dateDebut, dateFin);
        
        try {
            boolean existe = remunerationProcessor.remunerationExistsPourPeriode(collecteurId, dateDebut, dateFin);
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "collecteurId", collecteurId,
                "dateDebut", dateDebut,
                "dateFin", dateFin,
                "remunerationExiste", existe,
                "message", existe ? "Une rémunération existe déjà sur cette période" : "Période disponible"
            ));
            
        } catch (Exception e) {
            log.error("Erreur vérification période: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur système"));
        }
    }

    /**
     * Récupère le statut des commissions d'un collecteur
     */
    @GetMapping("/collecteur/{collecteurId}/statut")
    public ResponseEntity<?> getStatutCommissions(@PathVariable Long collecteurId) {
        
        // TODO: Implémenter la récupération du statut
        // Retourner les dernières commissions calculées, les rubriques actives, etc.
        
        return ResponseEntity.ok().body("Statut collecteur " + collecteurId);
    }

    /**
     * GESTION DES RUBRIQUES DE RÉMUNÉRATION
     */

    /**
     * Récupère les rubriques actives pour un collecteur
     */
    @GetMapping("/rubriques/collecteur/{collecteurId}")
    public ResponseEntity<?> getRubriquesCollecteur(@PathVariable Long collecteurId) {
        
        log.info("Récupération rubriques collecteur: {}", collecteurId);
        
        try {
            var rubriques = remunerationProcessor.getRubriquesCollecteur(collecteurId);
            return ResponseEntity.ok(rubriques);
            
        } catch (Exception e) {
            log.error("Erreur récupération rubriques: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur système"));
        }
    }

    /**
     * Crée une nouvelle rubrique de rémunération
     */
    @PostMapping("/rubriques")
    public ResponseEntity<?> createRubrique(@RequestBody Object rubriqueData) {
        
        log.info("Création rubrique: {}", rubriqueData);
        
        try {
            // TODO: Implémenter la création de rubrique
            return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "Rubrique créée avec succès",
                "data", rubriqueData
            ));
            
        } catch (Exception e) {
            log.error("Erreur création rubrique: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur création rubrique"));
        }
    }

    /**
     * Met à jour une rubrique de rémunération
     */
    @PutMapping("/rubriques/{rubriqueId}")
    public ResponseEntity<?> updateRubrique(
            @PathVariable Long rubriqueId, 
            @RequestBody Object rubriqueData) {
        
        log.info("Mise à jour rubrique: {} -> {}", rubriqueId, rubriqueData);
        
        try {
            // TODO: Implémenter la mise à jour de rubrique
            return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "Rubrique mise à jour avec succès",
                "rubriqueId", rubriqueId,
                "data", rubriqueData
            ));
            
        } catch (Exception e) {
            log.error("Erreur mise à jour rubrique: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur mise à jour rubrique"));
        }
    }

    /**
     * Désactive une rubrique de rémunération
     */
    @DeleteMapping("/rubriques/{rubriqueId}")
    public ResponseEntity<?> deleteRubrique(@PathVariable Long rubriqueId) {
        
        log.info("Désactivation rubrique: {}", rubriqueId);
        
        try {
            // TODO: Implémenter la désactivation de rubrique
            return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "Rubrique désactivée avec succès",
                "rubriqueId", rubriqueId
            ));
            
        } catch (Exception e) {
            log.error("Erreur désactivation rubrique: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("INTERNAL_ERROR", "Erreur désactivation rubrique"));
        }
    }

    /**
     * Calcule le montant S total à partir des IDs des commissions sélectionnées
     */
    private BigDecimal calculateMontantSFromCommissions(List<Long> commissionIds) {
        BigDecimal total = BigDecimal.ZERO;
        
        for (Long commissionId : commissionIds) {
            HistoriqueCalculCommission commission = historiqueCalculCommissionRepository
                    .findById(commissionId)
                    .orElseThrow(() -> new RuntimeException("Commission non trouvée: " + commissionId));
            
            if (commission.getMontantCommissionTotal() != null) {
                total = total.add(commission.getMontantCommissionTotal());
            }
        }
        
        log.info("Montant S calculé à partir de {} commissions: {}", commissionIds.size(), total);
        return total;
    }

    /**
     * Calcule la période globale à partir des commissions sélectionnées
     */
    private PeriodInfo calculatePeriodFromCommissions(List<Long> commissionIds) {
        LocalDate dateDebut = null;
        LocalDate dateFin = null;
        
        for (Long commissionId : commissionIds) {
            HistoriqueCalculCommission commission = historiqueCalculCommissionRepository
                    .findById(commissionId)
                    .orElseThrow(() -> new RuntimeException("Commission non trouvée: " + commissionId));
            
            if (dateDebut == null || commission.getDateDebut().isBefore(dateDebut)) {
                dateDebut = commission.getDateDebut();
            }
            if (dateFin == null || commission.getDateFin().isAfter(dateFin)) {
                dateFin = commission.getDateFin();
            }
        }
        
        log.info("Période calculée: {} - {} pour {} commissions", dateDebut, dateFin, commissionIds.size());
        return new PeriodInfo(dateDebut, dateFin);
    }

    /**
     * Marque les commissions comme rémunérées et lie à l'historique de rémunération
     */
    private void markCommissionsAsRemunerated(List<Long> commissionIds, Long remunerationId) {
        for (Long commissionId : commissionIds) {
            HistoriqueCalculCommission commission = historiqueCalculCommissionRepository
                    .findById(commissionId)
                    .orElseThrow(() -> new RuntimeException("Commission non trouvée: " + commissionId));
            
            // Utiliser la méthode de l'entité qui fait tout d'un coup
            commission.marquerCommeRemunere(remunerationId);
            historiqueCalculCommissionRepository.save(commission);
        }
        log.info("{} commissions marquées comme rémunérées avec remunerationId: {}", commissionIds.size(), remunerationId);
    }

    // Classes internes pour les réponses

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class PeriodInfo {
        private LocalDate dateDebut;
        private LocalDate dateFin;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RemunerationRequest {
        private List<Long> commissionIds;
        private List<Object> rubriques; // Pour l'instant, pas utilisé mais garde la compatibilité
        private String confirmationDateTime;
    }

    @lombok.Builder
    @lombok.Getter
    public static class ProcessusCompletResult {
        private Long collecteurId;
        private String periode;
        private CommissionOrchestrator.CommissionResult commissionResult;
        private RemunerationProcessor.RemunerationResult remunerationResult;
        private boolean success;
    }

    @lombok.AllArgsConstructor
    @lombok.Getter
    public static class ErrorResponse {
        private String code;
        private String message;
        private long timestamp;

        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(code, message, System.currentTimeMillis());
        }
    }
}