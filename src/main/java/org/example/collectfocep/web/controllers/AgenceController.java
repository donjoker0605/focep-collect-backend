package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.AgenceDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.util.ApiResponse;
import org.example.collectfocep.mappers.AgenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la gestion des agences
 * Accessible uniquement aux super administrateurs
 */
@RestController
@RequestMapping("/api/agences")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')") // Toutes les méthodes nécessitent le rôle SUPER_ADMIN
public class AgenceController {

    private final AgenceRepository agenceRepository;
    private final AgenceMapper agenceMapper; // Vous devrez créer ce mapper

    /**
     * Récupère toutes les agences
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AgenceDTO>>> getAllAgences() {
        log.info("Récupération de toutes les agences");

        List<Agence> agences = agenceRepository.findAll();
        List<AgenceDTO> agenceDTOs = agences.stream()
                .map(agenceMapper::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(agenceDTOs, "Agences récupérées avec succès")
        );
    }

    /**
     * Récupère les agences avec pagination
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<AgenceDTO>>> getAgencesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nomAgence") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Récupération paginée des agences - page: {}, size: {}", page, size);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Agence> agencesPage = agenceRepository.findAll(pageRequest);
        Page<AgenceDTO> agenceDTOPage = agencesPage.map(agenceMapper::toDTO);

        ApiResponse<Page<AgenceDTO>> response = ApiResponse.success(agenceDTOPage);
        response.addMeta("totalElements", agencesPage.getTotalElements());
        response.addMeta("totalPages", agencesPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Récupère une agence par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgenceDTO>> getAgenceById(@PathVariable Long id) {
        log.info("Récupération de l'agence avec l'ID: {}", id);

        Agence agence = agenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée avec l'ID: " + id));

        AgenceDTO agenceDTO = agenceMapper.toDTO(agence);

        return ResponseEntity.ok(
                ApiResponse.success(agenceDTO, "Agence récupérée avec succès")
        );
    }

    /**
     * Crée une nouvelle agence
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AgenceDTO>> createAgence(@Valid @RequestBody AgenceDTO agenceDTO) {
        log.info("Création d'une nouvelle agence: {}", agenceDTO.getNomAgence());

        // Vérifier que le code agence n'existe pas déjà (si fourni)
        if (agenceDTO.getCodeAgence() != null &&
                agenceRepository.findAll().stream()
                        .anyMatch(agence -> agence.getCodeAgence() != null && agence.getCodeAgence().equals(agenceDTO.getCodeAgence()))) {
            throw new DuplicateResourceException("Une agence avec ce code existe déjà");
        }

        Agence agence = agenceMapper.toEntity(agenceDTO);

        // Générer un code agence si non fourni
        if (agence.getCodeAgence() == null || agence.getCodeAgence().isEmpty()) {
            agence.setCodeAgence(generateUniqueAgenceCode(agence.getNomAgence()));
        }

        Agence savedAgence = agenceRepository.save(agence);
        AgenceDTO savedAgenceDTO = agenceMapper.toDTO(savedAgence);

        log.info("Agence créée avec succès: {} - {}", savedAgence.getCodeAgence(), savedAgence.getNomAgence());

        return ResponseEntity.ok(
                ApiResponse.success(savedAgenceDTO, "Agence créée avec succès")
        );
    }

    /**
     * Met à jour une agence existante
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AgenceDTO>> updateAgence(
            @PathVariable Long id,
            @Valid @RequestBody AgenceDTO agenceDTO) {

        log.info("Mise à jour de l'agence avec l'ID: {}", id);

        Agence agence = agenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée avec l'ID: " + id));

        // Mettre à jour les champs
        agence.setNomAgence(agenceDTO.getNomAgence());

        // Mettre à jour le code agence seulement s'il est fourni et différent
        if (agenceDTO.getCodeAgence() != null &&
                !agenceDTO.getCodeAgence().equals(agence.getCodeAgence())) {

            // Vérifier l'unicité du nouveau code
            if (agenceRepository.findAll().stream()
                    .anyMatch(a -> !a.getId().equals(id) && a.getCodeAgence().equals(agenceDTO.getCodeAgence()))) {
                throw new DuplicateResourceException("Une agence avec ce code existe déjà");
            }

            agence.setCodeAgence(agenceDTO.getCodeAgence());
        }

        Agence updatedAgence = agenceRepository.save(agence);
        AgenceDTO updatedAgenceDTO = agenceMapper.toDTO(updatedAgence);

        log.info("Agence mise à jour avec succès: {}", updatedAgence.getId());

        return ResponseEntity.ok(
                ApiResponse.success(updatedAgenceDTO, "Agence mise à jour avec succès")
        );
    }

    /**
     * Supprime une agence
     * Note: Seulement si elle n'a pas de collecteurs ou clients associés
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAgence(@PathVariable Long id) {
        log.info("Suppression de l'agence avec l'ID: {}", id);

        Agence agence = agenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agence non trouvée avec l'ID: " + id));

        // Vérifier qu'elle n'a pas de collecteurs ou clients associés
        if ((agence.getCollecteurs() != null && !agence.getCollecteurs().isEmpty()) ||
                (agence.getClients() != null && !agence.getClients().isEmpty())) {
            throw new IllegalStateException("Impossible de supprimer une agence ayant des collecteurs ou clients associés");
        }

        agenceRepository.delete(agence);

        log.info("Agence supprimée avec succès: {}", id);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Agence supprimée avec succès")
        );
    }

    /**
     * Génère un code agence unique
     */
    private String generateUniqueAgenceCode(String nomAgence) {
        // Nettoyer le nom et extraire le préfixe
        String cleanedName = nomAgence.replaceAll("[^A-Za-z]", "");
        String prefix = cleanedName.substring(0, Math.min(3, cleanedName.length())).toUpperCase();

        // Si le préfixe est vide, utiliser un préfixe par défaut
        if (prefix.isEmpty()) {
            prefix = "AGE";
        }

        // Trouver un suffixe unique
        String code;
        int counter = 1;
        boolean codeExists;

        do {
            code = prefix + String.format("%03d", counter);
            // Utiliser une variable locale finale pour la lambda
            final String currentCode = code;
            codeExists = agenceRepository.findAll().stream()
                    .anyMatch(agence -> agence.getCodeAgence() != null &&
                            agence.getCodeAgence().equals(currentCode));
            counter++;
        } while (codeExists);

        return code;
    }
}