package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.services.VersementCollecteurService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/versements")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class VersementCollecteurController {

    private final VersementCollecteurService versementService;

    /**
     * 📋 Obtenir un aperçu avant clôture
     */
    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ClotureJournalPreviewDTO>> getCloturePreview(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("📋 API: GET /admin/versements/preview - Collecteur: {}, Date: {}", collecteurId, date);

        try {
            ClotureJournalPreviewDTO preview = versementService.getCloturePreview(collecteurId, date);
            return ResponseEntity.ok(ApiResponse.success(preview, "Aperçu généré avec succès"));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération de l'aperçu", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PREVIEW_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * 💰 Effectuer le versement et clôturer le journal
     */
    @PostMapping("/cloture")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = "CLOTURE_JOURNAL", entityType = "VersementCollecteur")
    public ResponseEntity<ApiResponse<VersementCollecteurResponseDTO>> effectuerVersementEtCloture(
            @Valid @RequestBody VersementCollecteurRequestDTO request) {

        log.info("💰 API: POST /admin/versements/cloture - Collecteur: {}, Date: {}, Montant: {}",
                request.getCollecteurId(), request.getDate(), request.getMontantVerse());

        try {
            VersementCollecteurResponseDTO versement = versementService.effectuerVersementEtCloture(request);

            String message = "Versement effectué et journal clôturé avec succès";
            if (versement.getManquant() != null && versement.getManquant() > 0) {
                message += String.format(". Manquant détecté: %.2f FCFA", versement.getManquant());
            } else if (versement.getExcedent() != null && versement.getExcedent() > 0) {
                message += String.format(". Excédent détecté: %.2f FCFA", versement.getExcedent());
            }

            return ResponseEntity.ok(ApiResponse.success(versement, message));

        } catch (Exception e) {
            log.error("❌ Erreur lors du versement et de la clôture", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VERSEMENT_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * 📊 Récupérer les comptes d'un collecteur
     */
    @GetMapping("/collecteur/{collecteurId}/comptes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CollecteurComptesDTO>> getCollecteurComptes(
            @PathVariable Long collecteurId) {

        log.info("📊 API: GET /admin/versements/collecteur/{}/comptes", collecteurId);

        try {
            CollecteurComptesDTO comptes = versementService.getCollecteurComptes(collecteurId);
            return ResponseEntity.ok(ApiResponse.success(comptes, "Comptes récupérés avec succès"));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des comptes", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("COMPTES_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * 📈 Statistiques des manquants d'un collecteur
     */
    @GetMapping("/collecteur/{collecteurId}/manquants")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getManquantsStats(
            @PathVariable Long collecteurId) {

        log.info("📈 API: GET /admin/versements/collecteur/{}/manquants", collecteurId);

        try {
            CollecteurComptesDTO comptes = versementService.getCollecteurComptes(collecteurId);

            // Construire les statistiques des manquants
            Object stats = new Object() {
                public final Long collecteurId = comptes.getCollecteurId();
                public final String collecteurNom = comptes.getCollecteurNom();
                public final Double totalManquant = comptes.getCompteManquantSolde();
                public final Double totalAttente = comptes.getCompteAttenteSolde();
                public final Double soldeNet = comptes.getSoldeNet();
                public final Boolean hasManquant = comptes.getCompteManquantSolde() > 0;
                public final Boolean hasAttente = comptes.getCompteAttenteSolde() > 0;
            };

            return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques manquants récupérées"));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques manquants", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("STATS_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * 🔍 Vérifier si une clôture est possible
     */
    @GetMapping("/can-close")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> canCloseJournal(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("🔍 API: GET /admin/versements/can-close - Collecteur: {}, Date: {}", collecteurId, date);

        try {
            ClotureJournalPreviewDTO preview = versementService.getCloturePreview(collecteurId, date);

            boolean canClose = preview.getJournalExiste() && !preview.getDejaClôture();
            String reason = "";

            if (!preview.getJournalExiste()) {
                reason = "Aucun journal trouvé pour cette date";
            } else if (preview.getDejaClôture()) {
                reason = "Le journal est déjà clôturé";
            }

            Object result = new Object() {
                public final boolean canClose = canClose;
                public final String reason = reason;
                public final Double montantAVerser = preview.getSoldeCompteService();
                public final Integer nombreOperations = preview.getNombreOperations();
                public final Boolean journalExiste = preview.getJournalExiste();
                public final Boolean dejaClôture = preview.getDejaClôture();
            };

            return ResponseEntity.ok(ApiResponse.success(result, "Vérification effectuée"));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CHECK_ERROR", "Erreur: " + e.getMessage()));
        }
    }
}