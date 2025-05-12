package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.TransferDetailDTO;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.CompteTransferService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
            @RequestParam Long sourceCollecteurId,
            @RequestParam Long targetCollecteurId,
            @RequestBody List<Long> clientIds) {

        // Vérification des permissions
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!securityService.canManageCollecteur(auth, sourceCollecteurId) ||
                !securityService.canManageCollecteur(auth, targetCollecteurId)) {
            throw new UnauthorizedException("Accès non autorisé à l'un des collecteurs");
        }

        log.info("Transfert demandé de {} comptes du collecteur {} vers collecteur {}",
                clientIds.size(), sourceCollecteurId, targetCollecteurId);

        int transferredCount = compteTransferService.transferComptes(
                sourceCollecteurId, targetCollecteurId, clientIds);

        Map<String, Object> result = new HashMap<>();
        result.put("requested", clientIds.size());
        result.put("transferred", transferredCount);
        result.put("sourceCollecteurId", sourceCollecteurId);
        result.put("targetCollecteurId", targetCollecteurId);

        return ResponseEntity.ok(
                ApiResponse.success(result,
                        String.format("%d comptes sur %d transférés avec succès",
                                transferredCount, clientIds.size()))
        );
    }

    /**
     * Renvoie les détails d'un transfert effectué, y compris les mouvements comptables générés.
     */
    @GetMapping("/{transferId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TransferDetailDTO>> getTransferDetails(@PathVariable Long transferId) {
        log.info("Récupération des détails du transfert: {}", transferId);

        TransferDetailDTO details = compteTransferService.getTransferDetails(transferId);

        return ResponseEntity.ok(
                ApiResponse.success(details, "Détails du transfert récupérés avec succès")
        );
    }
}
