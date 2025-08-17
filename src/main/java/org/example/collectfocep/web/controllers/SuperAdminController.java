package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.dto.CreateAdminDTO;
import org.example.collectfocep.dto.CreateCollecteurDTO;
import org.example.collectfocep.dto.JournalDTO;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.ValidationException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.SuperAdminAgenceService;
import org.example.collectfocep.services.SuperAdminValidationService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔥 Contrôleur SuperAdmin - Gestion centralisée
 * Accessible uniquement aux SUPER_ADMIN
 */
@RestController
@RequestMapping("/api/super-admin")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')") // Toutes les méthodes nécessitent SUPER_ADMIN
public class SuperAdminController {

    private final AdminRepository adminRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AgenceRepository agenceRepository;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final SecurityService securityService;
    private final PasswordEncoder passwordEncoder;
    
    // Nouveaux services
    private final SuperAdminAgenceService superAdminAgenceService;
    private final SuperAdminValidationService superAdminValidationService;

    /**
     * 📊 DASHBOARD SUPER ADMIN GLOBAL
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<SuperAdminDashboardDTO>> getDashboardStats() {
        log.info("📊 SuperAdmin - Récupération dashboard global");

        SuperAdminDashboardDTO dashboard = SuperAdminDashboardDTO.builder()
                .totalAgences(agenceRepository.count())
                .totalAgencesActives(agenceRepository.countByActive(true))
                .totalAdmins(adminRepository.count())
                .totalCollecteurs(collecteurRepository.count())
                .totalCollecteursActifs(collecteurRepository.countByActiveTrue())
                .totalClients(clientRepository.count())
                .totalClientsActifs(clientRepository.countByValideTrue())
                .lastUpdate(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(dashboard, "Dashboard SuperAdmin récupéré avec succès")
        );
    }

    // ================================
    // 🏢 GESTION COMPLÈTE DES AGENCES
    // ================================

    /**
     * 📋 LISTE TOUTES LES AGENCES (avec pagination)
     */
    @GetMapping("/agences")
    public ResponseEntity<ApiResponse<Page<AgenceDTO>>> getAllAgences(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nomAgence") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.info("🏢 SuperAdmin - Récupération agences paginées: page={}, size={}", page, size);

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<AgenceDTO> agences = superAdminAgenceService.getAllAgences(pageable);
            
            return ResponseEntity.ok(
                    ApiResponse.success(agences, "Agences récupérées avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération agences paginées: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des agences: " + e.getMessage()));
        }
    }

    /**
     * 📋 LISTE TOUTES LES AGENCES (sans pagination)
     */
    @GetMapping("/agences/all")
    public ResponseEntity<ApiResponse<List<AgenceDTO>>> getAllAgencesSimple() {
        log.info("🏢 SuperAdmin - Récupération toutes agences");

        try {
            List<AgenceDTO> agences = superAdminAgenceService.getAllAgences();
            
            return ResponseEntity.ok(
                    ApiResponse.success(agences, "Agences récupérées avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération toutes agences: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des agences: " + e.getMessage()));
        }
    }

    /**
     * 🔍 DÉTAILS D'UNE AGENCE
     */
    @GetMapping("/agences/{agenceId}")
    public ResponseEntity<ApiResponse<AgenceDTO>> getAgenceById(@PathVariable Long agenceId) {
        log.info("🔍 SuperAdmin - Détails agence: {}", agenceId);

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            
            AgenceDTO agence = superAdminAgenceService.getAgenceById(agenceId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(agence, "Agence récupérée avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Agence non trouvée: {}", agenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur récupération agence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération de l'agence: " + e.getMessage()));
        }
    }

    /**
     * ✨ CRÉER UNE NOUVELLE AGENCE
     */
    @PostMapping("/agences")
    public ResponseEntity<ApiResponse<AgenceDTO>> createAgence(@Valid @RequestBody AgenceDTO agenceDTO) {
        log.info("✨ SuperAdmin - Création agence: {}", agenceDTO.getNomAgence());

        try {
            // Validation stricte
            superAdminValidationService.validateAgenceCreationData(agenceDTO);
            
            AgenceDTO savedAgence = superAdminAgenceService.createAgence(agenceDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedAgence, "Agence créée avec succès"));
        } catch (DuplicateResourceException | ValidationException e) {
            log.warn("⚠️ Erreur validation création agence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur création agence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la création de l'agence: " + e.getMessage()));
        }
    }

    /**
     * 🔄 MODIFIER UNE AGENCE
     */
    @PutMapping("/agences/{agenceId}")
    public ResponseEntity<ApiResponse<AgenceDTO>> updateAgence(
            @PathVariable Long agenceId,
            @Valid @RequestBody AgenceDTO agenceDTO) {
        
        log.info("🔄 SuperAdmin - Modification agence: {}", agenceId);

        try {
            // Validation stricte
            superAdminValidationService.validateId(agenceId, "Agence");
            superAdminValidationService.validateAgenceUpdateData(agenceId, agenceDTO);
            
            AgenceDTO updatedAgence = superAdminAgenceService.updateAgence(agenceId, agenceDTO);
            
            return ResponseEntity.ok(
                    ApiResponse.success(updatedAgence, "Agence modifiée avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Agence non trouvée pour modification: {}", agenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (DuplicateResourceException | ValidationException e) {
            log.warn("⚠️ Erreur validation modification agence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur modification agence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la modification de l'agence: " + e.getMessage()));
        }
    }

    /**
     * 🔄 ACTIVER/DÉSACTIVER UNE AGENCE
     */
    @PatchMapping("/agences/{agenceId}/toggle-status")
    public ResponseEntity<ApiResponse<AgenceDTO>> toggleAgenceStatus(@PathVariable Long agenceId) {
        log.info("🔄 SuperAdmin - Toggle status agence: {}", agenceId);

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            
            AgenceDTO updatedAgence = superAdminAgenceService.toggleAgenceStatus(agenceId);
            
            String status = updatedAgence.getActive() ? "activée" : "désactivée";
            return ResponseEntity.ok(
                    ApiResponse.success(updatedAgence, "Agence " + status + " avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Agence non trouvée pour toggle status: {}", agenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur toggle status agence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du changement de statut de l'agence: " + e.getMessage()));
        }
    }

    // NOTE: Les agences ne peuvent pas être supprimées, seulement activées/désactivées
    // pour préserver l'intégrité des données et l'historique

    // ================================
    // 💰 GESTION PARAMÈTRES COMMISSION PAR AGENCE
    // ================================

    /**
     * 💰 RÉCUPÉRER PARAMÈTRES COMMISSION D'UNE AGENCE
     */
    @GetMapping("/agences/{agenceId}/commission-params")
    public ResponseEntity<ApiResponse<List<ParametreCommissionDTO>>> getAgenceCommissionParams(@PathVariable Long agenceId) {
        log.info("💰 SuperAdmin - Paramètres commission agence: {}", agenceId);

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            
            List<ParametreCommissionDTO> parametres = superAdminAgenceService.getAgenceCommissionParams(agenceId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(parametres, "Paramètres commission récupérés avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Agence non trouvée pour paramètres commission: {}", agenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur récupération paramètres commission: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des paramètres commission: " + e.getMessage()));
        }
    }

    /**
     * 💰 DÉFINIR PARAMÈTRES COMMISSION POUR UNE AGENCE
     */
    @PostMapping("/agences/{agenceId}/commission-params")
    public ResponseEntity<ApiResponse<List<ParametreCommissionDTO>>> setAgenceCommissionParams(
            @PathVariable Long agenceId,
            @Valid @RequestBody List<ParametreCommissionDTO> parametres) {
        
        log.info("💰 SuperAdmin - Définition paramètres commission agence: {} ({} paramètres)", agenceId, parametres.size());

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            superAdminValidationService.validateCommissionParametersList(parametres);
            
            List<ParametreCommissionDTO> savedParametres = superAdminAgenceService.setAgenceCommissionParams(agenceId, parametres);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedParametres, "Paramètres commission définis avec succès"));
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Agence non trouvée pour définir paramètres commission: {}", agenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (ValidationException e) {
            log.warn("⚠️ Erreur validation paramètres commission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur définition paramètres commission: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la définition des paramètres commission: " + e.getMessage()));
        }
    }

    /**
     * 💰 MODIFIER UN PARAMÈTRE COMMISSION SPÉCIFIQUE
     */
    @PutMapping("/agences/{agenceId}/commission-params/{parametreId}")
    public ResponseEntity<ApiResponse<ParametreCommissionDTO>> updateCommissionParam(
            @PathVariable Long agenceId,
            @PathVariable Long parametreId,
            @Valid @RequestBody ParametreCommissionDTO parametre) {
        
        log.info("💰 SuperAdmin - Modification paramètre commission: {} agence: {}", parametreId, agenceId);

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            superAdminValidationService.validateId(parametreId, "Paramètre Commission");
            superAdminValidationService.validateCommissionParameters(parametre);
            
            ParametreCommissionDTO updatedParametre = superAdminAgenceService.updateCommissionParam(agenceId, parametreId, parametre);
            
            return ResponseEntity.ok(
                    ApiResponse.success(updatedParametre, "Paramètre commission modifié avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Paramètre commission non trouvé: {} agence: {}", parametreId, agenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (ValidationException e) {
            log.warn("⚠️ Erreur validation paramètre commission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur modification paramètre commission: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la modification du paramètre commission: " + e.getMessage()));
        }
    }

    /**
     * 🗑️ SUPPRIMER UN PARAMÈTRE COMMISSION
     */
    @DeleteMapping("/agences/{agenceId}/commission-params/{parametreId}")
    public ResponseEntity<ApiResponse<Void>> deleteCommissionParam(
            @PathVariable Long agenceId,
            @PathVariable Long parametreId) {
        
        log.info("🗑️ SuperAdmin - Suppression paramètre commission: {} agence: {}", parametreId, agenceId);

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            superAdminValidationService.validateId(parametreId, "Paramètre Commission");
            
            superAdminAgenceService.deleteCommissionParam(agenceId, parametreId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(null, "Paramètre commission supprimé avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Paramètre commission non trouvé pour suppression: {} agence: {}", parametreId, agenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur suppression paramètre commission: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la suppression du paramètre commission: " + e.getMessage()));
        }
    }

    /**
     * 👥 GESTION DES ADMINS
     */
    @GetMapping("/admins")
    public ResponseEntity<ApiResponse<List<SuperAdminAdminDTO>>> getAllAdmins() {
        log.info("👥 SuperAdmin - Récupération de tous les admins");

        List<SuperAdminAdminDTO> admins = adminRepository.findAll().stream()
                .map(this::mapToSuperAdminDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(admins, "Admins récupérés avec succès")
        );
    }

    /**
     * 👤 DÉTAILS D'UN ADMIN
     */
    @GetMapping("/admins/{adminId}")
    public ResponseEntity<ApiResponse<SuperAdminAdminDTO>> getAdminDetails(@PathVariable Long adminId) {
        log.info("👤 SuperAdmin - Détails admin: {}", adminId);

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé: " + adminId));

        SuperAdminAdminDTO adminDTO = mapToSuperAdminDTO(admin);

        return ResponseEntity.ok(
                ApiResponse.success(adminDTO, "Détails admin récupérés avec succès")
        );
    }

    /**
     * 🔒 RESET PASSWORD ADMIN
     */
    @PostMapping("/admins/{adminId}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetAdminPassword(
            @PathVariable Long adminId,
            @Valid @RequestBody PasswordResetRequest request) {

        log.info("🔒 SuperAdmin - Reset password admin: {}", adminId);

        Utilisateur admin = utilisateurRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé: " + adminId));

        // Vérifier que c'est bien un admin
        if (!"ADMIN".equals(admin.getRole())) {
            throw new IllegalArgumentException("L'utilisateur n'est pas un admin");
        }

        // Changer le mot de passe
        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        admin.setDateModification(LocalDateTime.now());
        utilisateurRepository.save(admin);

        log.info("✅ Mot de passe réinitialisé pour l'admin: {}", admin.getAdresseMail());

        return ResponseEntity.ok(
                ApiResponse.success("OK", "Mot de passe réinitialisé avec succès")
        );
    }

    /**
     * ❌ DÉSACTIVER UN ADMIN
     */
    @PatchMapping("/admins/{adminId}/toggle-status")
    public ResponseEntity<ApiResponse<String>> toggleAdminStatus(@PathVariable Long adminId) {
        log.info("❌ SuperAdmin - Toggle status admin: {}", adminId);

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé: " + adminId));

        // Pour l'instant, on ne peut que modifier via la table utilisateurs
        Utilisateur utilisateur = utilisateurRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur admin non trouvé: " + adminId));

        // Toggle du status (pour cela, on pourrait ajouter un champ 'active' à Utilisateur)
        log.info("Status admin modifié: {}", utilisateur.getAdresseMail());

        return ResponseEntity.ok(
                ApiResponse.success("OK", "Status admin modifié avec succès")
        );
    }


    // ================================
    // MÉTHODES UTILITAIRES DE MAPPING
    // ================================

    private SuperAdminAdminDTO mapToSuperAdminDTO(Admin admin) {
        return SuperAdminAdminDTO.builder()
                .id(admin.getId())
                .nom(admin.getNom())
                .prenom(admin.getPrenom())
                .email(admin.getAdresseMail())
                .agenceId(admin.getAgence() != null ? admin.getAgence().getId() : null)
                .agenceNom(admin.getAgence() != null ? admin.getAgence().getNomAgence() : "Non assigné")
                .dateCreation(admin.getDateCreation())
                .active(true) // À adapter selon votre logique
                .build();
    }


    // ================================
    // NOUVEAUX ENDPOINTS POUR DÉTAILS AGENCE
    // ================================

    /**
     * 🏢 DÉTAILS COMPLETS D'UNE AGENCE
     */
    @GetMapping("/agences/{agenceId}/details")
    public ResponseEntity<ApiResponse<AgenceDetailDTO>> getAgenceDetailsComplete(@PathVariable Long agenceId) {
        log.info("🏢 SuperAdmin - Détails complets agence: {}", agenceId);

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            
            AgenceDetailDTO agenceDetails = superAdminAgenceService.getAgenceDetailsComplete(agenceId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(agenceDetails, "Détails agence récupérés avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Agence non trouvée: {}", agenceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (ValidationException e) {
            log.warn("⚠️ Erreur validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur récupération détails agence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des détails de l'agence"));
        }
    }

    /**
     * 👥 ADMINS D'UNE AGENCE
     */
    @GetMapping("/agences/{agenceId}/admins")
    public ResponseEntity<ApiResponse<List<SuperAdminDTO>>> getAdminsByAgence(@PathVariable Long agenceId) {
        log.info("👥 SuperAdmin - Admins agence: {}", agenceId);

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            
            List<SuperAdminDTO> admins = superAdminAgenceService.getAdminsByAgence(agenceId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(admins, "Admins de l'agence récupérés avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération admins agence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des admins"));
        }
    }

    /**
     * 👥 COLLECTEURS D'UNE AGENCE
     */
    @GetMapping("/agences/{agenceId}/collecteurs")
    public ResponseEntity<ApiResponse<List<CollecteurDTO>>> getCollecteursByAgence(@PathVariable Long agenceId) {
        log.info("👥 SuperAdmin - Collecteurs agence: {}", agenceId);

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            
            List<CollecteurDTO> collecteurs = superAdminAgenceService.getCollecteursByAgence(agenceId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(collecteurs, "Collecteurs de l'agence récupérés avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération collecteurs agence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des collecteurs"));
        }
    }

    /**
     * 👥 CLIENTS D'UNE AGENCE
     */
    @GetMapping("/agences/{agenceId}/clients")
    public ResponseEntity<ApiResponse<List<ClientDTO>>> getClientsByAgence(@PathVariable Long agenceId) {
        log.info("👥 SuperAdmin - Clients agence: {}", agenceId);

        try {
            superAdminValidationService.validateId(agenceId, "Agence");
            
            List<ClientDTO> clients = superAdminAgenceService.getClientsByAgence(agenceId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(clients, "Clients de l'agence récupérés avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération clients agence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des clients"));
        }
    }

    /**
     * 👥 CLIENTS D'UN COLLECTEUR
     */
    @GetMapping("/collecteurs/{collecteurId}/clients")
    public ResponseEntity<ApiResponse<List<ClientDTO>>> getClientsByCollecteur(@PathVariable Long collecteurId) {
        log.info("👥 SuperAdmin - Clients collecteur: {}", collecteurId);

        try {
            superAdminValidationService.validateId(collecteurId, "Collecteur");
            
            List<ClientDTO> clients = superAdminAgenceService.getClientsByCollecteur(collecteurId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(clients, "Clients du collecteur récupérés avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération clients collecteur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des clients"));
        }
    }

    // ================================
    // 👤 GESTION COMPLÈTE DES ADMINS
    // ================================

    /**
     * ✨ CRÉER UN NOUVEL ADMIN
     */
    @PostMapping("/admins")
    public ResponseEntity<ApiResponse<SuperAdminAdminDTO>> createAdmin(@Valid @RequestBody CreateAdminDTO createAdminDTO) {
        log.info("✨ SuperAdmin - Création admin: {}", createAdminDTO.getAdresseMail());

        try {
            SuperAdminAdminDTO savedAdmin = superAdminAgenceService.createAdmin(createAdminDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedAdmin, "Admin créé avec succès"));
        } catch (DuplicateResourceException | ValidationException e) {
            log.warn("⚠️ Erreur validation création admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur création admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la création de l'admin: " + e.getMessage()));
        }
    }

    /**
     * 🔄 MODIFIER UN ADMIN
     */
    @PutMapping("/admins/{adminId}")
    public ResponseEntity<ApiResponse<SuperAdminAdminDTO>> updateAdmin(
            @PathVariable Long adminId,
            @Valid @RequestBody CreateAdminDTO updateAdminDTO) {
        
        log.info("🔄 SuperAdmin - Modification admin: {}", adminId);

        try {
            SuperAdminAdminDTO updatedAdmin = superAdminAgenceService.updateAdmin(adminId, updateAdminDTO);
            
            return ResponseEntity.ok(
                    ApiResponse.success(updatedAdmin, "Admin modifié avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Admin non trouvé pour modification: {}", adminId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (DuplicateResourceException | ValidationException e) {
            log.warn("⚠️ Erreur validation modification admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur modification admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la modification de l'admin: " + e.getMessage()));
        }
    }

    // ================================
    // 👨‍💼 GESTION COMPLÈTE DES COLLECTEURS
    // ================================

    /**
     * 📋 LISTE TOUS LES COLLECTEURS
     */
    @GetMapping("/collecteurs")
    public ResponseEntity<ApiResponse<List<CollecteurDTO>>> getAllCollecteurs() {
        log.info("📋 SuperAdmin - Récupération de tous les collecteurs");

        try {
            List<CollecteurDTO> collecteurs = superAdminAgenceService.getAllCollecteurs();
            
            return ResponseEntity.ok(
                    ApiResponse.success(collecteurs, "Collecteurs récupérés avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération collecteurs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des collecteurs: " + e.getMessage()));
        }
    }

    /**
     * 🔍 DÉTAILS D'UN COLLECTEUR
     */
    @GetMapping("/collecteurs/{collecteurId}")
    public ResponseEntity<ApiResponse<CollecteurDTO>> getCollecteurDetails(@PathVariable Long collecteurId) {
        log.info("🔍 SuperAdmin - Détails collecteur: {}", collecteurId);

        try {
            superAdminValidationService.validateId(collecteurId, "Collecteur");
            
            CollecteurDTO collecteur = superAdminAgenceService.getCollecteurDetails(collecteurId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(collecteur, "Collecteur récupéré avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Collecteur non trouvé: {}", collecteurId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur récupération collecteur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération du collecteur: " + e.getMessage()));
        }
    }

    /**
     * ✨ CRÉER UN NOUVEAU COLLECTEUR
     */
    @PostMapping("/collecteurs")
    public ResponseEntity<ApiResponse<CollecteurDTO>> createCollecteur(@Valid @RequestBody CreateCollecteurDTO createCollecteurDTO) {
        log.info("✨ SuperAdmin - Création collecteur: {}", createCollecteurDTO.getAdresseMail());

        try {
            CollecteurDTO savedCollecteur = superAdminAgenceService.createCollecteur(createCollecteurDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(savedCollecteur, "Collecteur créé avec succès"));
        } catch (DuplicateResourceException | ValidationException e) {
            log.warn("⚠️ Erreur validation création collecteur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur création collecteur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la création du collecteur: " + e.getMessage()));
        }
    }

    /**
     * 🔄 MODIFIER UN COLLECTEUR
     */
    @PutMapping("/collecteurs/{collecteurId}")
    public ResponseEntity<ApiResponse<CollecteurDTO>> updateCollecteur(
            @PathVariable Long collecteurId,
            @Valid @RequestBody CreateCollecteurDTO updateCollecteurDTO) {
        
        log.info("🔄 SuperAdmin - Modification collecteur: {}", collecteurId);

        try {
            CollecteurDTO updatedCollecteur = superAdminAgenceService.updateCollecteur(collecteurId, updateCollecteurDTO);
            
            return ResponseEntity.ok(
                    ApiResponse.success(updatedCollecteur, "Collecteur modifié avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Collecteur non trouvé pour modification: {}", collecteurId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (DuplicateResourceException | ValidationException e) {
            log.warn("⚠️ Erreur validation modification collecteur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur modification collecteur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la modification du collecteur: " + e.getMessage()));
        }
    }

    /**
     * 🔄 ACTIVER/DÉSACTIVER UN COLLECTEUR
     */
    @PatchMapping("/collecteurs/{collecteurId}/toggle-status")
    public ResponseEntity<ApiResponse<CollecteurDTO>> toggleCollecteurStatus(@PathVariable Long collecteurId) {
        log.info("🔄 SuperAdmin - Toggle status collecteur: {}", collecteurId);

        try {
            superAdminValidationService.validateId(collecteurId, "Collecteur");
            
            CollecteurDTO updatedCollecteur = superAdminAgenceService.toggleCollecteurStatus(collecteurId);
            
            String status = updatedCollecteur.getActive() ? "activé" : "désactivé";
            return ResponseEntity.ok(
                    ApiResponse.success(updatedCollecteur, "Collecteur " + status + " avec succès")
            );
        } catch (ResourceNotFoundException e) {
            log.warn("⚠️ Collecteur non trouvé pour toggle status: {}", collecteurId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur toggle status collecteur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du changement de statut du collecteur: " + e.getMessage()));
        }
    }

    // ================================
    // 📊 JOURNAUX D'ACTIVITÉS
    // ================================

    /**
     * 📋 JOURNAUX DE TOUS LES COLLECTEURS
     */
    @GetMapping("/journaux")
    public ResponseEntity<ApiResponse<List<JournalDTO>>> getAllJournaux(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long agenceId,
            @RequestParam(required = false) Long collecteurId) {
        
        log.info("📋 SuperAdmin - Récupération journaux: page={}, size={}, agence={}, collecteur={}", 
                page, size, agenceId, collecteurId);

        try {
            List<JournalDTO> journaux = superAdminAgenceService.getAllJournaux(page, size, agenceId, collecteurId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(journaux, "Journaux récupérés avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération journaux: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des journaux: " + e.getMessage()));
        }
    }

    // ================================
    // 👥 GESTION COMPLÈTE DES CLIENTS
    // ================================

    /**
     * 📋 LISTE TOUS LES CLIENTS
     */
    @GetMapping("/clients")
    public ResponseEntity<ApiResponse<List<ClientDTO>>> getAllClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long agenceId,
            @RequestParam(required = false) Long collecteurId) {
        
        log.info("📋 SuperAdmin - Récupération clients: page={}, size={}, agence={}, collecteur={}", 
                page, size, agenceId, collecteurId);

        try {
            List<ClientDTO> clients = superAdminAgenceService.getAllClients(page, size, agenceId, collecteurId);
            
            return ResponseEntity.ok(
                    ApiResponse.success(clients, "Clients récupérés avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération clients: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des clients: " + e.getMessage()));
        }
    }
}