package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    // STATISTIQUES GÉNÉRALES
    private Long totalCollecteurs;
    private Long totalClients;
    private Long agencesActives;

    // STATUT DES COLLECTEURS
    private Long collecteursActifs;
    private Long collecteursInactifs;

    // STATUT DES CLIENTS
    private Long clientsActifs;
    private Long clientsInactifs;

    // DONNÉES FINANCIÈRES (par période)
    private Double totalEpargne;
    private Double totalRetrait;
    private Double soldeTotal;

    // 🆕 NOUVEAUX: Données par période configurables
    private Double epargneAujourdhui;
    private Double retraitsAujourdhui;
    private Double soldeAujourdhui;
    
    private Double epargneSemaine;
    private Double retraitsSemaine; 
    private Double soldeSemaine;
    
    private Double epargneMois;
    private Double retraitsMois;
    private Double soldeMois;

    // COMMISSIONS
    private Long commissionsEnAttente;
    private Double totalCommissions;
    private Double commissionsATrait;

    // MÉTADONNÉES
    private LocalDateTime lastUpdate;
    private String periode;

    // STATISTIQUES TENDANCES (optionnel)
    private Double croissanceClients;
    private Double croissanceEpargne;
    private Double tauxActivite;

    // ALERTES SYSTÈME
    private Long alertesSysteme;
    private Long collecteursSansActivite;
    private Long clientsInactifsProlonges;

    // MÉTHODES UTILITAIRES
    public Double getTauxCollecteursActifs() {
        if (totalCollecteurs == null || totalCollecteurs == 0) return 0.0;
        return (collecteursActifs != null ? collecteursActifs.doubleValue() : 0.0) / totalCollecteurs.doubleValue() * 100;
    }

    public Double getTauxClientsActifs() {
        if (totalClients == null || totalClients == 0) return 0.0;
        return (clientsActifs != null ? clientsActifs.doubleValue() : 0.0) / totalClients.doubleValue() * 100;
    }

    public Double getSoldeNet() {
        double epargne = totalEpargne != null ? totalEpargne : 0.0;
        double retrait = totalRetrait != null ? totalRetrait : 0.0;
        return epargne - retrait;
    }

    public boolean hasAlertes() {
        return (alertesSysteme != null && alertesSysteme > 0) ||
                (collecteursSansActivite != null && collecteursSansActivite > 0) ||
                (clientsInactifsProlonges != null && clientsInactifsProlonges > 0);
    }

    public static DashboardStatsDTO createDefault() {
        return DashboardStatsDTO.builder()
                .totalCollecteurs(0L)
                .totalClients(0L)
                .agencesActives(0L)
                .collecteursActifs(0L)
                .collecteursInactifs(0L)
                .clientsActifs(0L)
                .clientsInactifs(0L)
                .totalEpargne(0.0)
                .totalRetrait(0.0)
                .soldeTotal(0.0)
                .commissionsEnAttente(0L)
                .totalCommissions(0.0)
                .commissionsATrait(0.0)
                .lastUpdate(LocalDateTime.now())
                .periode("Actuelle")
                .croissanceClients(0.0)
                .croissanceEpargne(0.0)
                .tauxActivite(0.0)
                .alertesSysteme(0L)
                .collecteursSansActivite(0L)
                .clientsInactifsProlonges(0L)
                .build();
    }
}