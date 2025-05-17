package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionTier;
import org.example.collectfocep.entities.CommissionType;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.CommissionParameterMapper;
import org.example.collectfocep.repositories.CommissionParameterRepository;
import org.example.collectfocep.security.annotations.AgenceAccess;
import org.example.collectfocep.services.CommissionValidationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commission-parameters")
@Slf4j
@RequiredArgsConstructor
public class CommissionParameterController {

    private final CommissionParameterRepository commissionParameterRepository;
    private final CommissionParameterMapper parameterMapper;
    private final CommissionValidationService validationService;

    /**
     * Créer un nouveau paramètre de commission
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createCommissionParameter(
            @Valid @RequestBody CommissionParameterDTO parameterDTO) {

        log.info("Création paramètre commission - Type: {}, Scope: {}",
                parameterDTO.getTypeCommission(), getScope(parameterDTO));

        try {
            // Validation du DTO
            if (!parameterDTO.isValidScope()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Un seul scope doit être défini (client, collecteur, ou agence)"));
            }

            // Mapping vers entité
            CommissionParameter parameter = parameterMapper.toEntity(parameterDTO);

            // Validation métier
            var validationResult = validationService.validateCommissionParameters(parameter);
            if (!validationResult.isValid()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Validation échouée",
                                "details", validationResult.getErrors()
                        ));
            }

            // Sauvegarde
            CommissionParameter saved = commissionParameterRepository.save(parameter);
            CommissionParameterDTO result = parameterMapper.toDTO(saved);

            log.info("Paramètre créé avec succès - ID: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            log.error("Erreur création paramètre: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupérer tous les paramètres de commission avec pagination
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<CommissionParameterDTO>> getAllCommissionParameters(
            Pageable pageable) {

        log.debug("Récupération paramètres commission - Page: {}", pageable.getPageNumber());

        Page<CommissionParameter> parameters = commissionParameterRepository.findAll(pageable);
        Page<CommissionParameterDTO> result = parameters.map(parameterMapper::toDTO);

        return ResponseEntity.ok(result);
    }

    /**
     * Récupérer un paramètre par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<CommissionParameterDTO> getCommissionParameter(@PathVariable Long id) {
        log.debug("Récupération paramètre commission - ID: {}", id);

        CommissionParameter parameter = commissionParameterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommissionParameter not found with id: " + id));

        return ResponseEntity.ok(parameterMapper.toDTO(parameter));
    }

    /**
     * Mettre à jour un paramètre de commission
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateCommissionParameter(
            @PathVariable Long id,
            @Valid @RequestBody CommissionParameterDTO parameterDTO) {

        log.info("Mise à jour paramètre commission - ID: {}", id);

        try {
            // Vérifier existence
            CommissionParameter existing = commissionParameterRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CommissionParameter not found with id: " + id));

            // Validation du DTO
            if (!parameterDTO.isValidScope()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Un seul scope doit être défini"));
            }

            // Forcer l'ID pour la mise à jour
            parameterDTO.setId(id);

            // Mapping et validation
            CommissionParameter parameter = parameterMapper.toEntity(parameterDTO);
            var validationResult = validationService.validateCommissionParameters(parameter);

            if (!validationResult.isValid()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Validation échouée",
                                "details", validationResult.getErrors()
                        ));
            }

            // Préserver certains champs de l'entité existante si nécessaire
            parameter.setVersion(existing.getVersion());

            // Sauvegarde
            CommissionParameter saved = commissionParameterRepository.save(parameter);
            CommissionParameterDTO result = parameterMapper.toDTO(saved);

            log.info("Paramètre mis à jour avec succès - ID: {}", id);
            return ResponseEntity.ok(result);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur mise à jour paramètre {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Supprimer un paramètre de commission
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteCommissionParameter(@PathVariable Long id) {
        log.info("Suppression paramètre commission - ID: {}", id);

        try {
            CommissionParameter parameter = commissionParameterRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CommissionParameter not found with id: " + id));

            // Vérifier si le paramètre est utilisé (optionnel - selon votre logique métier)
            // TODO: Ajouter vérification si des commissions utilisent ce paramètre

            commissionParameterRepository.delete(parameter);

            log.info("Paramètre supprimé avec succès - ID: {}", id);
            return ResponseEntity.noContent().build();

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur suppression paramètre {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Activer/Désactiver un paramètre
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> toggleParameterStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        log.info("Changement statut paramètre {} - Actif: {}", id, active);

        try {
            CommissionParameter parameter = commissionParameterRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CommissionParameter not found with id: " + id));

            parameter.setActive(active);
            CommissionParameter saved = commissionParameterRepository.save(parameter);

            return ResponseEntity.ok(parameterMapper.toDTO(saved));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur changement statut paramètre {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupérer paramètres par client
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('COLLECTEUR') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CommissionParameterDTO>> getParametersByClient(@PathVariable Long clientId) {
        log.debug("Récupération paramètres pour client: {}", clientId);

        // TODO: Implémenter la méthode dans le repository si nécessaire
        // List<CommissionParameter> parameters = commissionParameterRepository.findByClientId(clientId);
        // return ResponseEntity.ok(parameters.stream()
        //     .map(parameterMapper::toDTO)
        //     .toList());

        return ResponseEntity.ok().build(); // Placeholder
    }

    /**
     * Récupérer paramètres par collecteur
     */
    @GetMapping("/collecteur/{collecteurId}")
    @AgenceAccess
    public ResponseEntity<List<CommissionParameterDTO>> getParametersByCollecteur(@PathVariable Long collecteurId) {
        log.debug("Récupération paramètres pour collecteur: {}", collecteurId);

        // TODO: Implémenter selon vos besoins
        return ResponseEntity.ok().build(); // Placeholder
    }

