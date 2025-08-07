package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.CompteAgence;
import org.example.collectfocep.entities.CompteManquant;
import org.example.collectfocep.entities.CompteServiceEntity;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.CompteAgenceRepository;
import org.example.collectfocep.repositories.CompteManquantRepository;
import org.example.collectfocep.repositories.CompteServiceRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.services.impl.CompteAgenceService;
import org.example.collectfocep.services.impl.VersementCollecteurService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 💰 Controller pour la gestion des versements collecteur
 * Version corrigée avec support du compte agence
 */
@RestController
@RequestMapping("/api/admin/versements")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class VersementCollecteurController {

    private final VersementCollecteurService versementService;
    private final CompteAgenceService compteAgenceService;
    private final CollecteurRepository collecteurRepository;
    private final CompteServiceRepository compteServiceRepository;
    private final CompteManquantRepository compteManquantRepository;
    private final CompteAgenceRepository compteAgenceRepository;

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
     * 📊 Récupérer TOUS les comptes d'un collecteur (incluant agence)
     */
    @GetMapping("/collecteur/{collecteurId}/comptes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CollecteurComptesDTO>> getCollecteurComptes(
            @PathVariable Long collecteurId) {

        log.info("📊 API: GET /admin/versements/collecteur/{}/comptes", collecteurId);

        try {
            Collecteur collecteur = collecteurRepository.findByIdWithAgence(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // Récupérer tous les comptes
            CompteServiceEntity compteService = compteServiceRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte service non trouvé"));

            CompteManquant compteManquant = compteManquantRepository.findFirstByCollecteur(collecteur)
                    .orElseThrow(() -> new ResourceNotFoundException("Compte manquant non trouvé"));

            CompteAgence compteAgence = compteAgenceService.ensureCompteAgenceExists(collecteur.getAgence());

            // Construire le DTO complet
            CollecteurComptesDTO comptes = CollecteurComptesDTO.builder()
                    .collecteurId(collecteurId)
                    .collecteurNom(collecteur.getNom())
                    .collecteurPrenom(collecteur.getPrenom())
                    .agenceNom(collecteur.getAgence().getNomAgence())
                    .agenceId(collecteur.getAgence().getId())

                    // Comptes collecteur
                    .compteServiceSolde(compteService.getSolde())
                    .compteServiceNumero(compteService.getNumeroCompte())
                    .compteManquantSolde(compteManquant.getSolde())
                    .compteManquantNumero(compteManquant.getNumeroCompte())
                    .compteAttenteSolde(0.0) // Pas utilisé dans nouvelle logique
                    .compteAttenteNumero("N/A")

                    // Compte agence
                    .compteAgenceSolde(compteAgence.getSolde())
                    .compteAgenceNumero(compteAgence.getNumeroCompte())
                    .build();

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
    public ResponseEntity<ApiResponse<ManquantsStatsDTO>> getManquantsStats(
            @PathVariable Long collecteurId) {

        log.info("📈 API: GET /admin/versements/collecteur/{}/manquants", collecteurId);

        try {
            CollecteurComptesDTO comptes = getCollecteurComptes(collecteurId).getBody().getData();

            ManquantsStatsDTO stats = ManquantsStatsDTO.builder()
                    .collecteurId(comptes.getCollecteurId())
                    .collecteurNom(comptes.getCollecteurNom())
                    .totalManquant(comptes.getCompteManquantSolde())
                    .totalAttente(comptes.getCompteAttenteSolde())
                    .soldeNet(comptes.getSoldeNet())
                    .hasManquant(comptes.getCompteManquantSolde() < 0) // Négatif = dette
                    .hasAttente(comptes.getCompteAttenteSolde() > 0)
                    .build();

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
    public ResponseEntity<ApiResponse<ClotureCheckDTO>> canCloseJournal(
            @RequestParam Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("🔍 API: GET /admin/versements/can-close - Collecteur: {}, Date: {}", collecteurId, date);

        try {
            ClotureJournalPreviewDTO preview = versementService.getCloturePreview(collecteurId, date);
            ClotureCheckDTO result = ClotureCheckDTO.fromPreview(preview);

            return ResponseEntity.ok(ApiResponse.success(result, "Vérification effectuée"));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CHECK_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * 🏥 Diagnostic des comptes agence (endpoint admin)
     */
    @GetMapping("/diagnostic/comptes-agence")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> diagnosticComptesAgence() {

        log.info("🏥 API: GET /admin/versements/diagnostic/comptes-agence");

        try {
            compteAgenceService.diagnosticComptesAgence();
            return ResponseEntity.ok(ApiResponse.success("OK", "Diagnostic effectué - voir logs"));

        } catch (Exception e) {
            log.error("❌ Erreur lors du diagnostic", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("DIAGNOSTIC_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * 📊 Statistiques globales des versements par agence
     */
    @GetMapping("/stats/agences")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getStatsVersementsAgences() {

        log.info("📊 API: GET /admin/versements/stats/agences");

        try {
            var stats = compteAgenceService.getStatistiquesVersements();
            return ResponseEntity.ok(ApiResponse.success(stats, "Statistiques agences récupérées"));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques agences", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("STATS_AGENCES_ERROR", "Erreur: " + e.getMessage()));
        }
    }
}