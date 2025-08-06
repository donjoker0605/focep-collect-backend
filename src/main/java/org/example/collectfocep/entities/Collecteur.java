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
public class Collecteur extends Utilisateur {

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

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
}