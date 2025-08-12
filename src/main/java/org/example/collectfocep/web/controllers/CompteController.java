package org.example.collectfocep.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CompteDTO;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.mappers.CompteMapper;
import org.example.collectfocep.security.annotations.AgenceAccess;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.repositories.CompteSalaireCollecteurRepository;
import org.example.collectfocep.repositories.CompteManquantRepository;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comptes")
@Slf4j
public class CompteController {

    private final CompteService compteService;
    private final SecurityService securityService;
    private final CompteSalaireCollecteurRepository compteSalaireCollecteurRepository;
    private final CompteManquantRepository compteManquantRepository;
    private CompteMapper compteMapper;


    @Autowired
    public CompteController(CompteService compteService, SecurityService securityService, 
                           CompteSalaireCollecteurRepository compteSalaireCollecteurRepository,
                           CompteManquantRepository compteManquantRepository,
                           CompteMapper compteMapper) {
        this.compteService = compteService;
        this.securityService = securityService;
        this.compteSalaireCollecteurRepository = compteSalaireCollecteurRepository;
        this.compteManquantRepository = compteManquantRepository;
        this.compteMapper = compteMapper;
    }

    // Méthode existante
    @GetMapping("/agence/{agenceId}")
    @AgenceAccess
    public ResponseEntity<List<Compte>> getComptesByAgence(@PathVariable Long agenceId) {
        log.info("Récupération des comptes pour l'agence: {}", agenceId);
        return ResponseEntity.ok(compteService.findByAgenceId(agenceId));
    }

