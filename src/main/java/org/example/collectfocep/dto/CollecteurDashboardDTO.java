package org.example.collectfocep.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CollecteurDashboardDTO {
    private Long collecteurId;
    private String collecteurNom;
    private String collecteurPrenom;

    // Statistiques générales
    private Integer totalClients;
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeTotal;

    // Statistiques du jour
    private Long transactionsAujourdhui;
    private Double montantEpargneAujourdhui;
    private Double montantRetraitAujourdhui;
    private Long nouveauxClientsAujourdhui;

    // Statistiques de la semaine
    private Double montantEpargneSemaine;
    private Double montantRetraitSemaine;
    private Long transactionsSemaine;

    // Statistiques du mois
    private Double montantEpargneMois;
    private Double montantRetraitMois;
    private Long transactionsMois;

    // Objectifs et commissions
    private Double objectifMensuel;
    private Double progressionObjectif;
    private Double commissionsMois;
    private Double commissionsAujourdhui;

    // Journal actuel
    private JournalDTO journalActuel;

    // Transactions récentes
    private List<MouvementDTO> transactionsRecentes;

    // Clients actifs
    private List<ClientDTO> clientsActifs;

    // Alertes
    private List<AlerteDTO> alertes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdate;

    @Data
    @Builder
    public static class JournalDTO {
        private Long id;
        private String statut;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime dateDebut;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime dateFin;
        private Double soldeInitial;
        private Double soldeActuel;
        private Long nombreTransactions;
    }

    @Data
    @Builder
    public static class MouvementDTO {
        private Long id;
        private String type; // EPARGNE, RETRAIT
        private Double montant;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime date;
        private String clientNom;
        private String clientPrenom;
        private String statut;
    }

    @Data
    @Builder
    public static class ClientDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String telephone;
        private Double soldeActuel;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime derniereTransaction;
    }

    @Data
    @Builder
    public static class AlerteDTO {
        private String type; // INFO, WARNING, ERROR
        private String message;
        private String action;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime date;
    }
}