    /**
     * Récupérer paramètres par agence
     */
    @GetMapping("/agence/{agenceId}")
    @AgenceAccess
    public ResponseEntity<List<CommissionParameterDTO>> getParametersByAgence(@PathVariable Long agenceId) {
        log.debug("Récupération paramètres pour agence: {}", agenceId);

        // TODO: Implémenter selon vos besoins
        return ResponseEntity.ok().build(); // Placeholder
    }

    /**
     * Ajouter un tier à un paramètre (existant - amélioré)
     */
    @PostMapping("/{id}/tiers")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> addTier(@PathVariable Long id, @Valid @RequestBody CommissionTier tier) {
        log.info("Ajout tier au paramètre {}", id);

        try {
            CommissionParameter parameter = commissionParameterRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CommissionParameter not found with id: " + id));

            // Validation du type
            if (parameter.getType() != CommissionType.TIER) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Les tiers ne peuvent être ajoutés qu'aux paramètres de type TIER"));
            }

            // Validation du tier
            if (tier.getMontantMin() >= tier.getMontantMax()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le montant minimum doit être inférieur au montant maximum"));
            }

            if (tier.getTaux() < 0 || tier.getTaux() > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le taux doit être compris entre 0 et 100"));
            }

            // Vérifier chevauchements avec tiers existants
            for (CommissionTier existingTier : parameter.getTiers()) {
                if (tiersOverlap(tier, existingTier)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    String.format("Chevauchement détecté avec tier [%.2f-%.2f]",
                                            existingTier.getMontantMin(), existingTier.getMontantMax())));
                }
            }

            // Associer à paramètre et sauvegarder
            tier.setCommissionParameter(parameter);
            parameter.getTiers().add(tier);
            CommissionParameter saved = commissionParameterRepository.save(parameter);

            log.info("Tier ajouté avec succès au paramètre {}", id);
            return ResponseEntity.ok(parameterMapper.toDTO(saved));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur ajout tier au paramètre {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Supprimer un tier d'un paramètre
     */
    @DeleteMapping("/{id}/tiers/{tierId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> removeTier(@PathVariable Long id, @PathVariable Long tierId) {
        log.info("Suppression tier {} du paramètre {}", tierId, id);

        try {
            CommissionParameter parameter = commissionParameterRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CommissionParameter not found with id: " + id));

            boolean removed = parameter.getTiers().removeIf(tier -> tier.getId().equals(tierId));

            if (!removed) {
                return ResponseEntity.notFound().build();
            }

            CommissionParameter saved = commissionParameterRepository.save(parameter);
            return ResponseEntity.ok(parameterMapper.toDTO(saved));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur suppression tier {}: {}", tierId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Méthodes utilitaires
    private String getScope(CommissionParameterDTO dto) {
        if (dto.getClientId() != null) return "CLIENT:" + dto.getClientId();
        if (dto.getCollecteurId() != null) return "COLLECTEUR:" + dto.getCollecteurId();
        if (dto.getAgenceId() != null) return "AGENCE:" + dto.getAgenceId();
        return "UNDEFINED";
    }

    private boolean tiersOverlap(CommissionTier tier1, CommissionTier tier2) {
        return (tier1.getMontantMin() < tier2.getMontantMax() &&
                tier1.getMontantMax() > tier2.getMontantMin());
    }
}