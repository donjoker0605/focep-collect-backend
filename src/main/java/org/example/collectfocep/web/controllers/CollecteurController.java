package org.example.collectfocep.web.controllers;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.InvalidOperationException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.security.annotations.AgenceAccess;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.security.filters.JwtAuthenticationFilter;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collecteurs")
@Slf4j
@RequiredArgsConstructor
public class CollecteurController {

    @PostConstruct
    public void logControllerInitialization() {
        log.info("🚀 CollecteurController initialisé avec succès");
        log.info("📍 Mappings disponibles:");
        log.info("   - GET /api/collecteurs/{id}/dashboard");
        log.info("   - GET /api/collecteurs/{id}/dashboard-debug");
    }

    private final CollecteurService collecteurService;
    private final PasswordService passwordService;
    private final SecurityService securityService;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;

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

    // AJOUTEZ CETTE MÉTHODE À VOTRE CollecteurController

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<ApiResponse<CollecteurDashboardDTO>> getCollecteurDashboard(@PathVariable Long id) {
        log.info("🎯 REQUÊTE DASHBOARD REÇUE pour collecteur: {}", id);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.info("🔐 Auth: name={}, authorities={}",
                    auth != null ? auth.getName() : "null",
                    auth != null ? auth.getAuthorities() : "null");

            // ✅ VÉRIFICATION SIMPLE : Le collecteur connecté accède à ses propres données
            if (auth != null && auth.getPrincipal() instanceof JwtAuthenticationFilter.JwtUserPrincipal) {
                JwtAuthenticationFilter.JwtUserPrincipal principal =
                        (JwtAuthenticationFilter.JwtUserPrincipal) auth.getPrincipal();
                Long connectedUserId = principal.getUserId();

                log.info("🔍 Vérification accès: connectedUserId={}, requestedId={}", connectedUserId, id);

                if (!id.equals(connectedUserId)) {
                    log.warn("❌ Accès refusé: collecteur {} tente d'accéder aux données de {}", connectedUserId, id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Accès non autorisé"));
                }
            }

            // 1. RÉCUPÉRER LE COLLECTEUR
            Collecteur collecteur = collecteurService.getCollecteurById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            log.info("✅ Collecteur trouvé: {} - {} {}",
                    collecteur.getId(), collecteur.getNom(), collecteur.getPrenom());

            // 2. CONSTRUIRE LE DASHBOARD
            CollecteurDashboardDTO dashboard = buildDashboard(collecteur);

            log.info("✅ Dashboard construit avec succès pour: {} {}",
                    collecteur.getNom(), collecteur.getPrenom());

            return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard récupéré avec succès"));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du dashboard pour collecteur {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    private CollecteurDashboardDTO buildDashboard(Collecteur collecteur) {
        log.info("🔨 Construction du dashboard pour collecteur: {}", collecteur.getId());

        try {
            // STATISTIQUES DE BASE
            Long totalClientsCount = clientRepository.countByCollecteurId(collecteur.getId());
            Integer totalClients = totalClientsCount != null ? totalClientsCount.intValue() : 0;

            log.info("📊 Total clients trouvés: {}", totalClients);

            // CONSTRUCTION DU DASHBOARD
            return CollecteurDashboardDTO.builder()
                    .collecteurId(collecteur.getId())
                    .collecteurNom(collecteur.getNom())
                    .collecteurPrenom(collecteur.getPrenom())
                    .totalClients(totalClients)

                    // VALEURS PAR DÉFAUT (À ENRICHIR PROGRESSIVEMENT)
                    .totalEpargne(0.0)
                    .totalRetraits(0.0)
                    .soldeTotal(0.0)
                    .transactionsAujourdhui(0L)
                    .montantEpargneAujourdhui(0.0)
                    .montantRetraitAujourdhui(0.0)
                    .nouveauxClientsAujourdhui(0L)
                    .montantEpargneSemaine(0.0)
                    .montantRetraitSemaine(0.0)
                    .transactionsSemaine(0L)
                    .montantEpargneMois(0.0)
                    .montantRetraitMois(0.0)
                    .transactionsMois(0L)
                    .objectifMensuel(collecteur.getMontantMaxRetrait())
                    .progressionObjectif(0.0)
                    .commissionsMois(0.0)
                    .commissionsAujourdhui(0.0)

                    // COLLECTIONS VIDES POUR ÉVITER LES ERREURS
                    .transactionsRecentes(List.of())
                    .clientsActifs(List.of())
                    .alertes(List.of())
                    .journalActuel(null)

                    .lastUpdate(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la construction du dashboard: {}", e.getMessage(), e);
            throw new BusinessException("Erreur construction dashboard: " + e.getMessage());
        }
    }

    private CollecteurDashboardDTO buildSimpleDashboard(Collecteur collecteur) {
        log.info("🔨 Construction du dashboard simple pour collecteur: {}", collecteur.getId());

        try {
            // ÉTAPE 1: Compter les clients (requête simple et sûre)
            Long totalClientsCount = clientRepository.countByCollecteurId(collecteur.getId());
            Integer totalClients = totalClientsCount != null ? totalClientsCount.intValue() : 0;

            log.info("📊 Total clients trouvés: {}", totalClients);

            // ÉTAPE 2: Construire un dashboard basique mais fonctionnel
            return CollecteurDashboardDTO.builder()
                    .collecteurId(collecteur.getId())
                    .collecteurNom(collecteur.getNom())
                    .collecteurPrenom(collecteur.getPrenom())
                    .totalClients(totalClients)

                    // ✅ VALEURS TEMPORAIRES POUR ÉVITER LES ERREURS
                    .totalEpargne(0.0)
                    .totalRetraits(0.0)
                    .soldeTotal(0.0)
                    .transactionsAujourdhui(0L)
                    .montantEpargneAujourdhui(0.0)
                    .montantRetraitAujourdhui(0.0)
                    .nouveauxClientsAujourdhui(0L)
                    .montantEpargneSemaine(0.0)
                    .montantRetraitSemaine(0.0)
                    .transactionsSemaine(0L)
                    .montantEpargneMois(0.0)
                    .montantRetraitMois(0.0)
                    .transactionsMois(0L)
                    .objectifMensuel(collecteur.getMontantMaxRetrait())
                    .progressionObjectif(0.0)
                    .commissionsMois(0.0)
                    .commissionsAujourdhui(0.0)

                    // ✅ COLLECTIONS VIDES POUR ÉVITER LES ERREURS
                    .transactionsRecentes(List.of())
                    .clientsActifs(List.of())
                    .alertes(List.of())
                    .journalActuel(null)

                    .lastUpdate(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la construction du dashboard: {}", e.getMessage(), e);
            throw new BusinessException("Erreur construction dashboard: " + e.getMessage());
        }
    }

    /**
     * ✅ CONSTRUCTION COMPLÈTE DU DASHBOARD AVEC DONNÉES RÉELLES
     */
    private CollecteurDashboardDTO buildCompleteDashboard(Collecteur collecteur) {
        log.info("🔨 Construction du dashboard complet pour collecteur: {}", collecteur.getId());

        try {
            // ÉTAPE 1: Compter les clients
            Long totalClientsCount = clientRepository.countByCollecteurId(collecteur.getId());
            Integer totalClients = totalClientsCount != null ? totalClientsCount.intValue() : 0;
            log.info("📊 Clients trouvés: {}", totalClients);

            // ÉTAPE 2: Statistiques de base (pour commencer)
            return CollecteurDashboardDTO.builder()
                    .collecteurId(collecteur.getId())
                    .totalClients(totalClients)
                    .totalEpargne(0.0) // TODO: Implémenter le calcul réel
                    .totalRetraits(0.0) // TODO: Implémenter le calcul réel
                    .soldeTotal(0.0) // TODO: Implémenter le calcul réel
                    .transactionsRecentes(List.of()) // TODO: Récupérer les vraies transactions
                    .journalActuel(null) // TODO: Récupérer le journal actuel
                    .lastUpdate(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la construction du dashboard: {}", e.getMessage());
            throw new BusinessException("Erreur construction dashboard: " + e.getMessage());
        }
    }


    private Long extractUserIdFromToken(Authentication auth) {
        try {
            if (auth != null && auth.getPrincipal() instanceof JwtAuthenticationFilter.JwtUserPrincipal) {
                JwtAuthenticationFilter.JwtUserPrincipal principal =
                        (JwtAuthenticationFilter.JwtUserPrincipal) auth.getPrincipal();
                return principal.getUserId();
            }

            // ✅ FALLBACK: Récupérer depuis les détails de l'Authentication
            if (auth != null && auth.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) auth.getDetails();
                Object userId = details.get("userId");
                if (userId instanceof Number) {
                    return ((Number) userId).longValue();
                }
            }

            log.warn("❌ Impossible d'extraire userId du token");
            return null;
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'extraction userId: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ VERSION DE DÉBOGAGE SANS SÉCURITÉ POUR TESTER LE ROUTING
     */
    @GetMapping("/{id}/dashboard-debug")
    public ResponseEntity<Map<String, Object>> getDashboardDebug(@PathVariable Long id) {
        log.info("🎯🎯🎯 DASHBOARD DEBUG APPELÉ POUR COLLECTEUR: {}", id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "DEBUG_OK");
        response.put("collecteurId", id);
        response.put("timestamp", System.currentTimeMillis());
        response.put("authenticated", auth != null && auth.isAuthenticated());

        if (auth != null) {
            response.put("username", auth.getName());
            response.put("authorities", auth.getAuthorities().toString());
            response.put("principalType", auth.getPrincipal().getClass().getSimpleName());

            if (auth.getPrincipal() instanceof JwtAuthenticationFilter.JwtUserPrincipal) {
                JwtAuthenticationFilter.JwtUserPrincipal principal =
                        (JwtAuthenticationFilter.JwtUserPrincipal) auth.getPrincipal();
                response.put("userId", principal.getUserId());
                response.put("agenceId", principal.getAgenceId());
                response.put("role", principal.getRole());
            }

            if (auth.getDetails() != null) {
                response.put("authDetails", auth.getDetails().toString());
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ VERSION SIMPLIFIÉE AVEC SÉCURITÉ BASIQUE
     */
    @GetMapping("/{id}/dashboard-simple")
    @PreAuthorize("hasRole('COLLECTEUR')")
    public ResponseEntity<Map<String, Object>> getDashboardSimple(@PathVariable Long id) {
        log.info("🎯🎯🎯 DASHBOARD SIMPLE APPELÉ POUR COLLECTEUR: {}", id);
        log.info("🔐 Sécurité basique passée avec succès");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SIMPLE_OK");
        response.put("collecteurId", id);
        response.put("message", "Sécurité basique fonctionne");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/mappings-check")
    public ResponseEntity<String> checkMappings() {
        log.info("🎯 MAPPINGS CHECK APPELÉ");
        return ResponseEntity.ok("CollecteurController est bien configuré");
    }
}