package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.AgenceDTO;
import org.example.collectfocep.dto.SuperAdminDTO;
import org.example.collectfocep.entities.Admin;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.repositories.AdminRepository;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.security.config.RoleConfig;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Contrôleur pour l'initialisation bootstrap du système
 * Permet la création du premier super admin et des agences
 */
@RestController
@RequestMapping("/api/bootstrap")
@Slf4j
@RequiredArgsConstructor
public class BootstrapController {

    private final AdminRepository adminRepository;
    private final AgenceRepository agenceRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crée le premier super admin si aucun n'existe
     * Cette méthode est accessible uniquement s'il n'y a aucun super admin
     */
    @PostMapping("/super-admin")
    public ResponseEntity<ApiResponse<Admin>> createSuperAdmin(@Valid @RequestBody SuperAdminDTO dto) {
        log.info("Tentative de création du super admin initial");

        // Vérifier qu'aucun super admin n'existe déjà
        boolean hasSuperAdmin = adminRepository.findAll().stream()
                .anyMatch(admin -> RoleConfig.SUPER_ADMIN.equals(admin.getRole()));

        if (hasSuperAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("SUPER_ADMIN_EXISTS",
                            "Un super administrateur existe déjà dans le système"));
        }

        // Créer le super admin
        Admin superAdmin = Admin.builder()
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .adresseMail(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .numeroCni(dto.getNumeroCni())
                .telephone(dto.getTelephone())
                .role(RoleConfig.SUPER_ADMIN)
                .build();

        Admin savedAdmin = adminRepository.save(superAdmin);
        log.info("Super admin créé avec succès: {}", savedAdmin.getAdresseMail());

        return ResponseEntity.ok(ApiResponse.success(savedAdmin, "Super admin créé avec succès"));
    }

    /**
     * Crée une nouvelle agence
     * Accessible seulement aux super admins
     */
    @PostMapping("/agence")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Agence>> createAgence(@Valid @RequestBody AgenceDTO dto) {
        log.info("Création d'une nouvelle agence: {}", dto.getNomAgence());

        // Générer un code agence unique si non fourni
        String codeAgence = dto.getCodeAgence();
        if (codeAgence == null || codeAgence.isEmpty()) {
            codeAgence = generateAgenceCode(dto.getNomAgence());
        }

        Agence agence = Agence.builder()
                .codeAgence(codeAgence)
                .nomAgence(dto.getNomAgence())
                .build();

        Agence savedAgence = agenceRepository.save(agence);
        log.info("Agence créée avec succès: {} - {}", savedAgence.getCodeAgence(), savedAgence.getNomAgence());

        return ResponseEntity.ok(ApiResponse.success(savedAgence, "Agence créée avec succès"));
    }

    /**
     * Vérifie l'état du bootstrap du système
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<BootstrapStatus>> getBootstrapStatus() {
        long totalAdmins = adminRepository.count();
        long superAdmins = adminRepository.findAll().stream()
                .mapToLong(admin -> RoleConfig.SUPER_ADMIN.equals(admin.getRole()) ? 1 : 0)
                .sum();
        long totalAgences = agenceRepository.count();

        BootstrapStatus status = BootstrapStatus.builder()
                .hasSuperAdmin(superAdmins > 0)
                .totalAdmins(totalAdmins)
                .totalAgences(totalAgences)
                .systemInitialized(superAdmins > 0 && totalAgences > 0)
                .build();

        return ResponseEntity.ok(ApiResponse.success(status, "Statut du bootstrap récupéré"));
    }

    /**
     * Génère un code agence unique basé sur le nom
     */
    private String generateAgenceCode(String nomAgence) {
        String prefix = nomAgence.replaceAll("[^A-Za-z]", "")
                .substring(0, Math.min(3, nomAgence.length()))
                .toUpperCase();
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + suffix;
    }

    /**
     * Classe pour le statut du bootstrap
     */
    @Getter
    @Builder
    public static class BootstrapStatus {
        private boolean hasSuperAdmin;
        private long totalAdmins;
        private long totalAgences;
        private boolean systemInitialized;
    }
}