package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.TransferDetailDTO;
import org.example.collectfocep.dto.TransferRequest;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.CompteTransferService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
@Slf4j
@RequiredArgsConstructor
public class CompteTransferController {

    private final CompteTransferService compteTransferService;
    private final SecurityService securityService;

    /**
     * Transfère des comptes clients d'un collecteur à un autre avec gestion
     * spéciale des soldes lors du transfert entre agences différentes.
     */
    @PostMapping("/collecteurs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transferComptes(
            @Valid @RequestBody TransferRequest request) {

        log.info("Demande de transfert reçue: {} clients du collecteur {} vers {}",
                request.getClientIds().size(),
                request.getSourceCollecteurId(),
                request.getTargetCollecteurId());

        try {
            // Vérification des permissions
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (!securityService.canManageCollecteur(auth, request.getSourceCollecteurId()) ||
                    !securityService.canManageCollecteur(auth, request.getTargetCollecteurId())) {
                throw new UnauthorizedException("Accès non autorisé à l'un des collecteurs");
            }

            // Effectuer le transfert
            int transferredCount = compteTransferService.transferComptes(
                    request.getSourceCollecteurId(),
                    request.getTargetCollecteurId(),
                    request.getClientIds());

            // Préparer la réponse
            Map<String, Object> result = new HashMap<>();
            result.put("requested", request.getClientIds().size());
            result.put("transferred", transferredCount);
            result.put("sourceCollecteurId", request.getSourceCollecteurId());
            result.put("targetCollecteurId", request.getTargetCollecteurId());
            result.put("justification", request.getJustification());

            return ResponseEntity.ok(
                    ApiResponse.success(result,
                            String.format("%d comptes sur %d transférés avec succès",
                                    transferredCount, request.getClientIds().size()))
            );

        } catch (Exception e) {
            log.error("Erreur lors du transfert", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors du transfert: " + e.getMessage()));
        }
    }

    /**
     * Endpoint alternatif pour compatibilité avec les anciens appels
     */
    @PostMapping("/transfers")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transferComptesLegacy(
            @RequestParam Long sourceCollecteurId,
            @RequestParam Long targetCollecteurId,
            @RequestBody java.util.List<Long> clientIds) {

        // Créer un objet TransferRequest pour la compatibilité
        TransferRequest request = new TransferRequest();
        request.setSourceCollecteurId(sourceCollecteurId);
        request.setTargetCollecteurId(targetCollecteurId);
        request.setClientIds(clientIds);

        return transferComptes(request);
    }

    /**
     * Renvoie les détails d'un transfert effectué, y compris les mouvements comptables générés.
     */
    @GetMapping("/{transferId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TransferDetailDTO>> getTransferDetails(@PathVariable Long transferId) {
        log.info("Récupération des détails du transfert: {}", transferId);

        try {
            TransferDetailDTO details = compteTransferService.getTransferDetails(transferId);
            return ResponseEntity.ok(
                    ApiResponse.success(details, "Détails du transfert récupérés avec succès")
            );
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des détails du transfert", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Liste tous les transferts avec pagination
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getAllTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long collecteurId,
            @RequestParam(required = false) Boolean interAgence,
            Authentication authentication) {

        log.info("Récupération des transferts - page: {}, size: {}, collecteurId: {}, interAgence: {}", 
                page, size, collecteurId, interAgence);

        try {
            // Vérifier les permissions pour le collecteur si spécifié
            if (collecteurId != null && !securityService.canManageCollecteur(authentication, collecteurId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("FORBIDDEN", "Accès refusé au collecteur spécifié"));
            }

            // Créer la pageable
            PageRequest pageRequest = PageRequest.of(page, size, 
                Sort.by("dateTransfert").descending());

            // Utiliser le service pour récupérer les transferts
            Page<org.example.collectfocep.entities.TransfertCompte> transfersPage = 
                compteTransferService.getAllTransfers(pageRequest, collecteurId, interAgence);

            // Mapper vers des DTOs simplifiés pour la liste
            java.util.List<Map<String, Object>> transferDTOs = transfersPage.getContent().stream()
                .map(transfer -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", transfer.getId());
                    dto.put("dateTransfert", transfer.getDateTransfert());
                    dto.put("sourceCollecteurId", transfer.getSourceCollecteurId());
                    dto.put("targetCollecteurId", transfer.getTargetCollecteurId());
                    dto.put("nombreComptes", transfer.getNombreComptes());
                    dto.put("montantTotal", transfer.getMontantTotal());
                    dto.put("montantCommissions", transfer.getMontantCommissions());
                    dto.put("isInterAgence", transfer.getIsInterAgence());
                    dto.put("statut", transfer.getStatut());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

            // Créer la réponse paginée
            Map<String, Object> result = new HashMap<>();
            result.put("content", transferDTOs);
            result.put("totalElements", transfersPage.getTotalElements());
            result.put("totalPages", transfersPage.getTotalPages());
            result.put("currentPage", page);
            result.put("size", size);
            result.put("first", transfersPage.isFirst());
            result.put("last", transfersPage.isLast());

            return ResponseEntity.ok(
                    ApiResponse.success(result, 
                        String.format("Récupéré %d transferts sur %d total", 
                            transferDTOs.size(), transfersPage.getTotalElements()))
            );

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des transferts", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRANSFER_FETCH_ERROR", "Erreur: " + e.getMessage()));
        }
    }
}