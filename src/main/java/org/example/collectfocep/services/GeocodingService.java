package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.GeocodingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeocodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.geocoding.provider:nominatim}")
    private String provider;

    @Value("${app.geocoding.nominatim.url:https://nominatim.openstreetmap.org}")
    private String nominatimUrl;

    @Value("${app.geocoding.google.api-key:}")
    private String googleApiKey;

    @Value("${app.geocoding.google.url:https://maps.googleapis.com/maps/api/geocode/json}")
    private String googleUrl;

    @Value("${app.geocoding.timeout:5}")
    private int timeoutSeconds;

    @Bean
    public RestTemplate customRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    // ================ Méthodes publiques ================ //

    @Cacheable(value = "geocoding-reverse", key = "#latitude + ',' + #longitude")
    public GeocodingResponse reverseGeocode(Double latitude, Double longitude) {
        log.info("📍 Géocodage inverse: {}, {} avec provider: {}", latitude, longitude, provider);

        try {
            if ("google".equalsIgnoreCase(provider) && !googleApiKey.isEmpty()) {
                return reverseGeocodeGoogle(latitude, longitude);
            } else {
                return reverseGeocodeNominatim(latitude, longitude);
            }
        } catch (Exception e) {
            log.error("❌ Erreur avec provider principal, fallback...", e);
            return handleGeocodingFallback(latitude, longitude, e);
        }
    }

    @Cacheable(value = "geocoding-forward", key = "#address")
    public GeocodingResponse forwardGeocode(String address) {
        String normalizedAddress = normalizeAddress(address);
        log.info("📍 Géocodage direct: {}", normalizedAddress);

        try {
            if ("google".equalsIgnoreCase(provider) && !googleApiKey.isEmpty()) {
                return forwardGeocodeGoogle(normalizedAddress);
            } else {
                return forwardGeocodeNominatim(normalizedAddress);
            }
        } catch (Exception e) {
            log.error("❌ Erreur géocodage direct", e);
            throw new RuntimeException("Impossible de géocoder l'adresse: " + normalizedAddress, e);
        }
    }

    // ================ Méthodes privées ================ //

    private GeocodingResponse handleGeocodingFallback(Double latitude, Double longitude, Exception originalException) {
        try {
            if ("google".equalsIgnoreCase(provider)) {
                return reverseGeocodeNominatim(latitude, longitude);
            } else if (!googleApiKey.isEmpty()) {
                return reverseGeocodeGoogle(latitude, longitude);
            }
        } catch (Exception fallbackError) {
            log.error("❌ Tous les providers ont échoué", fallbackError);
        }
        return generateApproximateAddress(latitude, longitude);
    }

    private String normalizeAddress(String address) {
        if (!address.toLowerCase(Locale.FRENCH).contains("cameroun") &&
                !address.toLowerCase(Locale.FRENCH).contains("cameroon")) {
            return address + ", Cameroun";
        }
        return address;
    }

    private GeocodingResponse reverseGeocodeNominatim(Double latitude, Double longitude) {
        String url = buildNominatimReverseUrl(latitude, longitude);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            return parseNominatimResponse(root, latitude, longitude);
        } catch (Exception e) {
            log.error("Erreur Nominatim", e);
            throw new RuntimeException("Échec géocodage Nominatim", e);
        }
    }

    private GeocodingResponse reverseGeocodeGoogle(Double latitude, Double longitude) {
        String url = buildGoogleReverseUrl(latitude, longitude);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            return parseGoogleResponse(root, latitude, longitude);
        } catch (Exception e) {
            log.error("Erreur Google Maps", e);
            throw new RuntimeException("Échec géocodage Google", e);
        }
    }

    private GeocodingResponse forwardGeocodeNominatim(String address) {
        String url = buildNominatimForwardUrl(address);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode results = objectMapper.readTree(response);
            return parseNominatimForwardResponse(results);
        } catch (Exception e) {
            log.error("Erreur Nominatim forward", e);
            throw new RuntimeException("Aucun résultat Nominatim trouvé", e);
        }
    }

    private GeocodingResponse forwardGeocodeGoogle(String address) {
        String url = buildGoogleForwardUrl(address);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            return parseGoogleForwardResponse(root);
        } catch (Exception e) {
            log.error("Erreur Google forward", e);
            throw new RuntimeException("Aucun résultat Google trouvé", e);
        }
    }

    // ================ Méthodes de construction d'URL ================ //

    private String buildNominatimReverseUrl(Double latitude, Double longitude) {
        return UriComponentsBuilder.fromHttpUrl(nominatimUrl + "/reverse")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("accept-language", "fr")
                .build()
                .toUriString();
    }

    private String buildGoogleReverseUrl(Double latitude, Double longitude) {
        return UriComponentsBuilder.fromHttpUrl(googleUrl)
                .queryParam("latlng", latitude + "," + longitude)
                .queryParam("key", googleApiKey)
                .queryParam("language", "fr")
                .build()
                .toUriString();
    }

    private String buildNominatimForwardUrl(String address) {
        return UriComponentsBuilder.fromHttpUrl(nominatimUrl + "/search")
                .queryParam("q", address)
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("limit", 1)
                .queryParam("countrycodes", "CM")
                .build()
                .toUriString();
    }

    private String buildGoogleForwardUrl(String address) {
        return UriComponentsBuilder.fromHttpUrl(googleUrl)
                .queryParam("address", address)
                .queryParam("key", googleApiKey)
                .queryParam("components", "country:CM")
                .build()
                .toUriString();
    }

    // ================ Méthodes de parsing ================ //

    private GeocodingResponse parseNominatimResponse(JsonNode root, Double latitude, Double longitude) {
        JsonNode address = root.path("address");

        return GeocodingResponse.builder()
                .address(root.path("display_name").asText())
                .city(getFirstNonEmpty(
                        address.path("city").asText(),
                        address.path("town").asText(),
                        address.path("village").asText()
                ))
                .region(address.path("state").asText())
                .country(address.path("country").asText())
                .postalCode(address.path("postcode").asText())
                .latitude(latitude)
                .longitude(longitude)
                .isApproximate(false)
                .build();
    }

    private GeocodingResponse parseGoogleResponse(JsonNode root, Double latitude, Double longitude) {
        JsonNode results = root.path("results");
        if (results.isEmpty()) {
            throw new RuntimeException("Aucun résultat Google Maps");
        }

        JsonNode result = results.get(0);
        JsonNode components = result.path("address_components");

        return GeocodingResponse.builder()
                .address(result.path("formatted_address").asText())
                .city(extractComponent(components, "locality"))
                .region(extractComponent(components, "administrative_area_level_1"))
                .country(extractComponent(components, "country"))
                .postalCode(extractComponent(components, "postal_code"))
                .latitude(latitude)
                .longitude(longitude)
                .isApproximate(false)
                .build();
    }

    private GeocodingResponse parseNominatimForwardResponse(JsonNode results) {
        if (results.isEmpty()) {
            throw new RuntimeException("Aucun résultat Nominatim trouvé");
        }

        JsonNode result = results.get(0);
        return GeocodingResponse.builder()
                .address(result.path("display_name").asText())
                .latitude(result.path("lat").asDouble())
                .longitude(result.path("lon").asDouble())
                .isApproximate(false)
                .build();
    }

    private GeocodingResponse parseGoogleForwardResponse(JsonNode root) {
        JsonNode results = root.path("results");
        if (results.isEmpty()) {
            throw new RuntimeException("Aucun résultat Google trouvé");
        }

        JsonNode result = results.get(0);
        JsonNode location = result.path("geometry").path("location");

        return GeocodingResponse.builder()
                .address(result.path("formatted_address").asText())
                .latitude(location.path("lat").asDouble())
                .longitude(location.path("lng").asDouble())
                .isApproximate(false)
                .build();
    }

    // ================ Méthodes utilitaires ================ //

    private GeocodingResponse generateApproximateAddress(Double latitude, Double longitude) {
        String region = "Cameroun";
        String city = "Localisation approximative";

        if (latitude >= 9.0 && latitude <= 13.0 && longitude >= 13.0 && longitude <= 16.0) {
            region = "Extrême-Nord"; city = "Maroua";
        } else if (latitude >= 7.0 && latitude <= 9.0 && longitude >= 13.0 && longitude <= 15.0) {
            region = "Nord"; city = "Garoua";
        } else if (latitude >= 5.0 && latitude <= 7.0 && longitude >= 13.0 && longitude <= 15.0) {
            region = "Adamaoua"; city = "Ngaoundéré";
        } else if (latitude >= 3.5 && latitude <= 5.0 && longitude >= 11.0 && longitude <= 13.0) {
            region = "Centre";
            if (Math.abs(latitude - 3.848) < 0.2 && Math.abs(longitude - 11.502) < 0.2) {
                city = "Yaoundé";
            }
        } else if (latitude >= 3.5 && latitude <= 4.5 && longitude >= 9.0 && longitude <= 10.0) {
            region = "Littoral";
            if (Math.abs(latitude - 4.0483) < 0.1 && Math.abs(longitude - 9.7043) < 0.1) {
                city = "Douala";
            }
        } else if (latitude >= 4.0 && latitude <= 6.0 && longitude >= 8.5 && longitude <= 11.0) {
            region = "Ouest"; city = "Bafoussam";
        }

        return GeocodingResponse.builder()
                .address(String.format("%s, %s, Cameroun", city, region))
                .city(city)
                .region(region)
                .country("Cameroun")
                .latitude(latitude)
                .longitude(longitude)
                .isApproximate(true)
                .build();
    }

    private String getFirstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private String extractComponent(JsonNode components, String type) {
        for (JsonNode component : components) {
            for (JsonNode t : component.path("types")) {
                if (type.equals(t.asText())) {
                    return component.path("long_name").asText();
                }
            }
        }
        return "";
    }
}