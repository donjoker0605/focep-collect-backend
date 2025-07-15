package org.example.collectfocep.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.GeocodingRequest;
import org.example.collectfocep.dto.GeocodingResponse;
import org.example.collectfocep.services.GeocodingService;
import org.example.collectfocep.util.ApiResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/geocoding")
@Slf4j
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;

    /**
     * Géocodage inverse : coordonnées -> adresse
     */
    @PostMapping("/reverse")
    @Cacheable(value = "geocoding", key = "#request.latitude + ',' + #request.longitude")
    public ResponseEntity<ApiResponse<GeocodingResponse>> reverseGeocode(
            @Valid @RequestBody GeocodingRequest request) {

        log.info("📍 Géocodage inverse: lat={}, lng={}",
                request.getLatitude(), request.getLongitude());

        try {
            GeocodingResponse response = geocodingService.reverseGeocode(
                    request.getLatitude(),
                    request.getLongitude()
            );

            return ResponseEntity.ok(ApiResponse.success(response, "Adresse trouvée"));
        } catch (Exception e) {
            log.error("❌ Erreur géocodage inverse", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GEOCODING_ERROR", e.getMessage()));
        }
    }

    /**
     * Géocodage direct : adresse -> coordonnées
     */
    @GetMapping("/forward")
    @Cacheable(value = "geocoding", key = "#address")
    public ResponseEntity<ApiResponse<GeocodingResponse>> forwardGeocode(
            @RequestParam String address) {

        log.info("📍 Géocodage direct: {}", address);

        try {
            GeocodingResponse response = geocodingService.forwardGeocode(address);
            return ResponseEntity.ok(ApiResponse.success(response, "Coordonnées trouvées"));
        } catch (Exception e) {
            log.error("❌ Erreur géocodage direct", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("GEOCODING_ERROR", e.getMessage()));
        }
    }
}