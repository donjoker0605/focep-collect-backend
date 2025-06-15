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
public class TransactionDetailDTO {

    private Long id;
    private String reference;
    private String type; // EPARGNE, RETRAIT, TRANSFERT
    private String statut; // COMPLETED, PENDING, FAILED, CANCELLED
    private Double montant;
    private Double fraisTransaction;
    private String commentaire;
    private LocalDateTime dateTransaction;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Informations du client
    private ClientInfoDTO client;

    // Informations du collecteur
    private CollecteurInfoDTO collecteur;

    // Informations du compte
    private CompteInfoDTO compte;

    // Informations du journal
    private JournalInfoDTO journal;

    // Informations techniques
    private String adresseIP;
    private String userAgent;
    private String canalTransaction; // MOBILE, WEB, AGENCE

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientInfoDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String numeroCompte;
        private String telephone;
        private String adresse;
        private Boolean valide;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollecteurInfoDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String adresseMail;
        private String telephone;
        private String nomAgence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompteInfoDTO {
        private Long id;
        private String numeroCompte;
        private String typeCompte;
        private Double soldeAvantTransaction;
        private Double soldeApresTransaction;
        private String statut;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalInfoDTO {
        private Long id;
        private String statut;
        private LocalDateTime dateOuverture;
        private LocalDateTime dateFermeture;
        private String numeroJournal;
    }
}