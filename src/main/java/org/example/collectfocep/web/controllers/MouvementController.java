package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedAgencyAccessException;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.mappers.MouvementMapper;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.JournalRepository;
import org.example.collectfocep.services.impl.MouvementServiceImpl;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.JournalService; // ✅ IMPORT DÉJÀ PRÉSENT
import org.example.collectfocep.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/mouvements")
@Slf4j
public class MouvementController {

    private final SecurityService securityService;
    private final ClientRepository clientRepository;
    private final JournalRepository journalRepository;
    private final MouvementMapper mouvementMapper;
    private final JournalService journalService; // ✅ AJOUTER CETTE LIGNE

    @Autowired
    private MouvementServiceImpl mouvementServiceImpl;

    @Value("${app.mouvement.use-projection:true}")
    private boolean useProjection;

    @Autowired
    public MouvementController(
            MouvementServiceImpl mouvementServiceImpl,
            SecurityService securityService,
            ClientRepository clientRepository,
            JournalRepository journalRepository,
            MouvementMapper mouvementMapper,
            JournalService journalService) {
        this.mouvementServiceImpl = mouvementServiceImpl;
        this.securityService = securityService;
        this.clientRepository = clientRepository;
        this.journalRepository = journalRepository;
        this.mouvementMapper = mouvementMapper;
        this.journalService = journalService;
    }

    @PostMapping("/epargne")
    @PreAuthorize("@securityService.canManageClient(authentication, #request.clientId)")
    public ResponseEntity<ApiResponse<MouvementCommissionDTO>> effectuerEpargne(@Valid @RequestBody EpargneRequest request) {
        log.info("Traitement d'une opération d'épargne pour le client: {}", request.getClientId());
        try {
            // Validation des autorisations
            if (!securityService.canManageClient(SecurityContextHolder.getContext().getAuthentication(),
                    request.getClientId())) {
                throw new UnauthorizedException("Non autorisé à gérer ce client");
            }

            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

            // Vérifier que le client appartient à l'agence du collecteur
            if (!securityService.isClientInCollecteurAgence(request.getClientId(), request.getCollecteurId())) {
                throw new UnauthorizedAgencyAccessException("Client n'appartient pas à votre agence");
            }

            // Enregistrer l'épargne
            Mouvement mouvement = mouvementServiceImpl.enregistrerEpargne(client, request.getMontant(), null);

            // Convertir en DTO pour la réponse
            MouvementCommissionDTO responseDto = mouvementMapper.toCommissionDto(mouvement);

            return ResponseEntity.ok(
                    ApiResponse.success(responseDto, "Opération d'épargne enregistrée avec succès")
            );
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'épargne", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("EPARGNE_ERROR", "Erreur lors de l'enregistrement de l'épargne: " + e.getMessage()));
        }
    }

    @PostMapping("/retrait")
    @PreAuthorize("@securityService.canManageClient(authentication, #request.clientId)")
    public ResponseEntity<ApiResponse<MouvementCommissionDTO>> effectuerRetrait(@Valid @RequestBody RetraitRequest request) {
        log.info("Traitement d'une opération de retrait pour le client: {}", request.getClientId());
        try {
            // Vérification explicite des autorisations
            if (!securityService.canManageClient(SecurityContextHolder.getContext().getAuthentication(),
                    request.getClientId())) {
                throw new UnauthorizedException("Non autorisé à gérer ce client");
            }

            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

            if (!securityService.isClientInCollecteurAgence(request.getClientId(), request.getCollecteurId())) {
                throw new UnauthorizedAgencyAccessException("Client n'appartient pas à votre agence");
            }

            Mouvement mouvement = mouvementServiceImpl.enregistrerRetrait(client, request.getMontant(), null);

            // FIX PRINCIPAL : Utiliser DTO au lieu de l'entité brute
            MouvementCommissionDTO responseDto = mouvementMapper.toCommissionDto(mouvement);

            return ResponseEntity.ok(
                    ApiResponse.success(responseDto, "Retrait effectué avec succès")
            );
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du retrait", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("RETRAIT_ERROR", "Erreur lors de l'enregistrement du retrait: " + e.getMessage()));
        }
    }

