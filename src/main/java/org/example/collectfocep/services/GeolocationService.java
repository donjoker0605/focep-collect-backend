package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.AuditLogRequest;
import org.example.collectfocep.dto.ClientLocationDTO;
import org.example.collectfocep.dto.LocationUpdateRequest;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.services.impl.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // Constantes pour la validation des coordonnées
    private static final double CAMEROON_MIN_LAT = 1.5;
    private static final double CAMEROON_MAX_LAT = 13.0;
    private static final double CAMEROON_MIN_LNG = 8.0;
    private static final double CAMEROON_MAX_LNG = 16.5;
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Mettre à jour la localisation d'un client avec validation robuste
     */
    public ClientLocationDTO updateClientLocation(Long clientId, LocationUpdateRequest request) {
        log.info("📍 Mise à jour localisation client: {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

        // Validation des coordonnées AVANT sauvegarde
        validateCoordinatesStrict(request.getLatitude().doubleValue(), request.getLongitude().doubleValue());

        // Sauvegarder l'ancienne localisation pour l'audit
        String oldLocation = client.getLatitude() != null && client.getLongitude() != null
                ? String.format("lat: %s, lng: %s", client.getLatitude(), client.getLongitude())
                : "Aucune localisation";

        // Mettre à jour la localisation
        client.setLatitude(request.getLatitude());
        client.setLongitude(request.getLongitude());
        client.setCoordonneesSaisieManuelle(request.getSaisieManuelle());
        client.setAdresseComplete(request.getAdresseComplete());
        client.setDateMajCoordonnees(LocalDateTime.now());

        Client savedClient = clientRepository.save(client);
        log.info("✅ Localisation mise à jour: {}", savedClient.getId());

        // Enregistrer l'activité
        auditLocationUpdate(clientId, oldLocation, request, client.getAgence().getId());

        return toLocationDTO(savedClient);
    }

    /**
     * Obtenir la localisation d'un client
     */
    public ClientLocationDTO getClientLocation(Long clientId) {
        log.info("📍 Récupération localisation client: {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

        return toLocationDTO(client);
    }

    /**
     * Obtenir les clients proches avec calcul de distance correct (formule de Haversine)
     */
    public List<ClientLocationDTO> getClientsProches(Double latitude, Double longitude, Double radiusKm) {
        log.info("📍 Recherche clients proches: lat={}, lng={}, radius={}km", latitude, longitude, radiusKm);

        // Validation des paramètres
        validateCoordinatesStrict(latitude, longitude);
        if (radiusKm <= 0 || radiusKm > 100) {
            throw new BusinessException("Le rayon doit être entre 0 et 100 km", "INVALID_RADIUS");
        }

        // Récupérer tous les clients avec localisation
        List<Client> allClientsWithLocation = clientRepository.findAll().stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .collect(Collectors.toList());

        // Filtrer par distance avec calcul correct
        return allClientsWithLocation.stream()
                .filter(client -> {
                    double distance = calculateHaversineDistance(
                            latitude, longitude,
                            client.getLatitude().doubleValue(),
                            client.getLongitude().doubleValue()
                    );
                    return distance <= radiusKm;
                })
                .map(this::toLocationDTO)
                .collect(Collectors.toList());
    }

    /**
     * Validation stricte des coordonnées
     */
    public boolean validateCoordinates(Double latitude, Double longitude) {
        try {
            validateCoordinatesStrict(latitude, longitude);
            return true;
        } catch (Exception e) {
            log.warn("❌ Coordonnées invalides: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validation stricte avec exceptions
     */
    private void validateCoordinatesStrict(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new BusinessException("Les coordonnées ne peuvent pas être nulles", "NULL_COORDINATES");
        }

        // Validation globale
        if (latitude < -90 || latitude > 90) {
            throw new BusinessException("Latitude invalide (doit être entre -90 et 90)", "INVALID_LATITUDE");
        }

        if (longitude < -180 || longitude > 180) {
            throw new BusinessException("Longitude invalide (doit être entre -180 et 180)", "INVALID_LONGITUDE");
        }

        // Validation spécifique au Cameroun (avec tolérance)
        if (latitude < CAMEROON_MIN_LAT - 1 || latitude > CAMEROON_MAX_LAT + 1) {
            log.warn("⚠️ Latitude {} semble être en dehors du Cameroun", latitude);
        }

        if (longitude < CAMEROON_MIN_LNG - 1 || longitude > CAMEROON_MAX_LNG + 1) {
            log.warn("⚠️ Longitude {} semble être en dehors du Cameroun", longitude);
        }

        // Validation contre les coordonnées nulles exactes (0,0)
        if (Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001) {
            throw new BusinessException("Coordonnées (0,0) non autorisées", "NULL_ISLAND_COORDINATES");
        }
    }

    /**
     * Calcul de distance avec formule de Haversine (correct)
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Conversion Entity vers DTO
     */
    private ClientLocationDTO toLocationDTO(Client client) {
        return ClientLocationDTO.builder()
                .clientId(client.getId())
                .nomComplet(client.getPrenom() + " " + client.getNom())
                .latitude(client.getLatitude() != null ? client.getLatitude().doubleValue() : null)
                .longitude(client.getLongitude() != null ? client.getLongitude().doubleValue() : null)
                .coordonneesSaisieManuelle(client.getCoordonneesSaisieManuelle())
                .adresseComplete(client.getAdresseComplete())
                .dateMajCoordonnees(client.getDateMajCoordonnees())
                .source(client.getCoordonneesSaisieManuelle() != null && client.getCoordonneesSaisieManuelle() ? "MANUAL" : "GPS")
                .ville(client.getVille())
                .quartier(client.getQuartier())
                .collecteurId(client.getCollecteur().getId())
                .agenceId(client.getAgence().getId())
                .build();
    }

    /**
     * Audit de la mise à jour de localisation
     */
    private void auditLocationUpdate(Long clientId, String oldLocation, LocationUpdateRequest request, Long agenceId) {
        try {
            String details = String.format(
                    "{\"oldLocation\": \"%s\", \"newLocation\": \"lat: %s, lng: %s\", \"manual\": %s, \"address\": \"%s\"}",
                    oldLocation,
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getSaisieManuelle(),
                    request.getAdresseComplete() != null ? request.getAdresseComplete() : ""
            );

            auditService.logActivity(AuditLogRequest.builder()
                    .userId(getCurrentUserId())
                    .userType(getCurrentUserType())
                    .action("UPDATE_CLIENT_LOCATION")
                    .entityType("CLIENT")
                    .entityId(clientId)
                    .details(details)
                    .agenceId(agenceId)
                    .build()
            );
        } catch (Exception e) {
            log.error("❌ Erreur audit localisation: {}", e.getMessage());
        }
    }

    /**
     * Obtenir l'ID utilisateur courant
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            try {
                // TODO: Implémenter la récupération correcte de l'ID utilisateur
                return 1L; // Temporaire
            } catch (Exception e) {
                log.error("Erreur récupération ID utilisateur", e);
                return null;
            }
        }
        return null;
    }

    /**
     * Obtenir le type d'utilisateur courant
     */
    private String getCurrentUserType() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            String authority = auth.getAuthorities().iterator().next().getAuthority();
            if (authority.startsWith("ROLE_")) {
                authority = authority.substring(5);
            }
            return authority;
        }
        return "UNKNOWN";
    }
}