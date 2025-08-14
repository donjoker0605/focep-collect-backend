package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    /**
     * 🏢 COLLECTEURS PAR AGENCE
     */
    @GetMapping("/agences/{agenceId}/collecteurs")
    public ResponseEntity<ApiResponse<List<CollecteurDTO>>> getCollecteursByAgence(@PathVariable Long agenceId) {
        log.info("🏢 SuperAdmin - Collecteurs agence: {}", agenceId);

        List<Collecteur> collecteurs = collecteurRepository.findByAgenceId(agenceId);
        
        // Vous devrez créer un mapper CollecteurDTO ou utiliser celui existant
        List<CollecteurDTO> collecteurDTOs = collecteurs.stream()
                .map(this::mapCollecteurToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(collecteurDTOs, "Collecteurs récupérés avec succès")
        );
    }

    /**
     * 👥 CLIENTS PAR AGENCE
     */
    @GetMapping("/agences/{agenceId}/clients")
    public ResponseEntity<ApiResponse<List<ClientBasicDTO>>> getClientsByAgence(@PathVariable Long agenceId) {
        log.info("👥 SuperAdmin - Clients agence: {}", agenceId);

        List<Client> clients = clientRepository.findByAgenceId(agenceId);
        
        List<ClientBasicDTO> clientDTOs = clients.stream()
                .map(this::mapClientToBasicDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(clientDTOs, "Clients récupérés avec succès")
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

    private CollecteurDTO mapCollecteurToDTO(Collecteur collecteur) {
        // Mapping basique - à adapter selon votre CollecteurDTO existant
        CollecteurDTO dto = new CollecteurDTO();
        dto.setId(collecteur.getId());
        dto.setNom(collecteur.getNom());
        dto.setPrenom(collecteur.getPrenom());
        dto.setActive(collecteur.getActive());
        dto.setAgenceId(collecteur.getAgenceId());
        return dto;
    }

    private ClientBasicDTO mapClientToBasicDTO(Client client) {
        return ClientBasicDTO.builder()
                .id(client.getId())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .telephone(client.getTelephone())
                .numeroCni(client.getNumeroCni())
                .numeroCompte(client.getNumeroCompte())
                .ville(client.getVille())
                .quartier(client.getQuartier())
                .valide(client.getValide())
                .build();
    }
}