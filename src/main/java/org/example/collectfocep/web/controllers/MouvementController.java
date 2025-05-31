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
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.services.impl.JournalServiceImpl;
import org.example.collectfocep.services.impl.MouvementServiceImpl;
import org.example.collectfocep.security.service.SecurityService;
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
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mouvements")
@Slf4j
public class MouvementController {

    private final SecurityService securityService;
    private final ClientRepository clientRepository;
    private final JournalRepository journalRepository;
    private final MouvementRepository mouvementRepository; // ✅ AJOUTÉ
    private final MouvementMapper mouvementMapper;
    private final JournalService journalService;
    private final JournalServiceImpl journalServiceImpl;

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
            MouvementRepository mouvementRepository,
            MouvementMapper mouvementMapper,
            JournalService journalService,
            JournalServiceImpl journalServiceImpl) {
        this.mouvementServiceImpl = mouvementServiceImpl;
        this.securityService = securityService;
        this.clientRepository = clientRepository;
        this.journalRepository = journalRepository;
        this.mouvementRepository = mouvementRepository;
        this.mouvementMapper = mouvementMapper;
        this.journalService = journalService;
        this.journalServiceImpl = journalServiceImpl;
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<MouvementDTO>> getTransactionById(@PathVariable Long transactionId) {
        log.info("🔍 Récupération des détails de la transaction: {}", transactionId);

        try {
            // ✅ CORRECTION: Charger avec toutes les relations
            Mouvement mouvement = mouvementRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée avec l'ID: " + transactionId));

            // ✅ SÉCURITÉ: Vérifier les droits d'accès
            if (!securityService.canAccessMouvement(SecurityContextHolder.getContext().getAuthentication(), mouvement)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("UNAUTHORIZED", "Accès non autorisé à cette transaction"));
            }

            // ✅ CORRECTION: Forcer le chargement des relations lazy
            if (mouvement.getClient() != null) {
                mouvement.getClient().getNom(); // Force lazy loading
            }
            if (mouvement.getCollecteur() != null) {
                mouvement.getCollecteur().getNom(); // Force lazy loading
            }
            if (mouvement.getCompteSource() != null) {
                mouvement.getCompteSource().getNumeroCompte(); // Force lazy loading
            }
            if (mouvement.getCompteDestination() != null) {
                mouvement.getCompteDestination().getNumeroCompte(); // Force lazy loading
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

    // ✅ NOUVEAU : ENDPOINT POUR RÉCUPÉRER TOUTES LES TRANSACTIONS D'UN COLLECTEUR
    @GetMapping("/collecteur/{collecteurId}")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<List<MouvementDTO>>> getTransactionsByCollecteur(
            @PathVariable Long collecteurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.info("🔍 Récupération des transactions pour le collecteur: {}", collecteurId);

        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("dateOperation").descending());
            Page<Mouvement> mouvements = mouvementRepository.findByCollecteurId(collecteurId, pageRequest);

            List<MouvementDTO> dtos = mouvements.getContent().stream()
                    .map(mouvementMapper::toDTO)
                    .toList();

            return ResponseEntity.ok(
                    ApiResponse.success(dtos, "Transactions du collecteur récupérées avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des transactions du collecteur {}", collecteurId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("COLLECTEUR_TRANSACTIONS_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("@securityService.canManageClient(authentication, #clientId)")
    public ResponseEntity<ApiResponse<List<MouvementDTO>>> getTransactionsByClient(@PathVariable Long clientId) {
        log.info("🔍 Récupération des transactions pour le client: {}", clientId);

        try {
            // ✅ CORRECTION: Utiliser une requête avec JOIN FETCH
            List<Mouvement> mouvements = mouvementRepository.findByClientIdWithAllRelations(clientId);

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
    public ResponseEntity<ApiResponse<MouvementCommissionDTO>> effectuerEpargne(@Valid @RequestBody EpargneRequest request) {
        log.info("Traitement d'une opération d'épargne pour le client: {}", request.getClientId());
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

        log.info("Récupération des transactions du journal pour collecteur: {} à la date: {}", collecteurId, date);

        try {
            String[] sortParts = sort.split(",");
            String sortField = sortParts[0];
            String sortDirection = sortParts.length > 1 ? sortParts[1] : "desc";

            if ("dateHeure".equals(sortField)) {
                sortField = "dateOperation";
                log.warn("Paramètre de tri 'dateHeure' détecté et remplacé par 'dateOperation'");
            }

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
    public ResponseEntity<ApiResponse<JournalDuJourDTO>> getOperationsDuJour(
            @PathVariable Long collecteurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("📅 Récupération des opérations du jour - Collecteur: {}, Date: {}", collecteurId, date);

        try {
            // 1. ✅ Date par défaut si non fournie
            LocalDate dateRecherche = date != null ? date : LocalDate.now();

            // 2. ✅ Récupération/création du journal
            Journal journal = journalService.getOrCreateJournalDuJour(collecteurId, dateRecherche);

            // 3. ✅ ÉTAPE MANQUANTE : Récupérer les mouvements de la journée
            LocalDateTime startOfDay = dateRecherche.atStartOfDay();
            LocalDateTime endOfDay = dateRecherche.atTime(LocalTime.MAX);

            List<Mouvement> mouvements = mouvementRepository.findByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfDay, endOfDay);

            // 4. ✅ Conversion en DTOs
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
                    .operations(operationsDTOs)  // ✅ MAINTENANT `operations` existe !
                    .build();

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Opérations du jour récupérées avec succès")
            );

        } catch (Exception e) {
            log.error("❌ Erreur récupération opérations du jour pour collecteur {}", collecteurId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des opérations"));
        }
    }

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
            return mouvement.getTypeMouvement();
        } catch (Exception e) {
            log.debug("Erreur chargement typeMouvement, utilisation du sens: {}", mouvement.getSens());
            return mouvement.getSens() != null ? mouvement.getSens().toUpperCase() : "INCONNU";
        }
    }

    private String safeGetCompteSourceNumero(Mouvement mouvement) {
        try {
            return mouvement.getCompteSourceNumero();
        } catch (Exception e) {
            log.debug("Erreur chargement compteSourceNumero pour mouvement {}", mouvement.getId());
            return "N/A";
        }
    }

    private String safeGetCompteDestinationNumero(Mouvement mouvement) {
        try {
            return mouvement.getCompteDestinationNumero();
        } catch (Exception e) {
            log.debug("Erreur chargement compteDestinationNumero pour mouvement {}", mouvement.getId());
            return "N/A";
        }
    }

    private String safeGetClientNom(Mouvement mouvement) {
        try {
            return mouvement.getClient() != null ? mouvement.getClient().getNom() : null;
        } catch (Exception e) {
            log.debug("Erreur chargement client nom pour mouvement {}", mouvement.getId());
            return "N/A";
        }
    }

    private String safeGetClientPrenom(Mouvement mouvement) {
        try {
            return mouvement.getClient() != null ? mouvement.getClient().getPrenom() : null;
        } catch (Exception e) {
            log.debug("Erreur chargement client prénom pour mouvement {}", mouvement.getId());
            return "N/A";
        }
    }
}