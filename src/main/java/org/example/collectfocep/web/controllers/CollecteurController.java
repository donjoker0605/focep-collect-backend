package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.InvalidOperationException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.security.annotations.AgenceAccess;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.impl.PasswordService;
import org.example.collectfocep.services.interfaces.CollecteurService;
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
import java.util.Map;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collecteurs")
@Slf4j
@RequiredArgsConstructor
public class CollecteurController {

    private final CollecteurService collecteurService;
    private final PasswordService passwordService;
    private final SecurityService securityService;
    private final CollecteurRepository collecteurRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Audited(action = "CREATE", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<CollecteurDTO>> createCollecteur(@Valid @RequestBody CollecteurCreateDTO dto) {
        log.info("Création d'un nouveau collecteur pour l'agence: {}", dto.getAgenceId());

        try {
            Collecteur collecteur = collecteurService.saveCollecteur(dto);
            return ResponseEntity.ok(
                    ApiResponse.success(
                            collecteurService.convertToDTO(collecteur),
                            "Collecteur créé avec succès"
                    )
            );
        } catch (Exception e) {
            log.error("Erreur lors de la création du collecteur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la création du collecteur: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    @Audited(action = "UPDATE", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<CollecteurDTO>> updateCollecteur(
            @PathVariable Long id,
            @Valid @RequestBody CollecteurUpdateDTO dto) {
        log.info("Mise à jour du collecteur: {}", id);

        try {
            Collecteur updated = collecteurService.updateCollecteur(id, dto);
            return ResponseEntity.ok(
                    ApiResponse.success(
                            collecteurService.convertToDTO(updated),
                            "Collecteur mis à jour avec succès"
                    )
            );
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du collecteur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la mise à jour: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/montant-max")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    @Audited(action = "UPDATE_MONTANT_MAX", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<CollecteurDTO>> updateMontantMaxRetrait(
            @PathVariable Long id,
            @Valid @RequestBody MontantMaxRetraitRequest request) {

        log.info("Demande de modification du montant max de retrait pour le collecteur: {}", id);

        try {
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
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du montant max", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/agence/{agenceId}")
    @AgenceAccess
    public ResponseEntity<List<CollecteurDTO>> getCollecteursByAgence(@PathVariable Long agenceId) {
        log.info("Récupération des collecteurs pour l'agence: {}", agenceId);

        try {
            List<Collecteur> collecteurs = collecteurService.findByAgenceId(agenceId);
            List<CollecteurDTO> dtos = collecteurs.stream()
                    .map(collecteurService::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des collecteurs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
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

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Collecteur> collecteursPage = collecteurService.findByAgenceId(agenceId, pageRequest);
            Page<CollecteurDTO> dtoPage = collecteursPage.map(collecteurService::convertToDTO);

            ApiResponse<Page<CollecteurDTO>> response = ApiResponse.success(dtoPage);
            response.addMeta("totalElements", collecteursPage.getTotalElements());
            response.addMeta("totalPages", collecteursPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération paginée", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("@securityService.canResetPassword(authentication, #id)")
    @Audited(action = "RESET_PASSWORD", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordResetRequest request) {
        log.info("Réinitialisation du mot de passe pour le collecteur: {}", id);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            passwordService.resetPassword(id, request.getNewPassword(), auth);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            null,
                            "Mot de passe réinitialisé avec succès"
                    )
            );
        } catch (Exception e) {
            log.error("Erreur lors de la réinitialisation du mot de passe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @securityService.isAdminOfCollecteur(authentication, #id)")
    @Audited(action = "DELETE", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<Void>> deleteCollecteur(@PathVariable Long id) {
        log.info("Suppression du collecteur: {}", id);

        try {
            Collecteur collecteur = collecteurService.getCollecteurById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

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
        } catch (Exception e) {
            log.error("Erreur lors de la suppression", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CollecteurDTO>>> getAllCollecteurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        log.info("Récupération de tous les collecteurs - page: {}, size: {}, search: '{}'",
                page, size, search);

        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("nom", "prenom"));
            Page<Collecteur> collecteursPage;

            if (search != null && !search.trim().isEmpty()) {
                collecteursPage = collecteurRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrAdresseMailContainingIgnoreCase(
                        search.trim(), search.trim(), search.trim(), pageRequest);
            } else {
                collecteursPage = collecteurService.getAllCollecteurs(pageRequest);
            }

            List<CollecteurDTO> collecteurDTOs = collecteursPage.getContent().stream()
                    .map(collecteurService::convertToDTO)
                    .collect(Collectors.toList());

            ApiResponse<List<CollecteurDTO>> response = ApiResponse.success(collecteurDTOs);
            response.addMeta("totalElements", collecteursPage.getTotalElements());
            response.addMeta("totalPages", collecteursPage.getTotalPages());
            response.addMeta("currentPage", page);
            response.addMeta("size", size);
            response.addMeta("hasNext", collecteursPage.hasNext());
            response.addMeta("hasPrevious", collecteursPage.hasPrevious());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des collecteurs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des collecteurs: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @securityService.isOwnerCollecteur(authentication, #id)")
    public ResponseEntity<ApiResponse<CollecteurDashboardDTO>> getCollecteurDashboard(@PathVariable Long id) {
        log.info("Récupération du dashboard pour le collecteur: {}", id);

        try {
            // Vérification supplémentaire côté contrôleur
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Authentication principal: {}", auth.getName());
            log.debug("Authentication authorities: {}", auth.getAuthorities());

            CollecteurDashboardDTO dashboard = collecteurService.getDashboardStats(id);

            return ResponseEntity.ok(
                    ApiResponse.success(dashboard, "Dashboard récupéré avec succès")
            );
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dashboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }
}