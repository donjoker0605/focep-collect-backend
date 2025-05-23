package org.example.collectfocep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    // Statistiques générales
    private Long totalClients;
    private Double totalEpargne;
    private Double totalRetraits;
    private Double soldeTotal;
    private Double objectifMensuel;
    private Double progressionObjectif;

    // Statistiques de la journée
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

    // Commissions
    private Double commissionsMois;
    private Double commissionsAujourdhui;

    // Journal actuel
    private JournalDTO journalActuel;

    // Transactions récentes
    private List<TransactionRecenteDTO> transactionsRecentes;

    // Clients actifs récents
    private List<ClientDTO> clientsActifs;

    // Alertes et notifications
    private List<AlerteDTO> alertes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionRecenteDTO {
        private Long id;
        private String type;
        private Double montant;
        private LocalDate date;
        private String clientNom;
        private String clientPrenom;
        private String statut;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlerteDTO {
        private String type;
        private String message;
        private String niveau; // INFO, WARNING, ERROR
        private LocalDate date;
        private Boolean lue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalDTO {
        private Long id;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private String statut;
        private Double soldeInitial;
        private Double soldeActuel;
        private Long nombreTransactions;
    }
}