package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ClientDTO;
import org.example.collectfocep.dto.ClientDetailDTO;
import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.dto.CompteDTO;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.Compte;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.ClientMapper;
import org.example.collectfocep.mappers.CommissionParameterMapper;
import org.example.collectfocep.mappers.CompteMapper;
import org.example.collectfocep.mappers.MouvementMapperV2;
import org.example.collectfocep.repositories.CommissionParameterRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.services.interfaces.ClientService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clients")
@Slf4j
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final CompteService compteService;
    private final ClientMapper clientMapper;
    private final CompteMapper compteMapper;
    private final MouvementRepository mouvementRepository;
    private final MouvementMapperV2 mouvementMapper;
    private final CommissionParameterRepository commissionParameterRepository;
    private final CommissionParameterMapper commissionParameterMapper;

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

    @GetMapping("/{id}/with-transactions")
    @PreAuthorize("@securityService.canManageClient(authentication, #id)")
    public ResponseEntity<ApiResponse<ClientDetailDTO>> getClientWithTransactions(@PathVariable Long id) {
        log.info("🔍 Récupération du client avec transactions: {}", id);

        try {
            // 1. Récupérer le client
            Client client = clientService.getClientById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + id));

            // 2. Récupérer ses transactions
            List<Mouvement> transactions = mouvementRepository.findByClientIdWithAllRelations(id);

            // 3. Récupérer les paramètres de commission avec hiérarchie
            CommissionParameterDTO commissionParam = getEffectiveCommissionParameter(client);

            // 4. Calculer le solde total
            Double soldeTotal = calculateClientBalance(transactions);
            Double totalEpargne = calculateTotalEpargne(transactions);
            Double totalRetraits = calculateTotalRetraits(transactions);

            // 5. Créer le DTO complet
            ClientDetailDTO clientDetail = ClientDetailDTO.builder()
                    .id(client.getId())
                    .nom(client.getNom())
                    .prenom(client.getPrenom())
                    .numeroCni(client.getNumeroCni())
                    .ville(client.getVille())
                    .quartier(client.getQuartier())
                    .telephone(client.getTelephone())
                    .photoPath(client.getPhotoPath())
                    .numeroCompte(client.getNumeroCompte())
                    .valide(client.getValide())
                    .dateCreation(client.getDateCreation())
                    .dateModification(client.getDateModification())
                    .collecteurId(client.getCollecteur() != null ? client.getCollecteur().getId() : null)
                    .agenceId(client.getAgence() != null ? client.getAgence().getId() : null)
                    .transactions(transactions.stream()
                            .map(mouvementMapper::toDTO)
                            .collect(Collectors.toList()))
                    .totalTransactions(transactions.size())
                    .soldeTotal(soldeTotal)
                    .totalEpargne(totalEpargne)
                    .totalRetraits(totalRetraits)
                    .commissionParameter(commissionParam)
                    .build();

            return ResponseEntity.ok(
                    ApiResponse.success(clientDetail, "Client avec transactions récupéré avec succès")
            );
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération du client avec transactions {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CLIENT_DETAIL_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    // IMPLÉMENTATION DE LA HIÉRARCHIE DE COMMISSION
    private CommissionParameterDTO getEffectiveCommissionParameter(Client client) {
        // 1. Chercher au niveau client
        Optional<CommissionParameter> clientCommission =
                commissionParameterRepository.findActiveCommissionParameter(client.getId());

        if (clientCommission.isPresent()) {
            log.debug("Commission trouvée au niveau client: {}", client.getId());
            return commissionParameterMapper.toDTO(clientCommission.get());
        }

        // 2. Chercher au niveau collecteur
        Optional<CommissionParameter> collecteurCommission =
                commissionParameterRepository.findActiveCommissionParameterByCollecteur(
                        client.getCollecteur().getId());

        if (collecteurCommission.isPresent()) {
            log.debug("Commission trouvée au niveau collecteur: {}", client.getCollecteur().getId());
            return commissionParameterMapper.toDTO(collecteurCommission.get());
        }

        // 3. Chercher au niveau agence
        Optional<CommissionParameter> agenceCommission =
                commissionParameterRepository.findActiveCommissionParameterByAgence(
                        client.getAgence().getId());

        if (agenceCommission.isPresent()) {
            log.debug("Commission trouvée au niveau agence: {}", client.getAgence().getId());
            return commissionParameterMapper.toDTO(agenceCommission.get());
        }

        log.warn("Aucune commission trouvée pour le client: {}", client.getId());
        return null;
    }

    private Double calculateClientBalance(List<Mouvement> transactions) {
        return transactions.stream()
                .mapToDouble(t -> {
                    if ("epargne".equals(t.getSens()) || "EPARGNE".equals(t.getTypeMouvement())) {
                        return t.getMontant();
                    } else if ("retrait".equals(t.getSens()) || "RETRAIT".equals(t.getTypeMouvement())) {
                        return -t.getMontant();
                    }
                    return 0.0;
                })
                .sum();
    }

    private Double calculateTotalEpargne(List<Mouvement> transactions) {
        return transactions.stream()
                .filter(t -> "epargne".equals(t.getSens()) || "EPARGNE".equals(t.getTypeMouvement()))
                .mapToDouble(Mouvement::getMontant)
                .sum();
    }

    private Double calculateTotalRetraits(List<Mouvement> transactions) {
        return transactions.stream()
                .filter(t -> "retrait".equals(t.getSens()) || "RETRAIT".equals(t.getTypeMouvement()))
                .mapToDouble(Mouvement::getMontant)
                .sum();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ClientDTO>>> getAllClients() {
        log.info("Récupération de tous les clients (pour admin/super admin)");
        List<Client> clients = clientService.getAllClients();
        List<ClientDTO> dtos = clients.stream()
                .map(clientMapper::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos, "Liste des clients récupérée"));
    }
}