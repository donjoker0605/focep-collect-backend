package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.AuditLogRequest;
import org.example.collectfocep.dto.ClientLocationDTO;
import org.example.collectfocep.dto.LocationUpdateRequest;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.services.impl.AuditService;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GeolocationService {

    private final ClientRepository clientRepository;
    private final AuditService auditService;

    public ClientLocationDTO updateClientLocation(Long clientId, LocationUpdateRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

        // Sauvegarder l'ancienne localisation pour l'audit
        String oldLocation = String.format("lat: %s, lng: %s",
                client.getLatitude(), client.getLongitude());

        // Mettre à jour la localisation
        client.setLatitude(request.getLatitude());
        client.setLongitude(request.getLongitude());
        client.setCoordonneesSaisieManuelle(request.getSaisieManuelle());
        client.setAdresseComplete(request.getAdresseComplete());
        client.setDateMajCoordonnees(LocalDateTime.now());

        Client savedClient = clientRepository.save(client);

        // Enregistrer l'activité
        String details = String.format(
                "{\"oldLocation\": \"%s\", \"newLocation\": \"lat: %s, lng: %s\", \"manual\": %s}",
                oldLocation, request.getLatitude(), request.getLongitude(), request.getSaisieManuelle()
        );

        auditService.logActivity(AuditLogRequest.builder()
                .userId(getCurrentUserId())
                .userType(getCurrentUserType())
                .action("UPDATE_CLIENT_LOCATION")
                .entityType("CLIENT")
                .entityId(clientId)
                .details(details)
                .agenceId(client.getAgence().getId())
                .build()
        );

        return toLocationDTO(savedClient);
    }

    public ClientLocationDTO getClientLocation(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

        return toLocationDTO(client);
    }

    @Query("SELECT c FROM Client c WHERE " +
            "SQRT(POWER(c.latitude - :latitude, 2) + POWER(c.longitude - :longitude, 2)) * 111 <= :radiusKm " +
            "ORDER BY SQRT(POWER(c.latitude - :latitude, 2) + POWER(c.longitude - :longitude, 2))")
    public List<ClientLocationDTO> getClientsProches(Double latitude, Double longitude, Double radiusKm) {
        // Note: Cette requête devrait être dans le repository
        return clientRepository.findClientsInRadius(latitude, longitude, radiusKm)
                .stream()
                .map(this::toLocationDTO)
                .collect(Collectors.toList());
    }

    private ClientLocationDTO toLocationDTO(Client client) {
        return ClientLocationDTO.builder()
                .clientId(client.getId())
                .nomComplet(client.getNom() + " " + client.getPrenom())
                .latitude(client.getLatitude())
                .longitude(client.getLongitude())
                .coordonneesSaisieManuelle(client.getCoordonneesSaisieManuelle())
                .adresseComplete(client.getAdresseComplete())
                .dateMajCoordonnees(client.getDateMajCoordonnees())
                .build();
    }

    // Ces méthodes devraient récupérer l'utilisateur actuel depuis le SecurityContext
    private Long getCurrentUserId() {
        // return SecurityContextHolder.getContext()...
        return 1L; // Placeholder
    }

    private String getCurrentUserType() {
        // return SecurityContextHolder.getContext()...
        return "COLLECTEUR"; // Placeholder
    }
}