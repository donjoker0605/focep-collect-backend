package org.example.collectfocep.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientLocationDTO {
    private Long clientId;
    private String nomComplet;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean coordonneesSaisieManuelle;
    private String adresseComplete;
    private LocalDateTime dateMajCoordonnees;
}