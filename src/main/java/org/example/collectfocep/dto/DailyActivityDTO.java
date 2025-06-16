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
public class DailyActivityDTO {

    private LocalDate date;
    private String jourSemaine;

    // Volumes
    private Double totalEpargne;
    private Double totalRetrait;
    private Double soldeNet;

    // Opérations
    private Long nombreEpargnes;
    private Long nombreRetraits;
    private Long nombreOperationsTotal;

    // Clients
    private Long nombreClientsActifs;
    private Long nouveauxClients;

    // Moyennes
    private Double moyenneEpargne;
    private Double moyenneRetrait;
}
