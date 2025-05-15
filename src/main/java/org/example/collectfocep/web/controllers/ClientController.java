package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ClientDTO;
import org.example.collectfocep.dto.CompteDTO;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Compte;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.ClientMapper;
import org.example.collectfocep.mappers.CompteMapper;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.services.interfaces.ClientService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Slf4j
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final CompteService compteService;
    private final ClientMapper clientMapper;
    private final CompteMapper compteMapper;

    // Endpoint pour créer un client
    @PostMapping
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #clientDTO.collecteurId)")
    @Audited(action = "CREATE", entityType = "Client")
    public ResponseEntity<ApiResponse<ClientDTO>> createClient(@Valid @RequestBody ClientDTO clientDTO) {
        log.info("Création d'un nouveau client: {}", clientDTO.getNumeroCni());

        // Log pour debugging
        log.debug("DTO reçu - collecteurId: {}, agenceId: {}",
                clientDTO.getCollecteurId(), clientDTO.getAgenceId());

        // Conversion DTO vers Entity
        Client client = clientMapper.toEntity(clientDTO);

        // Log pour vérifier le mapping
        log.debug("Entity mappée - collecteur: {}, agence: {}",
                client.getCollecteur() != null ? client.getCollecteur().getId() : "null",
                client.getAgence() != null ? client.getAgence().getId() : "null");

        // Sauvegarde
        Client savedClient = clientService.saveClient(client);

        return ResponseEntity.ok(ApiResponse.success(
                clientMapper.toDTO(savedClient),
                "Client créé avec succès"
        ));
    }

    // Endpoint pour récupérer les clients d'un collecteur
    @GetMapping("/collecteur/{collecteurId}")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<List<ClientDTO>> getClientsByCollecteur(@PathVariable Long collecteurId) {
        log.info("Récupération des clients pour le collecteur: {}", collecteurId);
        List<Client> clients = clientService.findByCollecteurId(collecteurId);
        List<ClientDTO> dtos = clients.stream()
                .map(clientMapper::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // Endpoint pour récupérer un client par ID
    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canManageClient(authentication, #id)")
    public ResponseEntity<ApiResponse<ClientDTO>> getClientById(@PathVariable Long id) {
        log.info("Récupération du client avec l'ID: {}", id);
        Client client = clientService.getClientById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + id));

        return ResponseEntity.ok(ApiResponse.success(
                clientMapper.toDTO(client),
                "Client récupéré avec succès"
        ));
    }

    // Endpoint pour mettre à jour un client
    @PutMapping("/{id}")
    @PreAuthorize("@securityService.canManageClient(authentication, #id)")
    @Audited(action = "UPDATE", entityType = "Client")
    public ResponseEntity<ApiResponse<ClientDTO>> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientDTO clientDTO) {
        log.info("Mise à jour du client: {}", id);

        Client client = clientService.getClientById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + id));

        clientMapper.updateEntityFromDTO(clientDTO, client);
        Client updatedClient = clientService.updateClient(client);

        return ResponseEntity.ok(ApiResponse.success(
                clientMapper.toDTO(updatedClient),
                "Client mis à jour avec succès"
        ));
    }

    // Endpoint pour supprimer un client
    @DeleteMapping("/{id}")
    @PreAuthorize("@securityService.canManageClient(authentication, #id)")
    @Audited(action = "DELETE", entityType = "Client")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable Long id) {
        log.info("Suppression du client: {}", id);
        clientService.deleteClient(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Client supprimé avec succès"));
    }

    // Endpoint avec pagination
    @GetMapping("/collecteur/{collecteurId}/page")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<Page<ClientDTO>>> getClientsByCollecteurPaginated(
            @PathVariable Long collecteurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Récupération paginée des clients pour le collecteur: {}", collecteurId);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Client> clientsPage = clientService.findByCollecteurId(collecteurId, pageRequest);
        Page<ClientDTO> dtoPage = clientsPage.map(clientMapper::toDTO);

        ApiResponse<Page<ClientDTO>> response = ApiResponse.success(dtoPage);
        response.addMeta("totalElements", clientsPage.getTotalElements());
        response.addMeta("totalPages", clientsPage.getTotalPages());

        return ResponseEntity.ok(response);
    }
}