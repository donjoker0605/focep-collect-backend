package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.InvalidOperationException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.security.annotations.AgenceAccess;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.impl.PasswordService;
import org.example.collectfocep.services.interfaces.CollecteurService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collecteurs")
@Slf4j
@RequiredArgsConstructor
public class CollecteurController {
    private final CollecteurService collecteurService;
    private final PasswordService passwordService; // LIGNE MANQUANTE
    private final SecurityService securityService; // Aussi nécessaire

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Audited(action = "CREATE", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<CollecteurDTO>> createCollecteur(@Valid @RequestBody CollecteurCreateDTO dto) {
        log.info("Création d'un nouveau collecteur pour l'agence: {}", dto.getAgenceId());

        Collecteur collecteur = collecteurService.saveCollecteur(dto);

        return ResponseEntity.ok(
                ApiResponse.success(
                        collecteurService.convertToDTO(collecteur),
                        "Collecteur créé avec succès"
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    @Audited(action = "UPDATE", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<CollecteurDTO>> updateCollecteur(
            @PathVariable Long id,
            @Valid @RequestBody CollecteurUpdateDTO dto) {
        log.info("Mise à jour du collecteur: {}", id);

        Collecteur updated = collecteurService.updateCollecteur(id, dto);

        return ResponseEntity.ok(
                ApiResponse.success(
                        collecteurService.convertToDTO(updated),
                        "Collecteur mis à jour avec succès"
                )
        );
    }

    @PutMapping("/{id}/montant-max")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    @Audited(action = "UPDATE_MONTANT_MAX", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<CollecteurDTO>> updateMontantMaxRetrait(
            @PathVariable Long id,
            @Valid @RequestBody MontantMaxRetraitRequest request) {

        log.info("Demande de modification du montant max de retrait pour le collecteur: {}", id);

        Collecteur collecteur = collecteurService.updateMontantMaxRetrait(
                id,
                request.getNouveauMontant(),
                request.getJustification()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        collecteurService.convertToDTO(collecteur),
                        "Montant maximum de retrait mis à jour avec succès"
                )
        );
    }

    @GetMapping("/agence/{agenceId}")
    @AgenceAccess
    public ResponseEntity<List<CollecteurDTO>> getCollecteursByAgence(@PathVariable Long agenceId) {
        log.info("Récupération des collecteurs pour l'agence: {}", agenceId);
        List<Collecteur> collecteurs = collecteurService.findByAgenceId(agenceId);
        List<CollecteurDTO> dtos = collecteurs.stream()
                .map(collecteurService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/agence/{agenceId}/page")
    @AgenceAccess
    public ResponseEntity<ApiResponse<Page<CollecteurDTO>>> getCollecteursByAgencePaginated(
            @PathVariable Long agenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Récupération paginée des collecteurs pour l'agence: {}", agenceId);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Collecteur> collecteursPage = collecteurService.findByAgenceId(agenceId, pageRequest);
        Page<CollecteurDTO> dtoPage = collecteursPage.map(collecteurService::convertToDTO);

        ApiResponse<Page<CollecteurDTO>> response = ApiResponse.success(dtoPage);
        response.addMeta("totalElements", collecteursPage.getTotalElements());
        response.addMeta("totalPages", collecteursPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("@securityService.canResetPassword(authentication, #id)")
    @Audited(action = "RESET_PASSWORD", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordResetRequest request) {
        log.info("Réinitialisation du mot de passe pour le collecteur: {}", id);

        // Récupérer l'authentification courante
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Passer l'authentification à la méthode
        passwordService.resetPassword(id, request.getNewPassword(), auth);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Mot de passe réinitialisé avec succès"
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @securityService.isAdminOfCollecteur(authentication, #id)")
    @Audited(action = "DELETE", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<Void>> deleteCollecteur(@PathVariable Long id) {
        log.info("Suppression du collecteur: {}", id);

        Collecteur collecteur = collecteurService.getCollecteurById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        // Vérifier si le collecteur peut être supprimé
        if (collecteurService.hasActiveOperations(collecteur)) {
            throw new InvalidOperationException("Impossible de supprimer un collecteur ayant des opérations actives");
        }

        collecteurService.deactivateCollecteur(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Collecteur supprimé avec succès"
                )
        );
    }
}