package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDTO {

    // Statistiques principales
    private Integer totalCollecteurs;
    private Integer collecteursActifs;
    private Integer totalClients;
    private Integer clientsValides;
    private Double totalEpargnes;
    private Double totalRetraits;
    private Integer commissionsEnAttente;
    private Double totalCommissions;

    // Performance de l'agence
    private Double croissanceClients; // En pourcentage ce mois
    private Double croissanceEpargne; // En pourcentage ce mois
    private Double tauxValidationClients; // En pourcentage
    private Double moyenneClientsParCollecteur;

    // Top performers
    private List<TopCollecteurDTO> topCollecteurs;
    private List<ClientVIPDTO> clientsVIP;

    // Alertes et notifications
    private Integer alertesEnCours;
    private Integer journauxEnAttente;
    private Integer commissionsATraiter;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCollecteurDTO {
        private Long id;
        private String nom;
        private String prenom;
        private Integer nombreClients;
        private Double volumeEpargne;
        private Double commissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientVIPDTO {
        private Long id;
        private String nom;
        private String prenom;
        private Double soldeTotal;
        private String collecteurNom;
        private String dernierTransaction;
    }
}