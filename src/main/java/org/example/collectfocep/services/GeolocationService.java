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

    // Constantes pour la validation des coordonnées (Cameroun)
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

        // Validation des données d'entrée
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BusinessException("Les coordonnées latitude et longitude sont requises", "MISSING_COORDINATES");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));

        // Validation des coordonnées AVANT sauvegarde
        validateCoordinatesStrict(request.getLatitude(), request.getLongitude());

        // Sauvegarder l'ancienne localisation pour l'audit
        String oldLocation = client.hasLocation()
                ? String.format("lat: %s, lng: %s", client.getLatitude(), client.getLongitude())
                : "Aucune localisation";

        // Mettre à jour la localisation
        client.updateLocation(
                request.getLatitude(),
                request.getLongitude(),
                request.getSaisieManuelle(),
                request.getAdresseComplete()
        );

        Client savedClient = clientRepository.save(client);
        log.info("✅ Localisation mise à jour: {}", savedClient.getId());

        // Enregistrer l'activité d'audit
        auditLocationUpdate(clientId, oldLocation, request, client.getAgence().getId());

        return toLocationDTO(savedClient);
    }

    /**
     * Obtenir la localisation d'un client
     */
    public ClientLocationDTO getClientLocation(Long clientId) {
        log.info("📍 Récupération localisation client: {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));

        return toLocationDTO(client);
    }

    /**
     * Obtenir les clients proches avec calcul de distance correct (formule de Haversine)
     */
    public List<ClientLocationDTO> getClientsProches(Double latitude, Double longitude, Double radiusKm) {
        log.info("📍 Recherche clients proches: lat={}, lng={}, radius={}km", latitude, longitude, radiusKm);

        // Validation des paramètres
        validateCoordinatesStrict(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
        if (radiusKm <= 0 || radiusKm > 100) {
            throw new BusinessException("Le rayon doit être entre 0 et 100 km", "INVALID_RADIUS");
        }

        // Récupérer tous les clients avec localisation
        List<Client> allClientsWithLocation = clientRepository.findAll().stream()
                .filter(c -> c.hasLocation())
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
     * Rechercher des clients dans un rayon géographique (utilise la requête optimisée du repository)
     */
    public List<ClientLocationDTO> findClientsInRadius(Double latitude, Double longitude, Double radiusKm) {
        log.info("📍 Recherche optimisée clients dans rayon: lat={}, lng={}, radius={}km", latitude, longitude, radiusKm);

        // Validation des paramètres
        validateCoordinatesStrict(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
        if (radiusKm <= 0 || radiusKm > 100) {
            throw new BusinessException("Le rayon doit être entre 0 et 100 km", "INVALID_RADIUS");
        }

        // Utiliser la requête optimisée du repository
        List<Client> nearbyClients = clientRepository.findClientsInRadius(latitude, longitude, radiusKm);

        return nearbyClients.stream()
                .map(this::toLocationDTO)
                .collect(Collectors.toList());
    }

    /**
     * Validation stricte des coordonnées
     */
    public boolean validateCoordinates(Double latitude, Double longitude) {
        try {
            validateCoordinatesStrict(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
            return true;
        } catch (Exception e) {
            log.warn("❌ Coordonnées invalides: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validation stricte avec exceptions
     */
    private void validateCoordinatesStrict(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new BusinessException("Les coordonnées ne peuvent pas être nulles", "NULL_COORDINATES");
        }

        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();

        // Validation globale
        if (lat < -90 || lat > 90) {
            throw new BusinessException("Latitude invalide (doit être entre -90 et 90)", "INVALID_LATITUDE");
        }

        if (lng < -180 || lng > 180) {
            throw new BusinessException("Longitude invalide (doit être entre -180 et 180)", "INVALID_LONGITUDE");
        }

        // Validation spécifique au Cameroun (avec tolérance pour les tests)
        if (lat < CAMEROON_MIN_LAT - 2 || lat > CAMEROON_MAX_LAT + 2) {
            log.warn("⚠️ Latitude {} semble être en dehors du Cameroun", lat);
        }

        if (lng < CAMEROON_MIN_LNG - 2 || lng > CAMEROON_MAX_LNG + 2) {
            log.warn("⚠️ Longitude {} semble être en dehors du Cameroun", lng);
        }

        // Validation contre les coordonnées nulles exactes (0,0) - Golfe de Guinée
        if (Math.abs(lat) < 0.001 && Math.abs(lng) < 0.001) {
            throw new BusinessException("Coordonnées (0,0) non autorisées", "NULL_ISLAND_COORDINATES");
        }

        // Détection coordonnées émulateur (Mountain View, CA) - acceptées en mode développement
        if (Math.abs(lat - 37.4219983) < 0.001 && Math.abs(lng - (-122.084)) < 0.001) {
            log.info("🔧 Coordonnées émulateur détectées (Mountain View, CA) - Mode développement");
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
                .nomComplet(client.getNomComplet())
                .latitude(client.getLatitude() != null ? client.getLatitude().doubleValue() : null)
                .longitude(client.getLongitude() != null ? client.getLongitude().doubleValue() : null)
                .coordonneesSaisieManuelle(client.getCoordonneesSaisieManuelle())
                .adresseComplete(client.getAdresseComplete())
                .dateMajCoordonnees(client.getDateMajCoordonnees())
                .source(client.isManualLocation() ? "MANUAL" : "GPS")
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
                // TODO: Implémenter la récupération correcte de l'ID utilisateur depuis le JWT
                return 1L; // Temporaire - À remplacer par l'extraction du JWT
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

    /**
     * Statistiques de géolocalisation
     */
    public LocationStatisticsDTO getLocationStatistics() {
        long totalClients = clientRepository.count();
        long clientsWithLocation = clientRepository.findAll().stream()
                .mapToLong(c -> c.hasLocation() ? 1 : 0)
                .sum();

        long manualEntries = clientRepository.findAll().stream()
                .mapToLong(c -> c.isManualLocation() ? 1 : 0)
                .sum();

        return LocationStatisticsDTO.builder()
                .totalClients(totalClients)
                .clientsWithLocation(clientsWithLocation)
                .clientsWithoutLocation(totalClients - clientsWithLocation)
                .manualEntries(manualEntries)
                .gpsEntries(clientsWithLocation - manualEntries)
                .coveragePercentage(totalClients > 0 ? (double) clientsWithLocation / totalClients * 100 : 0)
                .build();
    }

    /**
     * DTO interne pour les statistiques de localisation
     */
    @lombok.Data
    @lombok.Builder
    public static class LocationStatisticsDTO {
        private long totalClients;
        private long clientsWithLocation;
        private long clientsWithoutLocation;
        private long manualEntries;
        private long gpsEntries;
        private double coveragePercentage;
    }
}