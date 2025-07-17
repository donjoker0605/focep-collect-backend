package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteurComptesDTO {

    private Long collecteurId;
    private String collecteurNom;

    // Compte Service
    private Long compteServiceId;
    private String compteServiceNumero;
    private Double compteServiceSolde;

    // Compte Manquant
    private Long compteManquantId;
    private String compteManquantNumero;
    private Double compteManquantSolde;

    // Compte Attente
    private Long compteAttenteId;
    private String compteAttenteNumero;
    private Double compteAttenteSolde;

    // Compte Rémunération
    private Long compteRemunerationId;
    private String compteRemunerationNumero;
    private Double compteRemunerationSolde;

    // Totaux
    private Double totalCreances; // Ce que le collecteur doit rembourser
    private Double totalAvoirs;   // Ce que l'entreprise doit au collecteur
    private Double soldeNet;      // Différence
}