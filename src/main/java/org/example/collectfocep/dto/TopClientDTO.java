package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopClientDTO {

    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String telephone;

    // Collecteur
    private Long collecteurId;
    private String collecteurNom;

    // Statistiques
    private Double totalEpargne;
    private Double totalRetrait;
    private Double soldeActuel;
    private Long nombreOperations;

    // Activité
    private LocalDate dateInscription;
    private LocalDate derniereOperation;
    private Integer ancienneteEnMois;

    // Rang
    private Integer rang;
    private Double pourcentageDuTotal;
}
