package org.example.collectfocep.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mouvement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double montant;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private String sens; // "debit" ou "credit"

    @Column(name = "date_operation", nullable = false)
    private LocalDateTime dateOperation;

    // ✅ AJOUT DES CHAMPS MANQUANTS
    @Column(name = "type_mouvement")
    private String typeMouvement; // "EPARGNE" ou "RETRAIT"

    @Column(name = "date_mouvement")
    private LocalDateTime dateMouvement;

    // ✅ RELATION AVEC CLIENT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @JsonIgnoreProperties({"mouvements", "collecteur", "agence"})
    private Client client;

    // ✅ RELATION AVEC COLLECTEUR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id")
    @JsonIgnoreProperties({"mouvements", "clients", "agence"})
    private Collecteur collecteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_source")
    @JsonIgnoreProperties({"mouvements", "client", "collecteur", "agence"})
    private Compte compteSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_destination")
    @JsonIgnoreProperties({"mouvements", "client", "collecteur", "agence"})
    private Compte compteDestination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    @JsonIgnoreProperties({"mouvements", "collecteur"})
    private Journal journal;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfert_id")
    @JsonIgnoreProperties({"mouvements"})
    private TransfertCompte transfert;


    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    public String getCompteSourceNumero() {
        return compteSource != null ? compteSource.getNumeroCompte() : null;
    }

    public String getCompteDestinationNumero() {
        return compteDestination != null ? compteDestination.getNumeroCompte() : null;
    }

    // ✅ GETTERS CORRIGÉS - maintenant les champs existent
    public String getTypeMouvement() {
        return this.typeMouvement;
    }

    public LocalDateTime getDateMouvement() {
        return this.dateMouvement;
    }

    public Client getClient() {
        return this.client;
    }

    @PrePersist
    @PreUpdate
    private void synchronizeDates() {
        // Si dateOperation est null, utiliser dateMouvement
        if (this.dateOperation == null && this.dateMouvement != null) {
            this.dateOperation = this.dateMouvement;
        }
        // Si dateOperation existe, elle prévaut (ne pas écraser)
        if (this.dateOperation == null) {
            this.dateOperation = LocalDateTime.now();
        }
    }

    public String getTypeMouvementCalcule() {
        if (this.typeMouvement != null) {
            return this.typeMouvement;
        }

        // Fallback basé sur le sens et le libellé
        if (this.libelle != null) {
            if (this.libelle.toLowerCase().contains("épargne") ||
                    this.libelle.toLowerCase().contains("depot")) {
                return "EPARGNE";
            } else if (this.libelle.toLowerCase().contains("retrait")) {
                return "RETRAIT";
            }
        }

        return this.sens != null ? this.sens.toUpperCase() : "INCONNU";
    }
}