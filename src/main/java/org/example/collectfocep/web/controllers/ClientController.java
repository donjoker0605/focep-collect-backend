package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.aspects.LogActivity;
import org.example.collectfocep.dto.*;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.ClientMapper;
import org.example.collectfocep.mappers.CommissionParameterMapper;
import org.example.collectfocep.mappers.CompteMapper;
import org.example.collectfocep.mappers.MouvementMapperV2;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CommissionParameterRepository;
import org.example.collectfocep.repositories.CommissionTierRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.interfaces.ClientService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.services.GeolocationService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.env.Environment;
import java.util.Arrays;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    private final ClientRepository clientRepository;
    private final GeolocationService geolocationService;
    private final SecurityService securityService;
    private final Environment environment;
    private final CommissionTierRepository commissionTierRepository;

    // Endpoint pour créer un client
    @PostMapping
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #clientDTO.collecteurId)")
    @LogActivity(action = "CREATE_CLIENT", entityType = "CLIENT", description = "Création d'un nouveau client")
    public ResponseEntity<ApiResponse<ClientDTO>> createClient(
            @Valid @RequestBody ClientDTO clientDTO,
            Authentication authentication) {

        log.info("Création d'un nouveau client: {}", clientDTO.getNumeroCni());

        try {
            // 🔥 SOLUTION 1: EXTRACTION AUTOMATIQUE DES IDS DEPUIS LE JWT
            Long currentCollecteurId = securityService.getCurrentUserId();
            Long currentAgenceId = securityService.getCurrentUserAgenceId();

            if (currentCollecteurId == null || currentAgenceId == null) {
                log.error("❌ Impossible d'extraire collecteurId={} ou agenceId={} du token",
                        currentCollecteurId, currentAgenceId);

                securityService.debugAuthenticationInfo();
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("MISSING_AUTH_INFO", "Informations d'authentification incomplètes"));
            }

            // 🔥 FORCER LES IDS CORRECTS (ignorer ceux envoyés par le client pour sécurité)
            clientDTO.setCollecteurId(currentCollecteurId);
            clientDTO.setAgenceId(currentAgenceId);

            log.info("✅ IDs assignés automatiquement: collecteur={}, agence={}",
                    currentCollecteurId, currentAgenceId);

            // 🔥 SOLUTION 2: VALIDATION INTELLIGENTE DES COORDONNÉES
            validateAndProcessCoordinates(clientDTO);

            // Conversion DTO vers Entity
            Client client = clientMapper.toEntity(clientDTO);

            // 🔥 NOUVEAU : Sauvegarde avec création compte et commission
            Client savedClient = clientService.saveClient(client);

            // 🔥 NOUVEAU : Créer le paramètre de commission si spécifié
            if (clientDTO.hasCommissionParameter()) {
                createCommissionParameterForClient(savedClient, clientDTO.getCommissionParameter());
            }

            return ResponseEntity.ok(ApiResponse.success(
                    clientMapper.toDTO(savedClient),
                    "Client créé avec succès"
            ));

        } catch (BusinessException e) {
            log.error("❌ Erreur métier: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la création du client", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CLIENT_CREATE_ERROR", "Erreur interne: " + e.getMessage()));
        }
    }

    /**
     * 🔥 NOUVELLE MÉTHODE : Créer paramètre de commission pour un client
     */
    private void createCommissionParameterForClient(Client client, CommissionParameterDTO commissionDTO) {
        try {
            log.info("💰 Création paramètre commission pour client: {} {}",
                    client.getPrenom(), client.getNom());

            // Validation du type de commission
            if (commissionDTO.getType() == null) {
                log.warn("⚠️ Type de commission manquant, ignoré");
                return;
            }

            // Créer l'entité CommissionParameter
            CommissionParameter parameter = CommissionParameter.builder()
                    .client(client)
                    .type(commissionDTO.getType())
                    .valeur(commissionDTO.getValeur() != null ? commissionDTO.getValeur() : 0.0)
                    .active(commissionDTO.getActive() != null ? commissionDTO.getActive() : true)
                    .validFrom(commissionDTO.getValidFrom() != null ?
                            commissionDTO.getValidFrom() : LocalDate.now())
                    .validTo(commissionDTO.getValidTo())
                    .build();

            // Sauvegarder le paramètre
            CommissionParameter savedParameter = commissionParameterRepository.save(parameter);
            log.info("✅ Paramètre commission créé: ID={}, Type={}, Valeur={}",
                    savedParameter.getId(), savedParameter.getType(), savedParameter.getValeur());

            // 🔥 GESTION DES PALIERS POUR TYPE TIER
            if (commissionDTO.getType() == CommissionType.TIER &&
                    commissionDTO.getPaliersCommission() != null && !commissionDTO.getPaliersCommission().isEmpty()) {

                createCommissionTiers(savedParameter, commissionDTO.getPaliersCommission());
            }

        } catch (Exception e) {
            log.error("❌ Erreur création paramètre commission: {}", e.getMessage(), e);
            // Ne pas faire échouer la création du client pour autant
            log.warn("⚠️ Client créé mais sans paramètre de commission - À corriger manuellement");
        }
    }

    /**
     * Créer les paliers de commission
     */
    /**
     * 🔥 VERSION FINALE CORRIGÉE: Créer les paliers de commission
     */
    private void createCommissionTiers(CommissionParameter parameter, List<PalierCommissionDTO> tiersDTO) {
        try {
            log.info("📊 Création de {} paliers de commission", tiersDTO.size());

            for (int i = 0; i < tiersDTO.size(); i++) {
                PalierCommissionDTO tierDTO = tiersDTO.get(i);

                // Validation des champs obligatoires
                if (tierDTO.getMontantMin() == null || tierDTO.getTaux() == null) {
                    log.warn("⚠️ Palier {} invalide (montantMin ou taux null), ignoré", i + 1);
                    continue;
                }

                Double montantMin = tierDTO.getMontantMin();
                Double taux = tierDTO.getTaux();
                Double montantMax = tierDTO.getMontantMax(); // Peut être null pour "illimité"

                // Validation des valeurs numériques
                if (montantMin < 0 || taux < 0 || taux > 100) {
                    log.warn("⚠️ Valeurs palier {} invalides (montantMin={}, taux={}), ignoré",
                            i + 1, montantMin, taux);
                    continue;
                }

                // Si montantMax est défini, vérifier qu'il est supérieur à montantMin
                if (montantMax != null && montantMax <= montantMin) {
                    log.warn("⚠️ Palier {} invalide (montantMax {} <= montantMin {}), ignoré",
                            i + 1, montantMax, montantMin);
                    continue;
                }

                // Vérifier les chevauchements avec les paliers existants
                if (!validateNoOverlap(parameter.getId(), montantMin, montantMax)) {
                    log.warn("⚠️ Palier {} ignoré à cause d'un chevauchement", i + 1);
                    continue;
                }

                // Créer l'entité CommissionTier avec la nouvelle structure
                CommissionTier tier = CommissionTier.builder()
                        .commissionParameter(parameter)
                        .montantMin(montantMin)
                        .montantMax(montantMax) // null est autorisé maintenant
                        .taux(taux)
                        .build();

                // Validation finale de l'entité
                if (!tier.isValid()) {
                    log.warn("⚠️ Palier {} invalide après création, ignoré", i + 1);
                    continue;
                }

                CommissionTier savedTier = commissionTierRepository.save(tier);
                log.info("✅ Palier {} créé: {} = {}%",
                        i + 1,
                        savedTier.getRangeDescription(),
                        savedTier.getTaux());
            }

        } catch (Exception e) {
            log.error("❌ Erreur création paliers commission: {}", e.getMessage(), e);
        }
    }

    /**
     * Valider qu'il n'y a pas de chevauchement avec les paliers existants
     */
    private boolean validateNoOverlap(Long parameterId, Double montantMin, Double montantMax) {
        try {
            List<CommissionTier> existingTiers = commissionTierRepository
                    .findByCommissionParameterIdOrderByMontantMinAsc(parameterId);

            // Créer un palier temporaire pour tester les chevauchements
            CommissionTier newTier = CommissionTier.builder()
                    .montantMin(montantMin)
                    .montantMax(montantMax)
                    .build();

            for (CommissionTier existing : existingTiers) {
                if (newTier.overlapsWith(existing)) {
                    log.warn("⚠️ Chevauchement détecté avec palier existant: {}",
                            existing.getRangeDescription());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("❌ Erreur validation chevauchement: {}", e.getMessage());
            return true; // En cas d'erreur, on autorise pour ne pas bloquer
        }
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
    @LogActivity(action = "MODIFY_CLIENT", entityType = "CLIENT", description = "Modification d'un client")
    public ResponseEntity<ApiResponse<ClientDTO>> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientUpdateDTO clientUpdateDTO) {

        log.info("Mise à jour du client: {}", id);

        try {
            // Vérifier que le client existe et appartient au bon collecteur
            Client existingClient = clientService.getClientById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + id));

            Long currentCollecteurId = securityService.getCurrentUserId();
            if (!existingClient.getCollecteur().getId().equals(currentCollecteurId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("UNAUTHORIZED", "Vous ne pouvez modifier que vos propres clients"));
            }

            // 🔥 MISE À JOUR SÉLECTIVE (collecteur ne peut modifier que certains champs)
            updateAllowedFieldsOnly(existingClient, clientUpdateDTO);

            Client updatedClient = clientService.updateClient(existingClient);

            return ResponseEntity.ok(ApiResponse.success(
                    clientMapper.toDTO(updatedClient),
                    "Client mis à jour avec succès"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("UPDATE_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * MISE À JOUR SÉLECTIVE - Collecteur ne peut modifier que certains champs
     */
    private void updateAllowedFieldsOnly(Client existingClient, ClientUpdateDTO updateDTO) {
        // Champs que le COLLECTEUR peut modifier
        if (updateDTO.getTelephone() != null) {
            existingClient.setTelephone(updateDTO.getTelephone());
        }

        if (updateDTO.getNumeroCni() != null) {
            existingClient.setNumeroCni(updateDTO.getNumeroCni());
        }

        if (updateDTO.getQuartier() != null) {
            existingClient.setQuartier(updateDTO.getQuartier());
        }

        if (updateDTO.getVille() != null) {
            existingClient.setVille(updateDTO.getVille());
        }

        // GÉOLOCALISATION - Peut être mise à jour
        if (updateDTO.getLatitude() != null && updateDTO.getLongitude() != null) {
            existingClient.setLatitude(BigDecimal.valueOf(updateDTO.getLatitude()));
            existingClient.setLongitude(BigDecimal.valueOf(updateDTO.getLongitude()));
            existingClient.setCoordonneesSaisieManuelle(updateDTO.getCoordonneesSaisieManuelle());
            existingClient.setAdresseComplete(updateDTO.getAdresseComplete());
            existingClient.setDateMajCoordonnees(LocalDateTime.now());
        }

        // Champs que le collecteur NE PEUT PAS modifier :
        // - nom, prenom (seulement admin)
        // - agenceId, collecteurId (sécurité)
        // - numeroCompte (sécurité)
        // - dateCreation (immutable)

        log.info("✅ Mise à jour sélective effectuée pour client: {}", existingClient.getId());
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
                    // 🔥 NOUVEAU : Champs géolocalisation
                    .latitude(client.getLatitude() != null ? client.getLatitude().doubleValue() : null)
                    .longitude(client.getLongitude() != null ? client.getLongitude().doubleValue() : null)
                    .coordonneesSaisieManuelle(client.getCoordonneesSaisieManuelle())
                    .adresseComplete(client.getAdresseComplete())
                    .dateMajCoordonnees(client.getDateMajCoordonnees())
                    .sourceLocalisation(client.isManualLocation() ? "MANUAL" : "GPS")
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

    // 🔥 NOUVEAU : Endpoints pour la géolocalisation

    /**
     * Mettre à jour la localisation d'un client
     */
    @PutMapping("/{clientId}/location")
    @PreAuthorize("@securityService.canManageClient(authentication, #clientId)")
    @LogActivity(action = "UPDATE_CLIENT_LOCATION", entityType = "CLIENT", description = "Mise à jour localisation client")
    public ResponseEntity<ApiResponse<ClientLocationDTO>> updateClientLocation(
            @PathVariable Long clientId,
            @Valid @RequestBody LocationUpdateRequest request) {

        log.info("📍 Mise à jour localisation client: {}", clientId);

        try {
            ClientLocationDTO result = geolocationService.updateClientLocation(clientId, request);
            return ResponseEntity.ok(ApiResponse.success(result, "Localisation mise à jour avec succès"));
        } catch (Exception e) {
            log.error("❌ Erreur mise à jour localisation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("LOCATION_UPDATE_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * Obtenir la localisation d'un client
     */
    @GetMapping("/{clientId}/location")
    @PreAuthorize("@securityService.canManageClient(authentication, #clientId)")
    public ResponseEntity<ApiResponse<ClientLocationDTO>> getClientLocation(@PathVariable Long clientId) {
        log.info("📍 Récupération localisation client: {}", clientId);

        try {
            ClientLocationDTO location = geolocationService.getClientLocation(clientId);
            return ResponseEntity.ok(ApiResponse.success(location, "Localisation récupérée"));
        } catch (Exception e) {
            log.error("❌ Erreur récupération localisation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("LOCATION_GET_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * Obtenir les clients proches d'une position
     */
    @GetMapping("/location/nearby")
    @PreAuthorize("hasAnyRole('COLLECTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ClientLocationDTO>>> getNearbyClients(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusKm) {

        log.info("📍 Recherche clients proches: lat={}, lng={}, radius={}km", latitude, longitude, radiusKm);

        try {
            List<ClientLocationDTO> nearbyClients = geolocationService.getClientsProches(latitude, longitude, radiusKm);
            return ResponseEntity.ok(ApiResponse.success(nearbyClients,
                    String.format("Trouvé %d clients dans un rayon de %.1f km", nearbyClients.size(), radiusKm)));
        } catch (Exception e) {
            log.error("❌ Erreur recherche clients proches: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("NEARBY_CLIENTS_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * Valider des coordonnées GPS
     */
    @PostMapping("/location/validate")
    @PreAuthorize("hasAnyRole('COLLECTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> validateCoordinates(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {

        log.info("📍 Validation coordonnées: lat={}, lng={}", latitude, longitude);

        try {
            boolean isValid = geolocationService.validateCoordinates(latitude, longitude);
            String message = isValid ? "Coordonnées valides" : "Coordonnées invalides";
            return ResponseEntity.ok(ApiResponse.success(isValid, message));
        } catch (Exception e) {
            log.error("❌ Erreur validation coordonnées: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("COORDINATE_VALIDATION_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * Statistiques de géolocalisation
     */
    @GetMapping("/location/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLocationStatistics() {
        log.info("📊 Récupération statistiques géolocalisation");

        try {
            // Utiliser la méthode du service si elle existe
            if (clientService instanceof org.example.collectfocep.services.impl.ClientServiceImpl) {
                org.example.collectfocep.services.impl.ClientServiceImpl.LocationStatistics stats =
                        ((org.example.collectfocep.services.impl.ClientServiceImpl) clientService).getLocationStatistics();

                Map<String, Object> result = Map.of(
                        "totalClients", stats.getTotalClients(),
                        "clientsWithLocation", stats.getClientsWithLocation(),
                        "clientsWithoutLocation", stats.getClientsWithoutLocation(),
                        "manualEntries", stats.getManualEntries(),
                        "gpsEntries", stats.getGpsEntries(),
                        "coveragePercentage", stats.getCoveragePercentage()
                );

                return ResponseEntity.ok(ApiResponse.success(result, "Statistiques de géolocalisation"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("SERVICE_NOT_AVAILABLE", "Statistiques non disponibles"));
            }
        } catch (Exception e) {
            log.error("❌ Erreur récupération statistiques: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("STATISTICS_ERROR", "Erreur: " + e.getMessage()));
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

    /**
     *  Statistiques d'un client
     * Endpoint séparé pour les statistiques (même si with-transactions les contient déjà)
     */
    @GetMapping("/{id}/statistics")
    @PreAuthorize("@securityService.canManageClient(authentication, #id)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClientStatistics(@PathVariable Long id) {
        log.info("📊 Récupération des statistiques du client: {}", id);

        try {
            // 1. Récupérer le client
            Client client = clientService.getClientById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + id));

            // 2. Récupérer ses transactions
            List<Mouvement> transactions = mouvementRepository.findByClientIdWithAllRelations(id);

            // 3. Calculer les statistiques
            Double totalEpargne = calculateTotalEpargne(transactions);
            Double totalRetraits = calculateTotalRetraits(transactions);
            Double soldeTotal = calculateClientBalance(transactions);

            // Statistiques par période
            LocalDate aujourdhui = LocalDate.now();
            LocalDate debutMois = aujourdhui.withDayOfMonth(1);
            LocalDate debutSemaine = aujourdhui.with(DayOfWeek.MONDAY);

            Double epargneCurrentMois = calculateEpargnePeriod(transactions, debutMois, aujourdhui);
            Double epargneCurrentSemaine = calculateEpargnePeriod(transactions, debutSemaine, aujourdhui);

            // Dernière transaction
            Optional<Mouvement> derniereTransaction = transactions.stream()
                    .max(Comparator.comparing(Mouvement::getDateOperation));

            // 4. Construire la réponse avec HashMap pour éviter la limite de Map.of()
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalEpargne", totalEpargne);
            stats.put("totalRetraits", totalRetraits);
            stats.put("soldeTotal", soldeTotal);
            stats.put("nombreTransactions", transactions.size());
            stats.put("epargneCurrentMois", epargneCurrentMois);
            stats.put("epargneCurrentSemaine", epargneCurrentSemaine);
            stats.put("derniereTransaction", derniereTransaction.map(t -> Map.of(
                    "date", t.getDateOperation(),
                    "montant", t.getMontant(),
                    "type", t.getSens()
            )).orElse(null));
            stats.put("moyenneEpargneParTransaction", transactions.size() > 0 ? totalEpargne / transactions.size() : 0);
            // Informations de localisation
            stats.put("hasLocation", client.hasLocation());
            stats.put("locationSummary", client.hasLocation() ? client.getLocationSummary() : null);
            stats.put("locationLastUpdate", client.getDateMajCoordonnees());

            ApiResponse<Map<String, Object>> response = ApiResponse.success(stats);
            response.addMeta("clientId", id);
            response.addMeta("dateCalcul", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors du calcul des statistiques du client {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du calcul des statistiques"));
        }
    }

    /**
     *  Solde d'un client
     * Endpoint séparé pour le solde
     */
    @GetMapping("/{id}/balance")
    @PreAuthorize("@securityService.canManageClient(authentication, #id)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClientBalance(@PathVariable Long id) {
        log.info("💰 Récupération du solde du client: {}", id);

        try {
            // 1. Vérifier que le client existe
            Client client = clientService.getClientById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + id));

            // 2. Récupérer ses transactions
            List<Mouvement> transactions = mouvementRepository.findByClientIdWithAllRelations(id);

            // 3. Calculer le solde
            Double totalEpargne = calculateTotalEpargne(transactions);
            Double totalRetraits = calculateTotalRetraits(transactions);
            Double soldeTotal = calculateClientBalance(transactions);

            // 4. Solde par période
            LocalDate aujourdhui = LocalDate.now();
            LocalDate debutMois = aujourdhui.withDayOfMonth(1);

            Double soldePrecedent = calculateSoldeAtDate(transactions, debutMois.minusDays(1));
            Double evolutionMois = soldeTotal - soldePrecedent;

            Map<String, Object> balance = Map.of(
                    "soldeTotal", soldeTotal,
                    "totalEpargne", totalEpargne,
                    "totalRetraits", totalRetraits,
                    "soldePrecedent", soldePrecedent,
                    "evolutionMois", evolutionMois,
                    "lastUpdated", LocalDateTime.now(),
                    "clientNom", client.getPrenom() + " " + client.getNom()
            );

            ApiResponse<Map<String, Object>> response = ApiResponse.success(balance);
            response.addMeta("clientId", id);
            response.addMeta("currency", "FCFA");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors du calcul du solde du client {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors du calcul du solde"));
        }
    }

    /**
     * 🔥 ENDPOINT 3: Transactions d'un client avec pagination
     * Endpoint séparé pour les transactions avec filtres avancés
     */
    @GetMapping("/{id}/transactions")
    @PreAuthorize("@securityService.canManageClient(authentication, #id)")
    public ResponseEntity<ApiResponse<Page<MouvementDTO>>> getClientTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateOperation") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String type, // EPARGNE ou RETRAIT
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {

        log.info("📋 Récupération des transactions du client: {} (page={}, size={})", id, page, size);

        try {
            // 1. Vérifier que le client existe
            clientService.getClientById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + id));

            // 2. Configuration pagination
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

            // 3. Récupérer les transactions avec filtres
            Page<Mouvement> transactionsPage;

            if (type != null || dateDebut != null || dateFin != null) {
                // Utiliser la recherche avec filtres (méthode à ajouter au repository)
                transactionsPage = mouvementRepository.findByClientIdWithFilters(
                        id, type, dateDebut, dateFin, pageRequest);
            } else {
                // Récupération simple (méthode à ajouter au repository)
                transactionsPage = mouvementRepository.findByClientId(id, pageRequest);
            }

            // 4. Mapper vers DTO
            Page<MouvementDTO> dtoPage = transactionsPage.map(mouvementMapper::toDTO);

            ApiResponse<Page<MouvementDTO>> response = ApiResponse.success(dtoPage);
            response.addMeta("clientId", id);
            response.addMeta("filtres", Map.of(
                    "type", type != null ? type : "tous",
                    "dateDebut", dateDebut != null ? dateDebut.toString() : "aucune",
                    "dateFin", dateFin != null ? dateFin.toString() : "aucune"
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des transactions du client {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la récupération des transactions"));
        }
    }

// ============================================
// 🔥 MÉTHODES UTILITAIRES PRIVÉES À AJOUTER
// ============================================

    /**
     * Calculer l'épargne sur une période donnée
     */
    private Double calculateEpargnePeriod(List<Mouvement> transactions, LocalDate debut, LocalDate fin) {
        return transactions.stream()
                .filter(t -> "epargne".equals(t.getSens()) || "EPARGNE".equals(t.getTypeMouvement()))
                .filter(t -> {
                    LocalDate dateOp = t.getDateOperation().toLocalDate();
                    return !dateOp.isBefore(debut) && !dateOp.isAfter(fin);
                })
                .mapToDouble(Mouvement::getMontant)
                .sum();
    }

    /**
     * Calculer le solde à une date précise
     */
    private Double calculateSoldeAtDate(List<Mouvement> transactions, LocalDate date) {
        return transactions.stream()
                .filter(t -> !t.getDateOperation().toLocalDate().isAfter(date))
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

    /**
     * Recherche client optimisée avec debounce côté frontend
     */
    @GetMapping("/collecteur/{collecteurId}/search")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<List<ClientSearchDTO>>> searchClientsOptimized(
            @PathVariable Long collecteurId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("🔍 Recherche clients optimisée: collecteur={}, query='{}', limit={}",
                collecteurId, query, limit);

        try {
            if (query.trim().length() < 2) {
                return ResponseEntity.ok(ApiResponse.success(
                        Collections.emptyList(),
                        "Requête trop courte"
                ));
            }

            // Utilisation de la méthode optimisée du repository
            PageRequest pageRequest = PageRequest.of(0, limit);
            Page<Client> clientsPage = clientRepository.findByCollecteurIdAndSearch(
                    collecteurId, query.trim(), pageRequest);

            List<ClientSearchDTO> searchResults = clientsPage.getContent().stream()
                    .map(client -> ClientSearchDTO.builder()
                            .id(client.getId())
                            .nom(client.getNom())
                            .prenom(client.getPrenom())
                            .numeroCompte(client.getNumeroCompte())
                            .numeroCni(client.getNumeroCni())
                            .telephone(client.getTelephone())
                            .displayName(String.format("%s %s", client.getPrenom(), client.getNom()))
                            .hasPhone(client.getTelephone() != null && !client.getTelephone().trim().isEmpty())
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(
                    searchResults,
                    String.format("Trouvé %d client(s)", searchResults.size())
            ));

        } catch (Exception e) {
            log.error("❌ Erreur recherche clients: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SEARCH_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * Recherche par numéro de compte
     */
    @GetMapping("/collecteur/{collecteurId}/search-by-account")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<ClientSearchDTO>> searchByAccountNumber(
            @PathVariable Long collecteurId,
            @RequestParam String accountNumber) {

        log.info("🔍 Recherche par numéro compte: collecteur={}, compte='{}'",
                collecteurId, accountNumber);

        try {
            Optional<Client> clientOpt = clientRepository.findByCollecteurIdAndNumeroCompte(
                    collecteurId, accountNumber.trim());

            if (clientOpt.isPresent()) {
                Client client = clientOpt.get();
                ClientSearchDTO result = ClientSearchDTO.builder()
                        .id(client.getId())
                        .nom(client.getNom())
                        .prenom(client.getPrenom())
                        .numeroCompte(client.getNumeroCompte())
                        .numeroCni(client.getNumeroCni())
                        .telephone(client.getTelephone())
                        .displayName(String.format("%s %s", client.getPrenom(), client.getNom()))
                        .hasPhone(client.getTelephone() != null && !client.getTelephone().trim().isEmpty())
                        .build();

                return ResponseEntity.ok(ApiResponse.success(result, "Client trouvé"));
            } else {
                return ResponseEntity.ok(ApiResponse.success(null, "Aucun client trouvé"));
            }

        } catch (Exception e) {
            log.error("❌ Erreur recherche par compte: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SEARCH_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     *  Recherche unifiée (nom + numéro de compte)
     */
    @GetMapping("/collecteur/{collecteurId}/search-unified")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<List<ClientSearchDTO>>> searchClientsUnified(
            @PathVariable Long collecteurId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("🔍 Recherche unifiée: collecteur={}, query='{}', limit={}",
                collecteurId, query, limit);

        try {
            if (query.trim().length() < 2) {
                return ResponseEntity.ok(ApiResponse.success(
                        Collections.emptyList(),
                        "Requête trop courte"
                ));
            }

            // Utiliser la nouvelle méthode optimisée
            PageRequest pageRequest = PageRequest.of(0, limit);
            Page<Client> clientsPage = clientRepository.findByCollecteurIdAndSearchOptimized(
                    collecteurId, query.trim(), pageRequest);

            List<ClientSearchDTO> searchResults = clientsPage.getContent().stream()
                    .map(this::mapToClientSearchDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(
                    searchResults,
                    String.format("Trouvé %d client(s)", searchResults.size())
            ));

        } catch (Exception e) {
            log.error("❌ Erreur recherche unifiée: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("UNIFIED_SEARCH_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     *  Recherche spécifique par numéro de compte
     */
    @GetMapping("/collecteur/{collecteurId}/by-account/{accountNumber}")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<ClientSearchDTO>> findClientByAccountNumber(
            @PathVariable Long collecteurId,
            @PathVariable String accountNumber) {

        log.info("🔍 Recherche par numéro compte exact: collecteur={}, compte='{}'",
                collecteurId, accountNumber);

        try {
            Optional<Client> clientOpt = clientRepository.findByNumeroCompteAndCollecteurId(
                    accountNumber.trim(), collecteurId);

            if (clientOpt.isPresent()) {
                ClientSearchDTO result = mapToClientSearchDTO(clientOpt.get());
                return ResponseEntity.ok(ApiResponse.success(result, "Client trouvé"));
            } else {
                return ResponseEntity.ok(ApiResponse.success(null, "Aucun client trouvé"));
            }

        } catch (Exception e) {
            log.error("❌ Erreur recherche par compte: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("ACCOUNT_SEARCH_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * Suggestions numéros de compte (pour autocomplete)
     */
    @GetMapping("/collecteur/{collecteurId}/accounts/suggest")
    @PreAuthorize("@securityService.canManageCollecteur(authentication, #collecteurId)")
    public ResponseEntity<ApiResponse<List<String>>> suggestAccountNumbers(
            @PathVariable Long collecteurId,
            @RequestParam String partial,
            @RequestParam(defaultValue = "5") int limit) {

        log.info("🔍 Suggestions numéros compte: collecteur={}, partial='{}'",
                collecteurId, partial);

        try {
            if (partial.trim().length() < 2) {
                return ResponseEntity.ok(ApiResponse.success(
                        Collections.emptyList(),
                        "Requête trop courte"
                ));
            }

            PageRequest pageRequest = PageRequest.of(0, limit);
            List<Client> clients = clientRepository.findByPartialNumeroCompteAndCollecteurId(
                    partial.trim(), collecteurId, pageRequest);

            List<String> suggestions = clients.stream()
                    .map(Client::getNumeroCompte)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(
                    suggestions,
                    String.format("Trouvé %d suggestion(s)", suggestions.size())
            ));

        } catch (Exception e) {
            log.error("❌ Erreur suggestions comptes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("ACCOUNT_SUGGEST_ERROR", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * Validation numéro de compte + téléphone
     */
    @PostMapping("/validate-client-data")
    @PreAuthorize("hasAnyRole('COLLECTEUR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ClientValidationDTO>> validateClientData(
            @Valid @RequestBody ClientValidationRequest request) {

        log.info("📋 Validation données client: collecteur={}, compte={}",
                request.getCollecteurId(), request.getAccountNumber());

        try {
            ClientValidationDTO validation = new ClientValidationDTO();

            // 1. Rechercher le client
            Optional<Client> clientOpt = clientRepository.findByNumeroCompteAndCollecteurId(
                    request.getAccountNumber(), request.getCollecteurId());

            if (clientOpt.isPresent()) {
                Client client = clientOpt.get();
                validation.setClientFound(true);
                validation.setClientId(client.getId());
                validation.setClientName(String.format("%s %s", client.getPrenom(), client.getNom()));
                validation.setAccountNumber(client.getNumeroCompte());

                // 2. Vérifier le téléphone
                boolean hasPhone = client.getTelephone() != null &&
                        !client.getTelephone().trim().isEmpty();
                validation.setHasValidPhone(hasPhone);

                if (!hasPhone) {
                    validation.setPhoneWarning("Ce client n'a pas de numéro de téléphone renseigné");
                }

            } else {
                validation.setClientFound(false);
                validation.setErrorMessage("Aucun client trouvé avec ce numéro de compte");
            }

            return ResponseEntity.ok(ApiResponse.success(validation, "Validation effectuée"));

        } catch (Exception e) {
            log.error("❌ Erreur validation client: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALIDATION_ERROR", "Erreur: " + e.getMessage()));
        }
    }

// ========================================
// 🔧 MÉTHODES UTILITAIRES PRIVÉES
// ========================================

    /**
     * Mapper Client vers ClientSearchDTO
     */
    private ClientSearchDTO mapToClientSearchDTO(Client client) {
        return ClientSearchDTO.builder()
                .id(client.getId())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .numeroCompte(client.getNumeroCompte())
                .numeroCni(client.getNumeroCni())
                .telephone(client.getTelephone())
                .displayName(String.format("%s %s", client.getPrenom(), client.getNom()))
                .hasPhone(client.getTelephone() != null && !client.getTelephone().trim().isEmpty())
                .build();
    }

    /**
     * VALIDATION INTELLIGENTE DES COORDONNÉES
     */
    private void validateAndProcessCoordinates(ClientDTO clientDTO) {
        if (clientDTO.getLatitude() != null && clientDTO.getLongitude() != null) {

            double lat = clientDTO.getLatitude();
            double lng = clientDTO.getLongitude();

            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                throw new BusinessException("Coordonnées GPS invalides", "INVALID_COORDINATES");
            }

            // DÉTECTION ÉMULATEUR/COORDONNÉES FICTIVES
            if (Math.abs(lat - 37.4219983) < 0.001 && Math.abs(lng - (-122.084)) < 0.001) {
                log.warn("🚨 Coordonnées émulateur détectées (Mountain View, CA)");

                if (!isDevMode()) {
                    throw new BusinessException(
                            "Coordonnées émulateur détectées. Utilisez un appareil physique ou saisissez des coordonnées réelles.",
                            "EMULATOR_COORDINATES"
                    );
                } else {
                    log.info("🔧 Mode développement: coordonnées émulateur acceptées");
                }
            }

            // Vérification spécifique Cameroun (avec tolérance)
            if (!isInCameroonBounds(lat, lng)) {
                log.warn("⚠️ Coordonnées {} {} semblent être en dehors du Cameroun", lat, lng);
            }

            log.info("📍 Coordonnées validées: lat={}, lng={}, manuel={}",
                    lat, lng, clientDTO.getCoordonneesSaisieManuelle());
        } else {
            log.warn("⚠️ Client créé sans coordonnées GPS");
        }
    }

    /**
     * Vérifier si les coordonnées sont dans les limites du Cameroun
     */
    private boolean isInCameroonBounds(double lat, double lng) {
        return lat >= 1.0 && lat <= 13.5 && lng >= 8.0 && lng <= 16.5;
    }

    /**
     * Vérifier si on est en mode développement
     */

    private boolean isDevMode() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
                Boolean.parseBoolean(environment.getProperty("app.development.mode", "false"));
    }

    @GetMapping("/debug/auth-info")
    @PreAuthorize("hasRole('COLLECTEUR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugAuthInfo() {
        try {
            securityService.debugAuthenticationInfo();

            Map<String, Object> info = new HashMap<>();
            info.put("userId", securityService.getCurrentUserId());
            info.put("agenceId", securityService.getCurrentUserAgenceId());
            info.put("role", securityService.getCurrentUserRole());
            info.put("userInfo", securityService.getCurrentUserInfo());

            return ResponseEntity.ok(ApiResponse.success(info, "Informations de debug"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("DEBUG_ERROR", e.getMessage()));
        }
    }
}