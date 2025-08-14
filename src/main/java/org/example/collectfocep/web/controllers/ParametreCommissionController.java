package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.util.ApiResponse;
import org.example.collectfocep.dto.CreateParametreCommissionRequest;
import org.example.collectfocep.dto.ParametreCommissionDTO;
import org.example.collectfocep.entities.ParametreCommission;
import org.example.collectfocep.services.ParametreCommissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/super-admin/parametres-commission")
@RequiredArgsConstructor
public class ParametreCommissionController {

    private final ParametreCommissionService parametreCommissionService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ParametreCommissionDTO>>> getAllParametres() {
        log.info("🔧 Récupération de tous les paramètres de commission");
        
        try {
            List<ParametreCommissionDTO> parametres = parametreCommissionService.getAllParametres();
            
            return ResponseEntity.ok(ApiResponse.success(parametres, 
                "Paramètres de commission récupérés avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des paramètres de commission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des paramètres de commission"));
        }
    }

    @GetMapping("/agence/{agenceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ParametreCommissionDTO>>> getParametresByAgence(@PathVariable Long agenceId) {
        log.info("🔧 Récupération des paramètres de commission pour l'agence {}", agenceId);
        
        try {
            List<ParametreCommissionDTO> parametres = parametreCommissionService.getParametresByAgence(agenceId);
            
            return ResponseEntity.ok(ApiResponse.success(parametres, 
                "Paramètres de commission récupérés avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des paramètres de commission pour l'agence {}", agenceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des paramètres de commission"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ParametreCommissionDTO>> getParametreById(@PathVariable Long id) {
        log.info("🔧 Récupération du paramètre de commission {}", id);
        
        try {
            return parametreCommissionService.getParametreById(id)
                    .map(parametre -> ResponseEntity.ok(ApiResponse.success(parametre,
                        "Paramètre de commission récupéré avec succès")))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du paramètre de commission {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération du paramètre de commission"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ParametreCommissionDTO>> createParametre(
            @Valid @RequestBody CreateParametreCommissionRequest request,
            Authentication authentication) {
        log.info("🔧 Création d'un nouveau paramètre de commission pour l'agence {}", request.getAgenceId());
        
        try {
            String currentUser = authentication.getName();
            ParametreCommissionDTO parametre = parametreCommissionService.createParametre(request, currentUser);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(parametre, "Paramètre de commission créé avec succès"));
        } catch (RuntimeException e) {
            log.warn("⚠️ Erreur lors de la création du paramètre de commission: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du paramètre de commission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la création du paramètre de commission"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ParametreCommissionDTO>> updateParametre(
            @PathVariable Long id,
            @Valid @RequestBody CreateParametreCommissionRequest request,
            Authentication authentication) {
        log.info("🔧 Mise à jour du paramètre de commission {}", id);
        
        try {
            String currentUser = authentication.getName();
            ParametreCommissionDTO parametre = parametreCommissionService.updateParametre(id, request, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success(parametre, "Paramètre de commission mis à jour avec succès"));
        } catch (RuntimeException e) {
            log.warn("⚠️ Erreur lors de la mise à jour du paramètre de commission: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour du paramètre de commission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la mise à jour du paramètre de commission"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteParametre(@PathVariable Long id) {
        log.info("🔧 Suppression du paramètre de commission {}", id);
        
        try {
            parametreCommissionService.deleteParametre(id);
            
            return ResponseEntity.ok(ApiResponse.success(null, "Paramètre de commission supprimé avec succès"));
        } catch (RuntimeException e) {
            log.warn("⚠️ Erreur lors de la suppression du paramètre de commission: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression du paramètre de commission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la suppression du paramètre de commission"));
        }
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activerParametre(@PathVariable Long id) {
        log.info("🔧 Activation du paramètre de commission {}", id);
        
        try {
            parametreCommissionService.activerParametre(id);
            
            return ResponseEntity.ok(ApiResponse.success(null, "Paramètre de commission activé avec succès"));
        } catch (RuntimeException e) {
            log.warn("⚠️ Erreur lors de l'activation du paramètre de commission: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'activation du paramètre de commission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de l'activation du paramètre de commission"));
        }
    }

    @GetMapping("/types-operation")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ParametreCommission.TypeOperation[]>> getTypesOperation() {
        log.info("🔧 Récupération des types d'opération disponibles");
        
        try {
            ParametreCommission.TypeOperation[] types = ParametreCommission.TypeOperation.values();
            
            return ResponseEntity.ok(ApiResponse.success(types,
                "Types d'opération récupérés avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des types d'opération", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des types d'opération"));
        }
    }

    @PostMapping("/calculer-commission")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BigDecimal>> calculerCommission(
            @RequestParam Long agenceId,
            @RequestParam ParametreCommission.TypeOperation typeOperation,
            @RequestParam BigDecimal montantTransaction) {
        log.info("🔧 Calcul de commission pour agence {}, opération {}, montant {}", 
                agenceId, typeOperation, montantTransaction);
        
        try {
            BigDecimal commission = parametreCommissionService.calculerCommission(
                    agenceId, typeOperation, montantTransaction);
            
            return ResponseEntity.ok(ApiResponse.success(commission, "Commission calculée avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur lors du calcul de commission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du calcul de commission"));
        }
    }
}