    @GetMapping("/journal/{journalId}")
    public ResponseEntity<ApiResponse<List<MouvementCommissionDTO>>> getMouvementsByJournal(
            @PathVariable Long journalId,
            @RequestParam(defaultValue = "auto") String strategy) {

        try {
            List<MouvementCommissionDTO> mouvementDTOs;

            // Stratégie configurable
            if ("projection".equals(strategy) || (useProjection && "auto".equals(strategy))) {
                // Approche projection (plus rapide)
                mouvementDTOs = mouvementServiceImpl.findMouvementsDtoByJournalId(journalId);
            } else {
                // Approche entity avec JOIN FETCH (plus flexible)
                mouvementDTOs = mouvementServiceImpl.convertToDto(
                        mouvementServiceImpl.findByJournalIdWithAccounts(journalId)
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.success(mouvementDTOs, "Mouvements récupérés avec succès")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("MOUVEMENT_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/journal/transactions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<Page<MouvementDTO>>> getJournalTransactions(
            @RequestParam Long collecteurId,
            @RequestParam String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateOperation,desc") String sort) {

        log.info("Récupération des transactions du journal pour collecteur: {} à la date: {}", collecteurId, date);

        try {
            // FIX CRITIQUE : Forcer le nom de champ correct
            String[] sortParts = sort.split(",");
            String sortField = sortParts[0];
            String sortDirection = sortParts.length > 1 ? sortParts[1] : "desc";

            // SÉCURITÉ : Remplacer dateHeure par dateOperation si présent
            if ("dateHeure".equals(sortField)) {
                sortField = "dateOperation";
                log.warn("Paramètre de tri 'dateHeure' détecté et remplacé par 'dateOperation'");
            }

            // VALIDATION : S'assurer que le champ existe
            if (!"dateOperation".equals(sortField) && !"montant".equals(sortField) && !"id".equals(sortField)) {
                log.warn("Champ de tri non reconnu: {}. Utilisation de 'dateOperation' par défaut", sortField);
                sortField = "dateOperation";
            }

            PageRequest pageRequest = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.fromString(sortDirection), sortField));

            Page<Mouvement> mouvements = mouvementServiceImpl.findByCollecteurAndDate(collecteurId, date, pageRequest);
            Page<MouvementDTO> dtoPage = mouvements.map(mouvementMapper::toDTO);

            ApiResponse<Page<MouvementDTO>> response = ApiResponse.success(dtoPage);
            response.addMeta("totalElements", mouvements.getTotalElements());
            response.addMeta("totalPages", mouvements.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des transactions du journal", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-balance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<BalanceVerificationDTO>> verifyBalance(
            @Valid @RequestBody BalanceVerificationRequest request) {

        log.info("Vérification du solde pour client: {} montant: {}", request.getClientId(), request.getMontant());

        try {
            BalanceVerificationDTO result = mouvementServiceImpl.verifyClientBalance(
                    request.getClientId(),
                    request.getMontant()
            );

            return ResponseEntity.ok(
                    ApiResponse.success(result, "Solde vérifié avec succès")
            );
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du solde", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/collecteur/{collecteurId}/jour")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<List<MouvementCommissionDTO>>> getOperationsDuJour(
            @PathVariable Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date) {

        try {
            LocalDate dateRecherche = date != null ? LocalDate.parse(date) : LocalDate.now();

            Journal journal = journalService.getOrCreateJournalDuJour(collecteurId, dateRecherche);

            // Récupérer les mouvements de ce journal
            List<MouvementCommissionDTO> mouvements = mouvementServiceImpl.findMouvementsDtoByJournalId(journal.getId());

            return ResponseEntity.ok(
                    ApiResponse.success(mouvements, "Opérations du jour récupérées avec succès")
            );
        } catch (Exception e) {
            log.error("Erreur récupération opérations du jour", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }
}