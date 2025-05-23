package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.TransferDetailDTO;
import org.example.collectfocep.dto.TransferRequest;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.CompteTransferService;
import org.example.collectfocep.util.ApiResponse;
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
            @RequestParam(defaultValue = "20") int size) {

        log.info("Récupération de tous les transferts - page: {}, size: {}", page, size);

        try {
            // TODO: Implémenter la récupération paginée des transferts
            // Pour l'instant, retourner une liste vide
            Map<String, Object> result = new HashMap<>();
            result.put("content", java.util.List.of());
            result.put("totalElements", 0);
            result.put("totalPages", 0);
            result.put("currentPage", page);
            result.put("size", size);

            return ResponseEntity.ok(
                    ApiResponse.success(result, "Liste des transferts récupérée")
            );
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des transferts", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }
}