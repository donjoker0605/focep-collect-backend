package org.example.collectfocep.dto;

import lombok.Data;


@Data
public class CompteDTO {
    private Long id;
    private String nomCompte;
    private String numeroCompte;
    private double solde;
    private String typeCompte;
    private Long collecteurId;
    private String collecteurNom;
}
