package org.example.collectfocep.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.JournalDTO;
import org.example.collectfocep.dto.JournalDuJourDTO;
import org.example.collectfocep.dto.MouvementJournalDTO;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.JournalMapper;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.JournalService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/journaux")
@Slf4j
public class JournalController {

    private final JournalService journalService;
    private final SecurityService securityService;
    private final JournalMapper journalMapper;
    private final MouvementRepository mouvementRepository;

    @Autowired
    public JournalController(JournalService journalService,
                             SecurityService securityService,
                             JournalMapper journalMapper,
                             MouvementRepository mouvementRepository) {
        this.journalService = journalService;
        this.securityService = securityService;
        this.journalMapper = journalMapper;
        this.mouvementRepository = mouvementRepository;
    }

    /**
     * ENDPOINT PRINCIPAL: Récupération automatique du journal du jour
     * Retourne le journal du jour avec toutes ses opérations
     */
    @GetMapping("/collecteur/{collecteurId}/jour")
    public ResponseEntity<ApiResponse<JournalDuJourDTO>> getJournalDuJour(
            @PathVariable Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate dateRecherche = date != null ? date : LocalDate.now();

        // Récupération du journal
        Journal journal = journalService.getOrCreateJournalDuJour(collecteurId, dateRecherche);

        // Récupération des opérations
        List<Mouvement> operations = mouvementRepository.findByCollecteurIdAndDateOperationBetween(
                collecteurId, dateRecherche.atStartOfDay(), dateRecherche.atTime(LocalTime.MAX));

        // Construction du DTO approprié
        JournalDuJourDTO response = JournalDuJourDTO.builder()
                .journalId(journal.getId())
                .collecteurId(collecteurId)
                .date(dateRecherche)
                .statut(journal.getStatut())
                .estCloture(journal.isEstCloture())
                .reference(journal.getReference())
                .nombreOperations(operations.size())
                .operations(operations.stream()
                        .map(this::convertToMouvementJournalDTO)
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Journal du jour récupéré avec succès"));
    }

    /**
     * ENDPOINT: Récupération du journal actif (aujourd'hui)
     */
    @GetMapping("/collecteur/{collecteurId}/actif")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<JournalDTO>> getJournalActif(@PathVariable Long collecteurId) {
        log.info("📅 Récupération journal actif pour collecteur: {}", collecteurId);

        try {
            Journal journal = journalService.getJournalActif(collecteurId);
            JournalDTO journalDTO = journalMapper.toDTO(journal);

            return ResponseEntity.ok(
                    ApiResponse.success(journalDTO, "Journal actif récupéré avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur récupération journal actif", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("JOURNAL_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * ENDPOINT: Clôture automatique du journal du jour
     */
    @PostMapping("/collecteur/{collecteurId}/cloture-jour")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    @Audited(action = "CLOTURE_JOUR", entityType = "Journal")
    public ResponseEntity<ApiResponse<JournalDTO>> cloturerJournalDuJour(
            @PathVariable Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("🔒 Clôture journal du jour - Collecteur: {}, Date: {}", collecteurId, date);

        try {
            LocalDate dateCloture = date != null ? date : LocalDate.now();
            Journal journal = journalService.cloturerJournalDuJour(collecteurId, dateCloture);

            JournalDTO journalDTO = journalMapper.toDTO(journal);

            return ResponseEntity.ok(
                    ApiResponse.success(journalDTO, "Journal du jour clôturé avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur clôture journal du jour", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("JOURNAL_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * MÉTHODES EXISTANTES CONSERVÉES POUR COMPATIBILITÉ
     */
    @GetMapping("/collecteur/{collecteurId}")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<List<Journal>> getJournauxByCollecteur(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        List<Journal> journaux = journalService.getJournauxByCollecteurAndDateRange(
                collecteurId, dateDebut, dateFin);
        return ResponseEntity.ok(journaux);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canAccessJournal(authentication, #id)")
    public ResponseEntity<ApiResponse<Journal>> getJournalById(@PathVariable Long id) {
        Journal journal = journalService.getJournalById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal non trouvé"));

        return ResponseEntity.ok(
                ApiResponse.success(journal, "Journal récupéré avec succès"));
    }

    /**
     * CONVERSION HELPER
     */
    private MouvementJournalDTO convertToMouvementJournalDTO(Mouvement mouvement) {
        return MouvementJournalDTO.builder()
                .id(mouvement.getId())
                .typeMouvement(mouvement.getTypeMouvement())  // ✅ Correction
                .montant(mouvement.getMontant())
                .sens(mouvement.getSens())
                .libelle(mouvement.getLibelle())
                .dateOperation(mouvement.getDateOperation())
                .clientNom(mouvement.getClient() != null ? mouvement.getClient().getNom() : null)
                .clientPrenom(mouvement.getClient() != null ? mouvement.getClient().getPrenom() : null)
                .build();
    }
}