    // Nouvelle méthode avec pagination
    @GetMapping("/agence/{agenceId}/page")
    @AgenceAccess
    public ResponseEntity<ApiResponse<Page<Compte>>> getComptesByAgencePaginated(
            @PathVariable Long agenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Récupération paginée des comptes pour l'agence: {}", agenceId);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Compte> comptesPage = compteService.findByAgenceId(agenceId, pageRequest);

        ApiResponse<Page<Compte>> response = ApiResponse.success(comptesPage);
        response.addMeta("totalElements", comptesPage.getTotalElements());
        response.addMeta("totalPages", comptesPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    // Méthode existante
    @GetMapping("/collecteur/{collecteurId}")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<List<CompteDTO>> getComptesByCollecteur(@PathVariable Long collecteurId) {
        log.info("Récupération des comptes pour le collecteur: {}", collecteurId);
        List<Compte> comptes = compteService.findByCollecteurId(collecteurId);

        // Utilisation du mapper pour convertir les entités en DTOs
        List<CompteDTO> compteDTOs = compteMapper.toDTOList(comptes);

        return ResponseEntity.ok(compteDTOs);
    }

    // Nouvelle méthode avec pagination
    @GetMapping("/collecteur/{collecteurId}/page")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<Page<Compte>>> getComptesByCollecteurPaginated(
            @PathVariable Long collecteurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Récupération paginée des comptes pour le collecteur: {}", collecteurId);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Compte> comptesPage = compteService.findByCollecteurId(collecteurId, pageRequest);

        ApiResponse<Page<Compte>> response = ApiResponse.success(comptesPage);
        response.addMeta("totalElements", comptesPage.getTotalElements());
        response.addMeta("totalPages", comptesPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/solde")
    @PreAuthorize("@securityService.canAccessCompte(authentication, #id)")
    @Audited(action = "VIEW_SOLDE", entityType = "Compte")
    public ResponseEntity<ApiResponse<Double>> getSolde(@PathVariable Long id) {
        log.info("Consultation du solde du compte: {}", id);
        Double solde = compteService.getSolde(id);

        return ResponseEntity.ok(
                ApiResponse.success(solde, "Solde récupéré avec succès")
        );
    }

    // Ajout d'une méthode pour obtenir un compte par son ID
    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canAccessCompte(authentication, #id)")
    public ResponseEntity<ApiResponse<Compte>> getCompteById(@PathVariable Long id) {
        log.info("Récupération du compte avec l'ID: {}", id);
        return compteService.getCompteById(id)
                .map(compte -> ResponseEntity.ok(ApiResponse.success(compte, "Compte récupéré avec succès")))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ================================
    // 🔥 ENDPOINTS SOLDES COLLECTEUR
    // ================================

    /**
     * Récupérer le solde du compte salaire d'un collecteur
     */
    @GetMapping("/collecteur/{collecteurId}/salaire/solde")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<Double>> getCompteSalaireSolde(@PathVariable Long collecteurId) {
        log.info("💰 Consultation solde compte salaire collecteur: {}", collecteurId);
        
        try {
            CompteSalaireCollecteur compteSalaire = compteSalaireCollecteurRepository
                    .findByCollecteurId(collecteurId)
                    .orElse(null);
            Double solde = compteSalaire != null ? compteSalaire.getSolde() : 0.0;
            
            return ResponseEntity.ok(ApiResponse.success(solde, "Solde compte salaire récupéré"));
        } catch (Exception e) {
            log.error("❌ Erreur récupération solde compte salaire: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SALARY_BALANCE_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * Récupérer le solde du compte manquant d'un collecteur
     */
    @GetMapping("/collecteur/{collecteurId}/manquant/solde")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<Double>> getCompteManquantSolde(@PathVariable Long collecteurId) {
        log.info("💰 Consultation solde compte manquant collecteur: {}", collecteurId);
        
        try {
            CompteManquant compteManquant = compteManquantRepository
                    .findByCollecteurId(collecteurId)
                    .orElse(null);
            Double solde = compteManquant != null ? compteManquant.getSolde() : 0.0;
            
            return ResponseEntity.ok(ApiResponse.success(solde, "Solde compte manquant récupéré"));
        } catch (Exception e) {
            log.error("❌ Erreur récupération solde compte manquant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("MISSING_BALANCE_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    // Ajout d'une méthode pour sauvegarder un compte
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Audited(action = "CREATE", entityType = "Compte")
    public ResponseEntity<ApiResponse<Compte>> createCompte(@RequestBody Compte compte) {
        log.info("Création d'un nouveau compte");
        Compte savedCompte = compteService.saveCompte(compte);
        return ResponseEntity.ok(ApiResponse.success(savedCompte, "Compte créé avec succès"));
    }

    // Ajout d'une méthode pour mettre à jour un compte
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Audited(action = "UPDATE", entityType = "Compte")
    public ResponseEntity<ApiResponse<Compte>> updateCompte(@PathVariable Long id, @RequestBody Compte compte) {
        log.info("Mise à jour du compte avec l'ID: {}", id);
        compte.setId(id);
        Compte updatedCompte = compteService.saveCompte(compte);
        return ResponseEntity.ok(ApiResponse.success(updatedCompte, "Compte mis à jour avec succès"));
    }

    // Ajout d'une méthode pour supprimer un compte
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Audited(action = "DELETE", entityType = "Compte")
    public ResponseEntity<ApiResponse<Void>> deleteCompte(@PathVariable Long id) {
        log.info("Suppression du compte avec l'ID: {}", id);
        compteService.deleteCompte(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Compte supprimé avec succès"));
    }

    // Endpoint pour récupérer le compte d'un client
    @GetMapping("/client/{clientId}")
    @PreAuthorize("@securityService.canManageClient(authentication, #clientId)")
    public ResponseEntity<ApiResponse<List<CompteDTO>>> getComptesByClient(@PathVariable Long clientId) {
        log.info("Récupération des comptes pour le client: {}", clientId);

        // Récupérer les comptes du client
        List<Compte> comptes = compteService.findByClientId(clientId);
        List<CompteDTO> compteDTOs = compteMapper.toDTOList(comptes);

        return ResponseEntity.ok(ApiResponse.success(
                compteDTOs,
                "Comptes récupérés avec succès"
        ));
    }
}