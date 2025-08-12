package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.TransferDetailDTO;
import org.example.collectfocep.dto.TransferRequest;
import org.example.collectfocep.dto.TransferValidationResult;
import org.example.collectfocep.exceptions.DryRunException;
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
     * ENDPOINT UNIFIÉ - Transfert avec validation ou dry-run
     * 
     * Usage:
     * - POST /api/transfers?dryRun=true  -> Validation seulement (remplace /validate-quick et /validate-full)
     * - POST /api/transfers             -> Transfert réel (remplace /collecteurs)
     * 
     * @param request Données du transfert
     * @param dryRun Si true, effectue validation sans transfert réel (default: false)
     * @return TransferValidationResult si dryRun=true, sinon résultat du transfert
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<?>> processTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun) {

        String mode = dryRun ? "VALIDATION" : "TRANSFERT";
        log.info("🔄 {} - {} clients du collecteur {} vers {} (dryRun={})",
                mode, request.getClientIds().size(),
                request.getSourceCollecteurId(), request.getTargetCollecteurId(), dryRun);

        try {
            // Vérification des permissions
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (!securityService.canManageCollecteur(auth, request.getSourceCollecteurId()) ||
                    !securityService.canManageCollecteur(auth, request.getTargetCollecteurId())) {
                throw new UnauthorizedException("Accès non autorisé à l'un des collecteurs");
            }

            // APPEL DE LA MÉTHODE UNIFIÉE AVEC DRY-RUN
            Object result = compteTransferService.transferComptesWithValidation(
                    request.getSourceCollecteurId(),
                    request.getTargetCollecteurId(),
                    request.getClientIds(),
                    dryRun
            );

            // Si on arrive ici sans exception, c'est un transfert réel (dryRun=false)
            Integer transferCount = (Integer) result;
            
            Map<String, Object> response = new HashMap<>();
            response.put("transferredCount", transferCount);
            response.put("totalRequested", request.getClientIds().size());
            response.put("successful", transferCount > 0);
            
            String message = String.format("✅ %d/%d clients transférés avec succès", 
                    transferCount, request.getClientIds().size());
            
            log.info("✅ Transfert réel terminé: {}", message);
            return ResponseEntity.ok(ApiResponse.success(response, message));

        } catch (DryRunException dryRunEx) {
            // Cas du dry-run - retourner le résultat de validation
            TransferValidationResult validation = dryRunEx.getResult();
            
            String message = validation.isValid() 
                ? String.format("🧪 Validation réussie: %s", validation.getSummary())
                : String.format("❌ Validation échouée: %s", validation.getSummary());
            
            log.info("🧪 Dry-run terminé: {}", message);
            return ResponseEntity.ok(ApiResponse.success(validation, message));
            
        } catch (UnauthorizedException e) {
            log.warn("🚫 Accès refusé: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("UNAUTHORIZED", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("❌ Erreur lors du {}: {}", mode.toLowerCase(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRANSFER_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * ENDPOINT DE COMPATIBILITÉ - pour les anciens appels directs
     * @deprecated Utiliser POST /api/transfers à la place
     */
    @PostMapping("/collecteurs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Deprecated
    public ResponseEntity<ApiResponse<?>> transferComptesLegacy(@Valid @RequestBody TransferRequest request) {
        log.warn("⚠️  Utilisation d'endpoint déprécié /collecteurs - utiliser POST /transfers");
        return processTransfer(request, false);
    }

    /**
     * Renvoie les détails d'un transfert effectué, y compris les mouvements comptables générés.
     */
    @GetMapping("/{transferId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TransferDetailDTO>> getTransferDetails(@PathVariable Long transferId) {
        
        log.info("📋 Récupération des détails du transfert: {}", transferId);
        
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
     * Récupère l'historique des transferts avec pagination
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTransfersHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateTransfert") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long agenceId) {
        
        log.info("📋 Récupération historique transferts - page: {}, size: {}, agence: {}", 
                page, size, agenceId);

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            
            // Pour l'instant, retourner une réponse vide - à implémenter selon les besoins
            Map<String, Object> result = new HashMap<>();
            result.put("content", java.util.Collections.emptyList());
            result.put("totalElements", 0L);
            result.put("totalPages", 0);
            result.put("currentPage", page);
            result.put("size", size);
            result.put("first", true);
            result.put("last", true);

            return ResponseEntity.ok(
                    ApiResponse.success(result, "Historique des transferts récupéré")
            );

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des transferts", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRANSFER_FETCH_ERROR", "Erreur: " + e.getMessage()));
        }
    }
}