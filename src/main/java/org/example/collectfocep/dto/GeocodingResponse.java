package org.example.collectfocep.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeocodingResponse {
    private String address;
    private String city;
    private String region;
    private String country;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private boolean isApproximate;
}