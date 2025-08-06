package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.RubriqueRemunerationDTO;
import org.example.collectfocep.entities.RubriqueRemuneration;
import org.example.collectfocep.repositories.RubriqueRemunerationRepository;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Contrôleur REST pour la gestion des rubriques de rémunération FOCEP v2
 * Permet de configurer les paramètres Vi pour le calcul des rémunérations
 */
@RestController
@RequestMapping("/api/v2/rubriques-remuneration")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class RubriqueRemunerationController {

    private final RubriqueRemunerationRepository rubriqueRepository;

    /**
     * Récupère les rubriques actives pour un collecteur
     * 
     * @param collecteurId ID du collecteur
     * @return Liste des rubriques applicables
     */
    @GetMapping("/collecteur/{collecteurId}")
    public ResponseEntity<ApiResponse<List<RubriqueRemuneration>>> getRubriquesByCollecteur(
            @PathVariable Long collecteurId) {
        try {
            log.info("📋 Récupération rubriques pour collecteur: {}", collecteurId);
            
            List<RubriqueRemuneration> rubriques = rubriqueRepository
                    .findActiveRubriquesByCollecteurId(collecteurId);
            
            log.info("✅ {} rubriques trouvées pour collecteur {}", rubriques.size(), collecteurId);
            
            return ResponseEntity.ok(
                ApiResponse.success(
                    rubriques, 
                    String.format("%d rubriques actives trouvées", rubriques.size())
                )
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération rubriques collecteur {}: {}", collecteurId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la récupération des rubriques"));
        }
    }

    /**
     * Récupère toutes les rubriques (pour admin)
     * 
     * @return Liste de toutes les rubriques
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RubriqueRemuneration>>> getAllRubriques() {
        try {
            log.info("📋 Récupération de toutes les rubriques");
            
            List<RubriqueRemuneration> rubriques = rubriqueRepository.findAll();
            
            return ResponseEntity.ok(
                ApiResponse.success(
                    rubriques, 
                    String.format("%d rubriques trouvées", rubriques.size())
                )
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération toutes rubriques: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la récupération des rubriques"));
        }
    }

    /**
     * Récupère une rubrique par son ID
     * 
     * @param id ID de la rubrique
     * @return Rubrique trouvée
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RubriqueRemuneration>> getRubriqueById(@PathVariable Long id) {
        try {
            log.info("🔍 Récupération rubrique ID: {}", id);
            
            Optional<RubriqueRemuneration> rubrique = rubriqueRepository.findById(id);
            
            if (rubrique.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(
                ApiResponse.success(rubrique.get(), "Rubrique trouvée")
            );
        } catch (Exception e) {
            log.error("❌ Erreur récupération rubrique {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la récupération de la rubrique"));
        }
    }

    /**
     * Crée une nouvelle rubrique de rémunération
     * 
     * @param rubriqueDTO Données de la rubrique
     * @return Rubrique créée
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RubriqueRemuneration>> createRubrique(
            @RequestBody RubriqueRemunerationDTO rubriqueDTO) {
        try {
            log.info("➕ Création rubrique: {}", rubriqueDTO.getNom());
            
            // Validation basique
            if (rubriqueDTO.getNom() == null || rubriqueDTO.getNom().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le nom de la rubrique est requis"));
            }
            
            if (rubriqueDTO.getType() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le type de rubrique est requis"));
            }
            
            if (rubriqueDTO.getValeur() == null || rubriqueDTO.getValeur().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La valeur doit être positive"));
            }
            
            // Conversion DTO vers Entity
            RubriqueRemuneration rubrique = RubriqueRemuneration.builder()
                .nom(rubriqueDTO.getNom().trim())
                .type(rubriqueDTO.getType())
                .valeur(rubriqueDTO.getValeur())
                .dateApplication(rubriqueDTO.getDateApplication())
                .delaiJours(rubriqueDTO.getDelaiJours())
                .collecteurIds(rubriqueDTO.getCollecteurIds())
                .active(true)
                .build();
            
            RubriqueRemuneration savedRubrique = rubriqueRepository.save(rubrique);
            
            log.info("✅ Rubrique créée avec ID: {}", savedRubrique.getId());
            
            return ResponseEntity.ok(
                ApiResponse.success(savedRubrique, "Rubrique créée avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur création rubrique: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la création de la rubrique"));
        }
    }

    /**
     * Met à jour une rubrique existante
     * 
     * @param id ID de la rubrique
     * @param rubriqueDTO Nouvelles données
     * @return Rubrique mise à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RubriqueRemuneration>> updateRubrique(
            @PathVariable Long id,
            @RequestBody RubriqueRemunerationDTO rubriqueDTO) {
        try {
            log.info("✏️ Mise à jour rubrique ID: {}", id);
            
            Optional<RubriqueRemuneration> existingRubrique = rubriqueRepository.findById(id);
            
            if (existingRubrique.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            RubriqueRemuneration rubrique = existingRubrique.get();
            
            // Mise à jour des champs
            if (rubriqueDTO.getNom() != null) {
                rubrique.setNom(rubriqueDTO.getNom().trim());
            }
            
            if (rubriqueDTO.getType() != null) {
                rubrique.setType(rubriqueDTO.getType());
            }
            
            if (rubriqueDTO.getValeur() != null) {
                rubrique.setValeur(rubriqueDTO.getValeur());
            }
            
            if (rubriqueDTO.getDateApplication() != null) {
                rubrique.setDateApplication(rubriqueDTO.getDateApplication());
            }
            
            if (rubriqueDTO.getDelaiJours() != null) {
                rubrique.setDelaiJours(rubriqueDTO.getDelaiJours());
            }
            
            if (rubriqueDTO.getCollecteurIds() != null) {
                rubrique.setCollecteurIds(rubriqueDTO.getCollecteurIds());
            }
            
            RubriqueRemuneration savedRubrique = rubriqueRepository.save(rubrique);
            
            log.info("✅ Rubrique {} mise à jour", id);
            
            return ResponseEntity.ok(
                ApiResponse.success(savedRubrique, "Rubrique mise à jour avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur mise à jour rubrique {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la mise à jour de la rubrique"));
        }
    }

    /**
     * Désactive une rubrique (soft delete)
     * 
     * @param id ID de la rubrique
     * @return Confirmation de désactivation
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deactivateRubrique(@PathVariable Long id) {
        try {
            log.info("❌ Désactivation rubrique ID: {}", id);
            
            Optional<RubriqueRemuneration> rubrique = rubriqueRepository.findById(id);
            
            if (rubrique.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            RubriqueRemuneration toDeactivate = rubrique.get();
            toDeactivate.setActive(false);
            
            rubriqueRepository.save(toDeactivate);
            
            log.info("✅ Rubrique {} désactivée", id);
            
            return ResponseEntity.ok(
                ApiResponse.success("OK", "Rubrique désactivée avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur désactivation rubrique {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la désactivation de la rubrique"));
        }
    }

    /**
     * Réactive une rubrique
     * 
     * @param id ID de la rubrique
     * @return Confirmation de réactivation
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<RubriqueRemuneration>> activateRubrique(@PathVariable Long id) {
        try {
            log.info("✅ Réactivation rubrique ID: {}", id);
            
            Optional<RubriqueRemuneration> rubrique = rubriqueRepository.findById(id);
            
            if (rubrique.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            RubriqueRemuneration toActivate = rubrique.get();
            toActivate.setActive(true);
            
            RubriqueRemuneration savedRubrique = rubriqueRepository.save(toActivate);
            
            log.info("✅ Rubrique {} réactivée", id);
            
            return ResponseEntity.ok(
                ApiResponse.success(savedRubrique, "Rubrique réactivée avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur réactivation rubrique {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Erreur lors de la réactivation de la rubrique"));
        }
    }
}