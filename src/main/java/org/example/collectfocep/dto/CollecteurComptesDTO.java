package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 📊 DTO pour afficher l'état complet des comptes d'un collecteur
 * Inclut maintenant le compte agence pour avoir une vue d'ensemble
 */
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

    // ===== COMPTES DU COLLECTEUR =====

    /**
     * Compte service - Montant collecté en attente de versement
     */
    private Double compteServiceSolde;
    private String compteServiceNumero;

    /**
     * Compte manquant - Différentiel positif/négatif des versements
     */
    private Double compteManquantSolde;
    private String compteManquantNumero;

    /**
     * Compte attente - Commission en attente de distribution
     */
    private Double compteAttenteSolde;
    private String compteAttenteNumero;

    // ===== COMPTE AGENCE =====

    /**
     * Compte agence - Fonds versés par le collecteur à l'agence
     * Selon logique métier: devrait être négatif en fonctionnement normal
     */
    private Double compteAgenceSolde;
    private String compteAgenceNumero;
    private Boolean compteAgenceEtatNormal; // true si négatif ou zéro

    // ===== INDICATEURS CALCULÉS =====

    /**
     * Solde net du collecteur (service + manquant)
     */
    private Double soldeNet;

    /**
     * Total versé à l'agence (valeur absolue si compte agence négatif)
     */
    private Double totalVerseAgence;

    /**
     * Indicateur de santé financière
     */
    private String indicateurSante; // "BON", "ATTENTION", "CRITIQUE"

    /**
     * Peut effectuer un versement aujourd'hui
     */
    private Boolean peutVerser;

    /**
     * Message explicatif sur l'état des comptes
     */
    private String messageEtat;

    // ===== MÉTHODES UTILITAIRES =====

    public Double getSoldeNet() {
        if (soldeNet == null) {
            double service = compteServiceSolde != null ? compteServiceSolde : 0.0;
            double manquant = compteManquantSolde != null ? compteManquantSolde : 0.0;
            soldeNet = service + manquant;
        }
        return soldeNet;
    }

    public Double getTotalVerseAgence() {
        if (totalVerseAgence == null && compteAgenceSolde != null) {
            // Si le compte agence est négatif (normal), on prend la valeur absolue
            totalVerseAgence = compteAgenceSolde < 0 ? Math.abs(compteAgenceSolde) : compteAgenceSolde;
        }
        return totalVerseAgence != null ? totalVerseAgence : 0.0;
    }

    public Boolean getCompteAgenceEtatNormal() {
        if (compteAgenceEtatNormal == null && compteAgenceSolde != null) {
            // État normal = solde négatif ou zéro selon logique métier
            compteAgenceEtatNormal = compteAgenceSolde <= 0;
        }
        return compteAgenceEtatNormal != null ? compteAgenceEtatNormal : false;
    }

    public String getIndicateurSante() {
        if (indicateurSante == null) {
            double service = compteServiceSolde != null ? compteServiceSolde : 0.0;
            double manquant = compteManquantSolde != null ? compteManquantSolde : 0.0;

            if (service > 0 && manquant >= 0) {
                indicateurSante = "BON";
            } else if (service > 0 && manquant < 0 && Math.abs(manquant) < service) {
                indicateurSante = "ATTENTION";
            } else if (manquant < 0 && Math.abs(manquant) >= service) {
                indicateurSante = "CRITIQUE";
            } else {
                indicateurSante = "BON";
            }
        }
        return indicateurSante;
    }

    public Boolean getPeutVerser() {
        if (peutVerser == null) {
            double service = compteServiceSolde != null ? compteServiceSolde : 0.0;
            peutVerser = service > 0;
        }
        return peutVerser;
    }

    public String getMessageEtat() {
        if (messageEtat == null) {
            if (!getPeutVerser()) {
                messageEtat = "Aucun montant à verser (compte service vide)";
            } else if ("CRITIQUE".equals(getIndicateurSante())) {
                messageEtat = "Situation critique: manquant important détecté";
            } else if ("ATTENTION".equals(getIndicateurSante())) {
                messageEtat = "Attention: manquant détecté, à surveiller";
            } else if (!getCompteAgenceEtatNormal()) {
                messageEtat = "Attention: compte agence en état anormal (solde positif)";
            } else {
                messageEtat = "Situation normale, prêt pour versement";
            }
        }
        return messageEtat;
    }
}