package org.example.collectfocep.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.JournalDTO;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.JournalMapper;
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
import java.util.List;

@RestController
@RequestMapping("/api/journaux")
@Slf4j
public class JournalController {
    private final JournalService journalService;
    private final SecurityService securityService;
    private final JournalMapper journalMapper;

    @Autowired
    public JournalController(JournalService journalService, SecurityService securityService, JournalMapper journalMapper) {
        this.journalService = journalService;
        this.securityService = securityService;
        this.journalMapper = journalMapper;
    }

    // ✅ NOUVEAU: Récupération automatique du journal du jour
    @GetMapping("/collecteur/{collecteurId}/jour")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<JournalDTO>> getJournalDuJour(
            @PathVariable Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("📅 Récupération journal du jour pour collecteur: {}, date: {}", collecteurId, date);

        try {
            LocalDate dateRecherche = date != null ? date : LocalDate.now();
            Journal journal = journalService.getOrCreateJournalDuJour(collecteurId, dateRecherche);

            JournalDTO journalDTO = journalMapper.toDTO(journal);

            return ResponseEntity.ok(
                    ApiResponse.success(journalDTO, "Journal du jour récupéré avec succès"));
        } catch (Exception e) {
            log.error("Erreur récupération journal du jour", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    // ✅ NOUVEAU: Récupération du journal actif (aujourd'hui)
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
            log.error("Erreur récupération journal actif", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    // Clôture automatique du journal du jour
    @PostMapping("/collecteur/{collecteurId}/cloture-jour")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    @Audited(action = "CLOTURE_JOUR", entityType = "Journal")
    public ResponseEntity<ApiResponse<JournalDTO>> cloturerJournalDuJour(
            @PathVariable Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date) {

        log.info("🔒 Clôture journal du jour pour collecteur: {}, date: {}", collecteurId, date);

        try {
            LocalDate dateCloture = date != null ? LocalDate.parse(date) : LocalDate.now();
            Journal journal = journalService.cloturerJournalDuJour(collecteurId, dateCloture);

            JournalDTO journalDTO = journalMapper.toDTO(journal);

            return ResponseEntity.ok(
                    ApiResponse.success(journalDTO, "Journal du jour clôturé avec succès"));
        } catch (Exception e) {
            log.error("Erreur clôture journal du jour", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    // MÉTHODES EXISTANTES CONSERVÉES
    @PostMapping
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #journalDTO.collecteurId)")
    @Audited(action = "CREATION", entityType = "Journal")
    public ResponseEntity<ApiResponse<JournalDTO>> createJournal(@Valid @RequestBody JournalDTO journalDTO) {
        log.info("Création d'un nouveau journal pour le collecteur ID: {}", journalDTO.getCollecteurId());

        Journal journal = journalMapper.toEntity(journalDTO);
        Journal savedJournal = journalService.saveJournal(journal);
        JournalDTO savedDTO = journalMapper.toDto(savedJournal);

        return ResponseEntity.ok(
                ApiResponse.success(savedDTO, "Journal créé avec succès"));
    }

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

    @GetMapping("/collecteur/{collecteurId}/page")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<Page<Journal>>> getJournauxByCollecteurPaginated(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateDebut") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Récupération paginée des journaux pour le collecteur: {} du {} au {}",
                collecteurId, dateDebut, dateFin);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Journal> journauxPage = journalService.getJournauxByCollecteurAndDateRange(
                collecteurId, dateDebut, dateFin, pageRequest);

        ApiResponse<Page<Journal>> response = ApiResponse.success(journauxPage);
        response.addMeta("totalElements", journauxPage.getTotalElements());
        response.addMeta("totalPages", journauxPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cloture")
    @PreAuthorize("@securityService.canManageJournal(authentication, #journalId)")
    @Audited(action = "CLOTURE", entityType = "Journal")
    public ResponseEntity<ApiResponse<Journal>> cloturerJournal(@RequestParam Long journalId) {
        Journal journal = journalService.cloturerJournal(journalId);

        return ResponseEntity.ok(
                ApiResponse.success(journal, "Journal clôturé avec succès"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canAccessJournal(authentication, #id)")
    public ResponseEntity<ApiResponse<Journal>> getJournalById(@PathVariable Long id) {
        Journal journal = journalService.getJournalById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal non trouvé"));

        return ResponseEntity.ok(
                ApiResponse.success(journal, "Journal récupéré avec succès"));
    }
}