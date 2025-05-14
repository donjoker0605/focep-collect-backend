package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.security.config.RoleConfig;
import org.example.collectfocep.entities.Admin;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.AdminRepository;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.dto.AdminDTO;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur amélioré pour la gestion des utilisateurs
 */
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    private final AdminRepository adminRepository;
    private final AgenceRepository agenceRepository;
    private final SecurityService securityService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(AdminRepository adminRepository,
                          AgenceRepository agenceRepository,
                          SecurityService securityService,
                          PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.agenceRepository = agenceRepository;
        this.securityService = securityService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crée un administrateur (nécessite SUPER_ADMIN)
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Admin>> createAdmin(@Valid @RequestBody AdminDTO adminDto) {
        log.info("Création d'un nouvel administrateur pour l'agence: {}", adminDto.getAgenceId());

        // Vérifier que l'agence existe
        if (adminDto.getAgenceId() != null) {
            Agence agence = agenceRepository.findById(adminDto.getAgenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée"));
        }

        // Vérifier l'unicité de l'email
        if (adminRepository.existsByAdresseMail(adminDto.getEmail())) {
            throw new DuplicateResourceException("Un utilisateur avec cet email existe déjà");
        }

        Admin admin = Admin.builder()
                .nom(adminDto.getNom())
                .prenom(adminDto.getPrenom())
                .adresseMail(adminDto.getEmail())
                .password(passwordEncoder.encode(adminDto.getPassword()))
                .numeroCni(generateDefaultCni())  // Générer un CNI temporaire
                .telephone("000000000")           // Valeur par défaut
                .role(RoleConfig.ADMIN)
                .build();

        // Associer l'agence si fournie
        if (adminDto.getAgenceId() != null) {
            Agence agence = new Agence();
            agence.setId(adminDto.getAgenceId());
            admin.setAgence(agence);
        }

        Admin savedAdmin = adminRepository.save(admin);
        log.info("Admin créé avec succès: {}", savedAdmin.getAdresseMail());

        return ResponseEntity.ok(ApiResponse.success(savedAdmin, "Administrateur créé avec succès"));
    }

    /**
     * Met à jour un administrateur
     */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Admin>> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminDTO adminDto) {
        log.info("Mise à jour de l'administrateur: {}", id);

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé"));

        admin.setNom(adminDto.getNom());
        admin.setPrenom(adminDto.getPrenom());

        // Mettre à jour l'agence si fournie
        if (adminDto.getAgenceId() != null) {
            Agence agence = agenceRepository.findById(adminDto.getAgenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée"));
            admin.setAgence(agence);
        }

        Admin updatedAdmin = adminRepository.save(admin);
        return ResponseEntity.ok(ApiResponse.success(updatedAdmin, "Administrateur mis à jour avec succès"));
    }

    /**
     * Liste tous les administrateurs
     */
    @GetMapping("/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Admin>>> getAllAdmins() {
        log.info("Récupération de tous les administrateurs");
        List<Admin> admins = adminRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(admins, "Administrateurs récupérés avec succès"));
    }

    /**
     * Supprime un administrateur
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(@PathVariable Long id) {
        log.info("Suppression de l'administrateur: {}", id);

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin non trouvé"));

        // Empêcher la suppression du dernier super admin
        if (RoleConfig.SUPER_ADMIN.equals(admin.getRole())) {
            long superAdminCount = adminRepository.findAll().stream()
                    .mapToLong(a -> RoleConfig.SUPER_ADMIN.equals(a.getRole()) ? 1 : 0)
                    .sum();

            if (superAdminCount <= 1) {
                throw new IllegalStateException("Impossible de supprimer le dernier super administrateur");
            }
        }

        adminRepository.delete(admin);
        log.info("Admin supprimé avec succès: {}", id);

        return ResponseEntity.ok(ApiResponse.success(null, "Administrateur supprimé avec succès"));
    }

    /**
     * Génère un CNI temporaire unique
     */
    private String generateDefaultCni() {
        return "CNI" + System.currentTimeMillis();
    }
}