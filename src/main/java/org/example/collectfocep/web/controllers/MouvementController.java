package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.aspects.LogActivity;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedAgencyAccessException;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.mappers.MouvementMapperV2;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.JournalRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.services.SoldeCollecteurValidationService;
import org.example.collectfocep.services.impl.AuditService;
import org.example.collectfocep.services.impl.JournalServiceImpl;
import org.example.collectfocep.services.impl.MouvementServiceImpl;
import org.example.collectfocep.services.impl.TransactionService;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.DateTimeService;
import org.example.collectfocep.services.interfaces.JournalService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mouvements")
@Slf4j
public class MouvementController {

    private final DateTimeService dateTimeService;
    private final TransactionService transactionService;
    private final SecurityService securityService;
    private final ClientRepository clientRepository;
    private final JournalRepository journalRepository;
    private final MouvementRepository mouvementRepository;
    private final MouvementMapperV2 mouvementMapper;
    private final JournalService journalService;
    private final JournalServiceImpl journalServiceImpl;
    private final AuditService auditService;
    private final SoldeCollecteurValidationService soldeValidationService;

    @Autowired
    private MouvementServiceImpl mouvementServiceImpl;

    @Value("${app.mouvement.use-projection:true}")
    private boolean useProjection;

    @Autowired
    public MouvementController(
            DateTimeService dateTimeService,
            TransactionService transactionService,
            MouvementServiceImpl mouvementServiceImpl,
            SecurityService securityService,
            ClientRepository clientRepository,
            JournalRepository journalRepository,
            MouvementRepository mouvementRepository,
            MouvementMapperV2 mouvementMapper,
            JournalService journalService,
            JournalServiceImpl journalServiceImpl,
            AuditService auditService,
            SoldeCollecteurValidationService soldeValidationService) {

        this.dateTimeService = dateTimeService;
        this.transactionService = transactionService;
        this.mouvementServiceImpl = mouvementServiceImpl;
        this.securityService = securityService;
        this.clientRepository = clientRepository;
        this.journalRepository = journalRepository;
        this.mouvementRepository = mouvementRepository;
        this.mouvementMapper = mouvementMapper;
        this.journalService = journalService;
        this.journalServiceImpl = journalServiceImpl;
        this.auditService = auditService;
        this.soldeValidationService = soldeValidationService;
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<MouvementDTO>> getTransactionById(@PathVariable Long transactionId) {
        log.info("🔍 Récupération des détails de la transaction: {}", transactionId);

        try {
            // Utiliser la requête optimisée avec toutes les relations
            Mouvement mouvement = mouvementRepository.findByIdWithAllRelations(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée avec l'ID: " + transactionId));

            // Vérifier les droits d'accès
            if (!securityService.canAccessMouvement(SecurityContextHolder.getContext().getAuthentication(), mouvement)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("UNAUTHORIZED", "Accès non autorisé à cette transaction"));
            }

            MouvementDTO dto = mouvementMapper.toDTO(mouvement);

            return ResponseEntity.ok(
                    ApiResponse.success(dto, "Détails de la transaction récupérés avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de la transaction {}", transactionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("TRANSACTION_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/collecteur/{collecteurId}")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<List<MouvementDTO>>> getTransactionsByCollecteur(
            @PathVariable Long collecteurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("🔍 Récupération des transactions pour le collecteur: {} (page: {}, size: {})", collecteurId, page, size);

        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("dateOperation").descending());
            Page<Mouvement> mouvements;

            // UTILISATION DU DateTimeService pour les filtres de date
            if (dateDebut != null && dateFin != null) {
                LocalDateTime startDateTime = dateTimeService.toStartOfDay(dateDebut);
                LocalDateTime endDateTime = dateTimeService.toEndOfDay(dateFin);

                mouvements = mouvementRepository.findByCollecteurIdAndDateTimeBetween(
                        collecteurId, startDateTime, endDateTime, pageRequest);
            } else if (dateDebut != null) {
                // Si seulement date de début, prendre toute la journée
                LocalDateTime startDateTime = dateTimeService.toStartOfDay(dateDebut);
                LocalDateTime endDateTime = dateTimeService.toEndOfDay(dateDebut);

                mouvements = mouvementRepository.findByCollecteurIdAndDateTimeBetween(
                        collecteurId, startDateTime, endDateTime, pageRequest);
            } else {
                // Sans filtre de date
                mouvements = mouvementRepository.findByCollecteurId(collecteurId, pageRequest);
            }

            List<MouvementDTO> dtos = mouvements.getContent().stream()
                    .map(mouvementMapper::toDTO)
                    .toList();

            ApiResponse<List<MouvementDTO>> response = ApiResponse.success(dtos, "Transactions du collecteur récupérées avec succès");
            response.addMeta("totalElements", mouvements.getTotalElements());
            response.addMeta("totalPages", mouvements.getTotalPages());
            response.addMeta("currentPage", page);
            response.addMeta("size", size);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des transactions du collecteur {}", collecteurId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("COLLECTEUR_TRANSACTIONS_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("@securityService.canManageClient(authentication, #clientId)")
    public ResponseEntity<ApiResponse<List<MouvementDTO>>> getTransactionsByClient(
            @PathVariable Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("🔍 Récupération des transactions pour le client: {}", clientId);

        try {
            List<Mouvement> mouvements;

            // UTILISATION DU DateTimeService pour les filtres de date
            if (dateDebut != null && dateFin != null) {
                LocalDateTime startDateTime = dateTimeService.toStartOfDay(dateDebut);
                LocalDateTime endDateTime = dateTimeService.toEndOfDay(dateFin);

                mouvements = mouvementRepository.findByClientIdAndPeriod(clientId, startDateTime, endDateTime);
            } else {
                // CORRECTION: Utiliser une requête avec JOIN FETCH
                mouvements = mouvementRepository.findByClientIdWithAllRelations(clientId);
            }

            List<MouvementDTO> dtos = mouvements.stream()
                    .map(mouvementMapper::toDTO)
                    .toList();

            return ResponseEntity.ok(
                    ApiResponse.success(dtos, "Transactions du client récupérées avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des transactions du client {}", clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CLIENT_TRANSACTIONS_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/epargne")
    @PreAuthorize("@securityService.canManageClient(authentication, #request.clientId)")
    @LogActivity(action = "TRANSACTION_EPARGNE", entityType = "MOUVEMENT")
    public ResponseEntity<ApiResponse<MouvementCommissionDTO>> effectuerEpargne(@Valid @RequestBody EpargneRequest request) {
        log.info("💰 Traitement d'une opération d'épargne pour le client: {} - Montant: {}",
                request.getClientId(), request.getMontant());

        return transactionService.executeInTransaction(status -> {
            try {
                if (!securityService.canManageClient(SecurityContextHolder.getContext().getAuthentication(),
                        request.getClientId())) {
                    throw new UnauthorizedException("Non autorisé à gérer ce client");
                }

                Client client = clientRepository.findById(request.getClientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

                if (!securityService.isClientInCollecteurAgence(request.getClientId(), request.getCollecteurId())) {
                    throw new UnauthorizedAgencyAccessException("Client n'appartient pas à votre agence");
                }

                Mouvement mouvement = mouvementServiceImpl.enregistrerEpargne(client, request.getMontant(), null);
                MouvementCommissionDTO responseDto = mouvementMapper.toCommissionDto(mouvement);

                log.info("✅ Épargne enregistrée avec succès: ID={}, Client={}, Montant={}",
                        mouvement.getId(), client.getNom(), request.getMontant());


                return ResponseEntity.ok(
                        ApiResponse.success(responseDto, "Opération d'épargne enregistrée avec succès")
                );
            } catch (Exception e) {
                log.error("❌ Erreur lors de l'enregistrement de l'épargne", e);
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.<MouvementCommissionDTO>error("EPARGNE_ERROR",
                                "Erreur lors de l'enregistrement de l'épargne: " + e.getMessage()));
            }
        });
    }


    @PostMapping("/retrait")
    @PreAuthorize("@securityService.canManageClient(authentication, #request.clientId)")
    @LogActivity(action = "TRANSACTION_RETRAIT", entityType = "MOUVEMENT")
    public ResponseEntity<ApiResponse<MouvementCommissionDTO>> effectuerRetrait(@Valid @RequestBody RetraitRequest request) {
        log.info("💸 Traitement d'une opération de retrait pour le client: {} - Montant: {}",
                request.getClientId(), request.getMontant());

        // Validation du solde collecteur
        ValidationResult validation = soldeValidationService.validateRetraitPossible(
                request.getCollecteurId(),
                request.getMontant()
        );

        if (!validation.isSuccess()) {
            // Logger l'échec de validation
            auditService.logAction(
                    "RETRAIT_REFUSE",
                    "VALIDATION",
                    request.getClientId(),
                    validation.getErrorCode() + ": " + validation.getMessage()
            );

            return ResponseEntity.badRequest()
                    .body(ApiResponse.<MouvementCommissionDTO>error(validation.getErrorCode(),
                            validation.getMessage()));
        }

        return transactionService.executeInTransaction(status -> {
            try {
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
                MouvementCommissionDTO responseDto = mouvementMapper.toCommissionDto(mouvement);

                log.info("✅ Retrait effectué avec succès: ID={}, Client={}, Montant={}",
                        mouvement.getId(), client.getNom(), request.getMontant());

                return ResponseEntity.ok(
                        ApiResponse.success(responseDto, "Retrait effectué avec succès")
                );
            } catch (Exception e) {
                log.error("❌ Erreur lors de l'enregistrement du retrait", e);
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.<MouvementCommissionDTO>error("RETRAIT_ERROR",
                                "Erreur lors de l'enregistrement du retrait: " + e.getMessage()));
            }
        });
    }

    @GetMapping("/journal/{journalId}")
    public ResponseEntity<ApiResponse<List<MouvementCommissionDTO>>> getMouvementsByJournal(
            @PathVariable Long journalId,
            @RequestParam(defaultValue = "auto") String strategy) {

        try {
            List<MouvementCommissionDTO> mouvementDTOs;

            if ("projection".equals(strategy) || (useProjection && "auto".equals(strategy))) {
                mouvementDTOs = mouvementServiceImpl.findMouvementsDtoByJournalId(journalId);
            } else {
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

        log.info("📋 Récupération des transactions du journal pour collecteur: {} à la date: {}", collecteurId, date);

        try {
            String[] sortParts = sort.split(",");
            String sortField = sortParts[0];
            String sortDirection = sortParts.length > 1 ? sortParts[1] : "desc";

            // Validation et correction du champ de tri
            if ("dateHeure".equals(sortField)) {
                sortField = "dateOperation";
                log.warn("⚠️ Paramètre de tri 'dateHeure' détecté et remplacé par 'dateOperation'");
            }

            if (!"dateOperation".equals(sortField) && !"montant".equals(sortField) && !"id".equals(sortField)) {
                log.warn("⚠️ Champ de tri non reconnu: {}. Utilisation de 'dateOperation' par défaut", sortField);
                sortField = "dateOperation";
            }

            PageRequest pageRequest = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.fromString(sortDirection), sortField));

            // ✅ CORRECTION: Utiliser la nouvelle méthode qui accepte LocalDateTime
            LocalDate localDate = LocalDate.parse(date);
            LocalDateTime startOfDay = dateTimeService.toStartOfDay(localDate);
            LocalDateTime endOfDay = dateTimeService.toEndOfDay(localDate);

            Page<Mouvement> mouvements = mouvementRepository.findByCollecteurAndDate(
                    collecteurId, startOfDay, endOfDay, pageRequest);

            Page<MouvementDTO> dtoPage = mouvements.map(mouvementMapper::toDTO);

            ApiResponse<Page<MouvementDTO>> response = ApiResponse.success(dtoPage);
            response.addMeta("totalElements", mouvements.getTotalElements());
            response.addMeta("totalPages", mouvements.getTotalPages());
            response.addMeta("date", date);
            response.addMeta("collecteurId", collecteurId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des transactions du journal", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("JOURNAL_TRANSACTIONS_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-balance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<BalanceVerificationDTO>> verifyBalance(
            @Valid @RequestBody BalanceVerificationRequest request) {

        log.info("🔍 Vérification du solde pour client: {} montant: {}", request.getClientId(), request.getMontant());

        try {
            BalanceVerificationDTO result = mouvementServiceImpl.verifyClientBalance(
                    request.getClientId(),
                    request.getMontant()
            );

            return ResponseEntity.ok(
                    ApiResponse.success(result, "Solde vérifié avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification du solde", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("BALANCE_VERIFICATION_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/collecteur/{collecteurId}/jour")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<JournalDuJourDTO>> getOperationsDuJour(
            @PathVariable Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("📅 Récupération des opérations du jour - Collecteur: {}, Date: {}", collecteurId, date);

        try {
            // 1. ✅ UTILISATION DU DateTimeService pour la date par défaut
            LocalDate dateRecherche = date != null ? date : dateTimeService.getCurrentDate();

            // 2. ✅ Récupération/création du journal
            Journal journal = journalService.getOrCreateJournalDuJour(collecteurId, dateRecherche);

            // 3. ✅ UTILISATION DU DateTimeService pour les plages horaires
            LocalDateTime startOfDay = dateTimeService.toStartOfDay(dateRecherche);
            LocalDateTime endOfDay = dateTimeService.toEndOfDay(dateRecherche);

            List<Mouvement> mouvements = mouvementRepository.findByCollecteurAndDay(
                    collecteurId, startOfDay, endOfDay);

            // 4. ✅ Conversion en DTOs avec gestion sécurisée
            List<MouvementJournalDTO> operationsDTOs = mouvements.stream()
                    .map(this::convertToMouvementJournalDTO)
                    .collect(Collectors.toList());

            // 5. ✅ Construction du DTO de réponse
            JournalDuJourDTO response = JournalDuJourDTO.builder()
                    .journalId(journal.getId())
                    .collecteurId(collecteurId)
                    .date(dateRecherche)
                    .statut(journal.getStatut())
                    .estCloture(journal.isEstCloture())
                    .reference(journal.getReference())
                    .nombreOperations(operationsDTOs.size())
                    .operations(operationsDTOs)
                    .build();

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Opérations du jour récupérées avec succès")
            );

        } catch (Exception e) {
            log.error("❌ Erreur récupération opérations du jour pour collecteur {}", collecteurId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("OPERATIONS_JOUR_ERROR", "Erreur lors de la récupération des opérations"));
        }
    }

    // ✅ NOUVEAU ENDPOINT: Statistiques de période pour un collecteur
    @GetMapping("/collecteur/{collecteurId}/stats")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<Object>> getCollecteurStats(
            @PathVariable Long collecteurId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("📊 Récupération des statistiques pour collecteur: {} (période: {} - {})",
                collecteurId, dateDebut, dateFin);

        try {
            // ✅ UTILISATION DU DateTimeService pour les plages
            LocalDateTime startDateTime = dateTimeService.toStartOfDay(dateDebut);
            LocalDateTime endDateTime = dateTimeService.toEndOfDay(dateFin);

            Object[] stats = mouvementRepository.getStatsByCollecteurAndPeriod(
                    collecteurId, startDateTime, endDateTime);

            return ResponseEntity.ok(
                    ApiResponse.success(stats, "Statistiques récupérées avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("STATS_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    // =====================================
    // MÉTHODES UTILITAIRES
    // =====================================

    private MouvementJournalDTO convertToMouvementJournalDTO(Mouvement mouvement) {
        return MouvementJournalDTO.builder()
                .id(mouvement.getId())
                .typeMouvement(safeGetTypeMouvement(mouvement))
                .montant(mouvement.getMontant())
                .sens(mouvement.getSens())
                .libelle(mouvement.getLibelle())
                .dateOperation(mouvement.getDateOperation())
                .compteSourceNumero(safeGetCompteSourceNumero(mouvement))
                .compteDestinationNumero(safeGetCompteDestinationNumero(mouvement))
                .clientNom(safeGetClientNom(mouvement))
                .clientPrenom(safeGetClientPrenom(mouvement))
                .build();
    }

    private String safeGetTypeMouvement(Mouvement mouvement) {
        try {
            String type = mouvement.getTypeMouvement();
            return type != null ? type : mouvement.getSens().toUpperCase();
        } catch (Exception e) {
            log.debug("⚠️ Erreur chargement typeMouvement, utilisation du sens: {}", mouvement.getSens());
            return mouvement.getSens() != null ? mouvement.getSens().toUpperCase() : "INCONNU";
        }
    }

    private String safeGetCompteSourceNumero(Mouvement mouvement) {
        try {
            return mouvement.getCompteSourceNumero();
        } catch (Exception e) {
            log.debug("⚠️ Erreur chargement compteSourceNumero pour mouvement {}", mouvement.getId());
            return "N/A";
        }
    }

    private String safeGetCompteDestinationNumero(Mouvement mouvement) {
        try {
            return mouvement.getCompteDestinationNumero();
        } catch (Exception e) {
            log.debug("⚠️ Erreur chargement compteDestinationNumero pour mouvement {}", mouvement.getId());
            return "N/A";
        }
    }

    private String safeGetClientNom(Mouvement mouvement) {
        try {
            return mouvement.getClient() != null ? mouvement.getClient().getNom() : null;
        } catch (Exception e) {
            log.debug("⚠️ Erreur chargement client nom pour mouvement {}", mouvement.getId());
            return "N/A";
        }
    }

    private String safeGetClientPrenom(Mouvement mouvement) {
        try {
            return mouvement.getClient() != null ? mouvement.getClient().getPrenom() : null;
        } catch (Exception e) {
            log.debug("⚠️ Erreur chargement client prénom pour mouvement {}", mouvement.getId());
            return "N/A";
        }
    }
}