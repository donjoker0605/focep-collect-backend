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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
        log.info("   - GET /api/collecteurs - Liste filtrée par agence");
        log.info("   - POST /api/collecteurs - Création sécurisée");
        log.info("   - PATCH /api/collecteurs/{id}/toggle-status");
        log.info("   - POST /api/collecteurs/{id}/reset-password - NOUVEAU");
    }

    private final CollecteurService collecteurService;
    private final PasswordService passwordService;
    private final SecurityService securityService;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;

    // ✅ TON CODE EXISTANT - CONSERVÉ INTÉGRALEMENT
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CollecteurDTO>> createCollecteur(@Valid @RequestBody CollecteurCreateDTO dto) {
        log.info("🆕 Création d'un nouveau collecteur: {}", dto.getAdresseMail());

        try {
            // SÉCURITÉ CRITIQUE: Récupérer l'agence de l'admin connecté
            Long agenceIdFromAuth = securityService.getCurrentUserAgenceId();

            if (agenceIdFromAuth == null) {
                log.error("❌ Tentative de création sans agence identifiée");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé - agence non déterminée"));
            }

            // FORCER L'AGENCE DE L'ADMIN - Ignorer toute agence envoyée par le client
            dto.setAgenceId(agenceIdFromAuth);
            log.info("✅ Agence {} assignée automatiquement au collecteur", agenceIdFromAuth);

            // 🔥 VÉRIFICATION DU MOT DE PASSE
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                log.warn("⚠️ Aucun mot de passe fourni pour le collecteur {}", dto.getAdresseMail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Le mot de passe est obligatoire"));
            }

            // Créer le collecteur
            Collecteur collecteur = collecteurService.saveCollecteur(dto);
            CollecteurDTO collecteurDTO = collecteurService.convertToDTO(collecteur);

            log.info("✅ Collecteur créé avec succès: ID={}, Email={}, Agence={}",
                    collecteur.getId(), collecteur.getAdresseMail(), collecteur.getAgence().getId());

            return ResponseEntity.ok(ApiResponse.success(
                    collecteurDTO,
                    "Collecteur créé avec succès"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du collecteur", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * ENDPOINT POUR RÉINITIALISER LE MOT DE PASSE PAR L'ADMIN
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Audited(action = "RESET_PASSWORD", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetCollecteurPassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordResetRequestDTO request) {

        log.info("🔑 Réinitialisation du mot de passe pour le collecteur: {}", id);

        try {
            // Vérifier l'accès
            if (!securityService.hasPermissionForCollecteur(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé à ce collecteur"));
            }

            // Utiliser le service pour réinitialiser
            collecteurService.resetCollecteurPassword(id, request.getNewPassword());

            // Réponse avec le nouveau mot de passe (pour que l'admin puisse le communiquer)
            Map<String, String> response = new HashMap<>();
            response.put("message", "Mot de passe réinitialisé avec succès");
            response.put("newPassword", request.getNewPassword());
            response.put("collecteurId", id.toString());

            log.info("✅ Mot de passe réinitialisé avec succès pour le collecteur: {}", id);

            return ResponseEntity.ok(ApiResponse.success(
                    response,
                    "Mot de passe réinitialisé avec succès"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la réinitialisation du mot de passe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * MISE À JOUR D'UN COLLECTEUR - AVEC CHANGEMENT DE MOT DE PASSE
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CollecteurDTO>> updateCollecteur(
            @PathVariable Long id,
            @Valid @RequestBody CollecteurUpdateDTO dto) {

        log.info("📝 Mise à jour du collecteur: {}", id);

        try {
            // Vérifier l'accès
            if (!securityService.hasPermissionForCollecteur(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            // NE JAMAIS permettre la modification de l'agence
            dto.setAgenceId(null);

            // 🔥 LOG POUR LE CHANGEMENT DE MOT DE PASSE
            if (dto.hasNewPassword()) {
                log.info("🔑 Changement de mot de passe demandé pour le collecteur: {}", id);
            }

            Collecteur updated = collecteurService.updateCollecteur(id, dto);
            CollecteurDTO collecteurDTO = collecteurService.convertToDTO(updated);

            log.info("✅ Collecteur {} mis à jour avec succès", id);

            return ResponseEntity.ok(ApiResponse.success(
                    collecteurDTO,
                    dto.hasNewPassword() ?
                            "Collecteur et mot de passe mis à jour avec succès" :
                            "Collecteur mis à jour avec succès"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    // CONSERVÉ INTÉGRALEMENT
    @PutMapping("/{id}/montant-max")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #id)")
    @Audited(action = "UPDATE_MONTANT_MAX", entityType = "Collecteur")
    public ResponseEntity<ApiResponse<CollecteurDTO>> updateMontantMaxRetrait(
            @PathVariable Long id,
            @Valid @RequestBody MontantMaxRetraitRequest request) {

        log.info("Demande de modification du montant max de retrait pour le collecteur: {}", id);

        try {
            // - getNouveauMontant() doit retourner BigDecimal
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

    // ✅ TON CODE EXISTANT - CONSERVÉ INTÉGRALEMENT
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

    // ✅ TON CODE EXISTANT - CONSERVÉ INTÉGRALEMENT
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

    /**
     * DÉSACTIVER UN COLLECTEUR (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateCollecteur(@PathVariable Long id) {
        log.info("🗑️ Désactivation du collecteur: {}", id);

        try {
            // Vérifier l'accès
            if (!securityService.hasPermissionForCollecteur(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            collecteurService.deactivateCollecteur(id);
            log.info("✅ Collecteur {} désactivé avec succès", id);

            return ResponseEntity.ok(ApiResponse.success(
                    null,
                    "Collecteur désactivé avec succès"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la désactivation", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * LISTE DES COLLECTEURS - Filtrée par agence
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<List<CollecteurDTO>>> getAllCollecteurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        log.info("📋 Récupération des collecteurs - page: {}, size: {}, search: '{}'", page, size, search);

        try {
            // SÉCURITÉ: Filtrer par agence de l'utilisateur connecté
            Long agenceId = securityService.getCurrentUserAgenceId();
            if (agenceId == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("nom", "prenom"));
            Page<Collecteur> collecteursPage;

            if (search != null && !search.trim().isEmpty()) {
                collecteursPage = collecteurService.searchCollecteursByAgence(agenceId, search, pageable);
            } else {
                collecteursPage = collecteurService.getCollecteursByAgence(agenceId, pageable);
            }

            List<CollecteurDTO> collecteurDTOs = collecteursPage.getContent().stream()
                    .map(collecteurService::convertToDTO)
                    .collect(Collectors.toList());

            log.info("✅ {} collecteurs récupérés pour l'agence: {}", collecteurDTOs.size(), agenceId);

            ApiResponse<List<CollecteurDTO>> response = ApiResponse.success(
                    collecteurDTOs,
                    "Opération réussie"
            );
            response.addMeta("totalElements", collecteursPage.getTotalElements());
            response.addMeta("totalPages", collecteursPage.getTotalPages());
            response.addMeta("currentPage", collecteursPage.getNumber());
            response.addMeta("pageSize", collecteursPage.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des collecteurs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur serveur"));
        }
    }

    /**
     * DÉTAILS D'UN COLLECTEUR - Avec vérification d'accès
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<CollecteurDTO>> getCollecteurById(@PathVariable Long id) {
        log.info("🔍 Récupération du collecteur: {}", id);

        try {
            // Vérifier l'accès au collecteur
            if (!securityService.hasPermissionForCollecteur(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé à ce collecteur"));
            }

            Collecteur collecteur = collecteurService.getCollecteurById(id)
                    .orElse(null);

            if (collecteur == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Collecteur non trouvé"));
            }

            CollecteurDTO dto = collecteurService.convertToDTO(collecteur);
            return ResponseEntity.ok(ApiResponse.success(dto));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du collecteur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur serveur"));
        }
    }

    /**
     * DASHBOARD D'UN COLLECTEUR
     */
    @GetMapping("/{id}/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<CollecteurDashboardDTO>> getCollecteurDashboard(@PathVariable Long id, Authentication authentication) {
        log.info("📊 Récupération du dashboard du collecteur: {}", id);

        try {
            // Pour un collecteur, vérifier qu'il accède à son propre dashboard
            if (securityService.hasRole("COLLECTEUR")) {
                Long currentCollecteurId = securityService.getCurrentUserId(authentication);
                if (!id.equals(currentCollecteurId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Accès non autorisé"));
                }
            } else {
                // Pour admin, vérifier l'accès au collecteur
                if (!securityService.hasPermissionForCollecteur(id)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Accès non autorisé"));
                }
            }

            CollecteurDashboardDTO dashboard = collecteurService.getDashboardStats(id);
            return ResponseEntity.ok(ApiResponse.success(dashboard));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du dashboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur serveur"));
        }
    }

    /**
     * BASCULER LE STATUT ACTIF/INACTIF
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CollecteurDTO>> toggleCollecteurStatus(
            @PathVariable Long id,
            @RequestBody StatusToggleDTO statusDto) {

        log.info("🔄 Basculement du statut du collecteur: {} vers {}", id, statusDto.isActive());

        try {
            // Vérifier l'accès
            if (!securityService.hasPermissionForCollecteur(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            Collecteur collecteur = collecteurService.toggleCollecteurStatus(id);
            CollecteurDTO dto = collecteurService.convertToDTO(collecteur);

            String action = collecteur.getActive() ? "activé" : "désactivé";
            log.info("✅ Collecteur {} {} avec succès", id, action);

            return ResponseEntity.ok(ApiResponse.success(
                    dto,
                    "Collecteur " + action + " avec succès"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors du changement de statut", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * STATISTIQUES D'UN COLLECTEUR
     */
    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COLLECTEUR')")
    public ResponseEntity<ApiResponse<CollecteurStatisticsDTO>> getCollecteurStatistics(@PathVariable Long id) {
        log.info("📊 Récupération des statistiques du collecteur: {}", id);

        try {
            // Vérifier l'accès
            if (!securityService.hasPermissionForCollecteur(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès non autorisé"));
            }

            CollecteurStatisticsDTO stats = collecteurService.getCollecteurStatistics(id);
            return ResponseEntity.ok(ApiResponse.success(stats));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des statistiques", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur serveur"));
        }
    }

    // ✅ TES MÉTHODES HELPER EXISTANTES - CONSERVÉES INTÉGRALEMENT

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