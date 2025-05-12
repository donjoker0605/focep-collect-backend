package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.EpargneRequest;
import org.example.collectfocep.dto.MouvementCommissionDTO;
import org.example.collectfocep.dto.RetraitRequest;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedAgencyAccessException;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.mappers.MouvementMapper;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.JournalRepository;
import org.example.collectfocep.services.impl.MouvementService;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mouvements")
@Slf4j
public class MouvementController {
    private final MouvementService mouvementService;
    private final SecurityService securityService;
    private final ClientRepository clientRepository;
    private final JournalRepository journalRepository;
    private final MouvementMapper mouvementMapper;

    @Autowired
    public MouvementController(
            MouvementService mouvementService,
            SecurityService securityService,
            ClientRepository clientRepository,
            JournalRepository journalRepository,
            MouvementMapper mouvementMapper) {
        this.mouvementService = mouvementService;
        this.securityService = securityService;
        this.clientRepository = clientRepository;
        this.journalRepository = journalRepository;
        this.mouvementMapper = mouvementMapper;
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

            Journal journal = null;
            if (request.getJournalId() != null) {
                journal = journalRepository.findById(request.getJournalId())
                        .orElseThrow(() -> new ResourceNotFoundException("Journal non trouvé"));
            }

            // Enregistrer l'épargne
            Mouvement mouvement = mouvementService.enregistrerEpargne(client, request.getMontant(), journal);

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
    public ResponseEntity<?> effectuerRetrait(@Valid @RequestBody RetraitRequest request) {
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

            Journal journal = null;
            if (request.getJournalId() != null) {
                journal = journalRepository.findById(request.getJournalId())
                        .orElseThrow(() -> new ResourceNotFoundException("Journal non trouvé"));
            }

            Mouvement mouvement = mouvementService.enregistrerRetrait(client, request.getMontant(), journal);
            return ResponseEntity.ok(mouvement);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du retrait", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'enregistrement du retrait: " + e.getMessage());
        }
    }

    @GetMapping("/journal/{journalId}")
    @PreAuthorize("@securityService.canAccessJournal(authentication, #journalId)")
    public ResponseEntity<List<Mouvement>> getMouvementsByJournal(@PathVariable Long journalId) {
        // Vérification explicite des autorisations
        if (!securityService.canAccessJournal(SecurityContextHolder.getContext().getAuthentication(),
                journalId)) {
            throw new UnauthorizedException("Non autorisé à accéder à ce journal");
        }

        return ResponseEntity.ok(mouvementService.findByJournalId(journalId));
    }
}