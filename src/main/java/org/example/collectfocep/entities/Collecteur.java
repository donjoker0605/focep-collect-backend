package org.example.collectfocep.entities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collecteurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"clients", "comptes", "agence", "rapport"})
@EqualsAndHashCode(callSuper = true, exclude = {"clients", "comptes", "rapport"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "clients", "comptes"})
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "Collecteur.full",
        attributeNodes = {
            @NamedAttributeNode("agence"),
            @NamedAttributeNode(value = "comptes"),
            @NamedAttributeNode(value = "clients", subgraph = "client-with-commission")
        },
        subgraphs = {
            @NamedSubgraph(
                name = "client-with-commission",
                attributeNodes = {
                    @NamedAttributeNode("commissionParameters")
                }
            )
        }
    ),
    @NamedEntityGraph(
        name = "Collecteur.withComptes",
        attributeNodes = {
            @NamedAttributeNode("agence"),
            @NamedAttributeNode("comptes")
        }
    )
})
public class Collecteur extends Utilisateur {

    // 🔥 MODIFICATION: Collecteurs créés inactifs par défaut selon requirements
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = false;

    @Column(name = "id_agence", nullable = false)
    private Long agenceId;

    @Column(name = "anciennete_en_mois", nullable = false)
    @Builder.Default
    private Integer ancienneteEnMois = 0;

    @Column(name = "montant_max_retrait", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal montantMaxRetrait = BigDecimal.valueOf(50000.0);

    @Column(name = "date_modification_montant")
    private LocalDateTime dateModificationMontantMax;

    @Column(name = "modifie_par")
    private String modifiePar;

    @Column(name = "rapport_id")
    private Long rapportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agence", insertable = false, updatable = false)
    @JsonIgnoreProperties({"collecteurs", "admins"})
    private Agence agence;

    @OneToMany(mappedBy = "collecteur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"collecteur", "comptes"})
    @Builder.Default
    private List<Client> clients = new ArrayList<>();

    @OneToMany(mappedBy = "collecteur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"collecteur"})
    @Builder.Default
    private List<CompteCollecteur> comptes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rapport_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"collecteurs"})
    private RapportMensuel rapport;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "fcm_token_updated_at")
    private LocalDateTime fcmTokenUpdatedAt;

    // ================================
    // MÉTHODES UTILITAIRES CORRIGÉES
    // ================================

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public LocalDateTime getFcmTokenUpdatedAt() {
        return fcmTokenUpdatedAt;
    }

    public void setFcmTokenUpdatedAt(LocalDateTime fcmTokenUpdatedAt) {
        this.fcmTokenUpdatedAt = fcmTokenUpdatedAt;
    }

    public void addClient(Client client) {
        if (clients == null) {
            clients = new ArrayList<>();
        }
        clients.add(client);
        client.setCollecteur(this);
    }

    public void removeClient(Client client) {
        if (clients != null) {
            clients.remove(client);
            client.setCollecteur(null);
        }
    }

    public void addCompte(CompteCollecteur compte) {
        if (comptes == null) {
            comptes = new ArrayList<>();
        }
        comptes.add(compte);
        compte.setCollecteur(this);
    }

    public boolean isActive() {
        return active != null && active;
    }

    public String getDisplayName() {
        return String.format("%s %s", this.getNom(), this.getPrenom());
    }

    // ================================
    // MÉTHODES DE COMPATIBILITÉ (TRANSITION)
    // ================================

