package org.example.collectfocep.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemunerationCollecteurDTO {
    private Long collecteurId;
    private int ancienneteEnMois;
    private double montantFixe;
    private double totalCommissions;
    private double montantRemuneration;
    private double montantTVA;
    private LocalDate dateDebut;
    private LocalDate dateFin;
}
