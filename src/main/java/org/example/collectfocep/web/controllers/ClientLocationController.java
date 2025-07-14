package org.example.collectfocep.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ClientLocationDTO;
import org.example.collectfocep.dto.LocationUpdateRequest;
import org.example.collectfocep.services.GeolocationService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Slf4j
@RequiredArgsConstructor
public class ClientLocationController {

    private final GeolocationService geolocationService;

    /**
     * Mettre à jour la localisation d'un client
     */
    @PutMapping("/{clientId}/location")
    @PreAuthorize("@securityService.canManageClient(authentication, #clientId)")
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
}