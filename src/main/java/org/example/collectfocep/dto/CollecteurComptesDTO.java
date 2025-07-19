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
    private String collecteurPrenom;
    private String agenceNom;
    private Long agenceId;

    // Comptes collecteur
    private Double compteServiceSolde;
    private String compteServiceNumero;
    private Double compteManquantSolde;
    private String compteManquantNumero;
    private Double compteAttenteSolde;
    private String compteAttenteNumero;

    // Compte agence
    private Double compteAgenceSolde;
    private String compteAgenceNumero;

    // Méthodes calculées
    public Double getSoldeNet() {
        Double service = compteServiceSolde != null ? compteServiceSolde : 0.0;
        Double manquant = compteManquantSolde != null ? compteManquantSolde : 0.0;
        Double attente = compteAttenteSolde != null ? compteAttenteSolde : 0.0;
        return service + manquant + attente;
    }

    public boolean hasManquant() {
        return compteManquantSolde != null && compteManquantSolde < 0;
    }

    public boolean hasAttente() {
        return compteAttenteSolde != null && compteAttenteSolde > 0;
    }

    public boolean peutVerser() {
        return compteServiceSolde != null && compteServiceSolde != 0;
    }
}