    /**
     * Méthode de compatibilité pour retourner le montant en Double
     * À utiliser uniquement pour les DTOs et interfaces qui attendent Double
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public Double getMontantMaxRetraitAsDouble() {
        return montantMaxRetrait != null ? montantMaxRetrait.doubleValue() : null;
    }

    /**
     * Méthode de compatibilité pour définir le montant depuis Double
     * À utiliser uniquement pour la transition
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void setMontantMaxRetraitFromDouble(Double montant) {
        this.montantMaxRetrait = montant != null ? BigDecimal.valueOf(montant) : null;
    }

    /**
     * Vérifie si le collecteur peut effectuer des retraits
     */
    public boolean canPerformWithdrawals() {
        return Boolean.TRUE.equals(active) &&
                montantMaxRetrait != null &&
                montantMaxRetrait.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Vérifie si le collecteur est nouveau (moins de 3 mois d'ancienneté)
     */
    public boolean isNouveauCollecteur() {
        return ancienneteEnMois != null && ancienneteEnMois < 3;
    }

    // ================================
    // 🔥 SYSTÈME D'ANCIENNETÉ AUTOMATIQUE
    // ================================

    /**
     * Calcule l'ancienneté en mois depuis la date de création
     * @return ancienneté en mois
     */
    public int calculateAncienneteEnMois() {
        if (this.getDateCreation() == null) {
            return 0;
        }
        
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime dateCreation = this.getDateCreation();
        
        // Calcul précis en utilisant Period pour les mois
        int moisDifference = (maintenant.getYear() - dateCreation.getYear()) * 12 
                           + (maintenant.getMonthValue() - dateCreation.getMonthValue());
        
        // Si nous sommes avant le jour de création dans le mois courant, enlever 1 mois
        if (maintenant.getDayOfMonth() < dateCreation.getDayOfMonth()) {
            moisDifference--;
        }
        
        return Math.max(0, moisDifference);
    }

    /**
     * Met à jour l'ancienneté automatiquement
     */
    @PreUpdate
    @PrePersist
    public void updateAnciennete() {
        this.ancienneteEnMois = calculateAncienneteEnMois();
    }

    /**
     * Retourne le niveau d'ancienneté textuel
     */
    public String getNiveauAnciennete() {
        if (ancienneteEnMois == null || ancienneteEnMois < 1) {
            return "NOUVEAU"; // < 1 mois
        } else if (ancienneteEnMois < 3) {
            return "JUNIOR"; // 1-3 mois
        } else if (ancienneteEnMois < 12) {
            return "CONFIRMÉ"; // 3-12 mois
        } else if (ancienneteEnMois < 24) {
            return "SENIOR"; // 1-2 ans
        } else {
            return "EXPERT"; // > 2 ans
        }
    }

    /**
     * Retourne le coefficient de commission basé sur l'ancienneté
     */
    public double getCoefficientAnciennete() {
        String niveau = getNiveauAnciennete();
        switch (niveau) {
            case "NOUVEAU":
                return 1.0; // Pas de bonus
            case "JUNIOR":
                return 1.05; // +5%
            case "CONFIRMÉ":
                return 1.10; // +10%
            case "SENIOR":
                return 1.15; // +15%
            case "EXPERT":
                return 1.20; // +20%
            default:
                return 1.0;
        }
    }

    /**
     * Retourne un résumé de l'ancienneté pour l'affichage
     */
    public String getAncienneteSummary() {
        int mois = ancienneteEnMois != null ? ancienneteEnMois : 0;
        int annees = mois / 12;
        int moisRestants = mois % 12;
        
        if (annees == 0) {
            return mois == 1 ? "1 mois" : mois + " mois";
        } else if (moisRestants == 0) {
            return annees == 1 ? "1 an" : annees + " ans";
        } else {
            return annees + " an" + (annees > 1 ? "s" : "") + 
                   " et " + moisRestants + " mois";
        }
    }

    /**
     * Vérifie si le collecteur a droit à une promotion d'ancienneté
     */
    public boolean isEligibleForSeniorityPromotion() {
        // Promotion automatique tous les 3 mois les 12 premiers mois,
        // puis chaque année
        if (ancienneteEnMois == null) return false;
        
        if (ancienneteEnMois < 12) {
            return ancienneteEnMois % 3 == 0 && ancienneteEnMois > 0;
        } else {
            return ancienneteEnMois % 12 == 0;
        }
    }
}