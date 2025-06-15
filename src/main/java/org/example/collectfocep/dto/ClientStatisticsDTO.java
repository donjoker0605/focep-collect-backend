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
public class ClientStatisticsDTO {

    private Long clientId;
    private String nomComplet;
    private String numeroCompte;

    // Statistiques transactionnelles
    private Long totalTransactions;
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeActuel;

    // Moyennes
    private Double moyenneEpargneParTransaction;
    private Double moyenneRetraitParTransaction;
    private Double frequenceTransactions; // Transactions par mois

    // Historique
    private LocalDateTime premierTransaction;
    private LocalDateTime derniereTransaction;
    private Long joursDepuisCreation;
    private Long joursDepuisDerniereActivite;

    // Performance
    private String categorieClient; // VIP, STANDARD, NOUVEAU, INACTIF
    private Double scoreFidelite; // Sur 100
    private Integer rangementVolume; // Classement par volume d'épargne

    // Prédictions
    private Double epargnePrevisionnelle; // Épargne prévue pour le mois suivant
    private String tendance; // CROISSANTE, STABLE, DECROISSANTE
    private Boolean risqueAttrition; // Risque de perdre le client
}

