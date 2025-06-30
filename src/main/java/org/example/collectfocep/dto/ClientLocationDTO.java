package org.example.collectfocep.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientLocationDTO {
    private Long clientId;
    private String nomComplet;
    private Double latitude;
    private Double longitude;
    private Boolean coordonneesSaisieManuelle;
    private String adresseComplete;
    private LocalDateTime dateMajCoordonnees;
}