package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients", indexes = {
        @Index(name = "idx_client_cni", columnList = "numero_cni"),
        @Index(name = "idx_client_collecteur", columnList = "id_collecteur"),
        @Index(name = "idx_client_agence", columnList = "id_agence"),
        @Index(name = "idx_client_date_creation", columnList = "date_creation")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(name = "numero_cni", nullable = false, unique = true)
    private String numeroCni;

    private String ville;
    private String quartier;
    private String telephone;

    @Column(name = "photo_path")
    private String photoPath;

    @Column(name = "numero_compte", unique = true)
    private String numeroCompte;

    @Column(nullable = false)
    @Builder.Default
    private Boolean valide = true;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_collecteur", nullable = false)
    private Collecteur collecteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agence", nullable = false)
    private Agence agence;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "coordonnees_saisie_manuelle")
    @Builder.Default
    private Boolean coordonneesSaisieManuelle = false;

    @Column(name = "adresse_complete")
    private String adresseComplete;

    @Column(name = "date_maj_coordonnees")
    private LocalDateTime dateMajCoordonnees;

    public Boolean getValide() {
        return this.valide;
    }

    public String getNumeroCompte() {
        return this.numeroCompte;
    }

    // Méthodes utilitaires pour récupérer les IDs (lecture seule)
    @Transient
    public Long getAgenceId() {
        return agence != null ? agence.getId() : null;
    }

    @Transient
    public Long getCollecteurId() {
        return collecteur != null ? collecteur.getId() : null;
    }

    // Méthodes lifecycle
    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
        if (valide == null) {
            valide = true;
        }
        if (coordonneesSaisieManuelle == null) {
            coordonneesSaisieManuelle = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }

    /**
     * Vérifie si le client a une localisation définie
     */
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    /**
     * Vérifie si la localisation a été saisie manuellement
     */
    public boolean isManualLocation() {
        return coordonneesSaisieManuelle != null && coordonneesSaisieManuelle;
    }


    /**
     * Obtient un résumé de la localisation
     */
    public String getLocationSummary() {
        if (!hasLocation()) {
            return "Pas de localisation";
        }

        String source = isManualLocation() ? "Saisie manuelle" : "GPS";
        return String.format("%.6f, %.6f (%s)",
                latitude.doubleValue(),
                longitude.doubleValue(),
                source);
    }

    /**
     * Obtient l'adresse complète ou construite
     */
    public String getFullAddress() {
        if (adresseComplete != null && !adresseComplete.trim().isEmpty()) {
            return adresseComplete;
        }

        if (ville != null && quartier != null) {
            return String.format("%s, %s", quartier, ville);
        }

        return ville != null ? ville : "Adresse non renseignée";
    }

    /**
     * Met à jour la localisation
     */
    public void updateLocation(BigDecimal newLatitude, BigDecimal newLongitude,
                               Boolean manualEntry, String fullAddress) {
        this.latitude = newLatitude;
        this.longitude = newLongitude;
        this.coordonneesSaisieManuelle = manualEntry != null ? manualEntry : false;
        this.adresseComplete = fullAddress;
        this.dateMajCoordonnees = LocalDateTime.now();
    }

    /**
     * Vérifie si la localisation nécessite une mise à jour
     */
    public boolean needsLocationUpdate() {
        return !hasLocation() ||
                (dateMajCoordonnees == null) ||
                (dateModification != null && dateModification.isAfter(dateMajCoordonnees));
    }

    /**
     * Obtient la dernière activité (création, modification ou mise à jour localisation)
     */
    public LocalDateTime getLastActivity() {
        LocalDateTime latest = dateCreation;

        if (dateModification != null && dateModification.isAfter(latest)) {
            latest = dateModification;
        }

        if (dateMajCoordonnees != null && dateMajCoordonnees.isAfter(latest)) {
            latest = dateMajCoordonnees;
        }

        return latest;
    }

    // Méthodes utilitaires existantes
    public String getNomComplet() {
        return String.format("%s %s",
                prenom != null ? prenom : "",
                nom != null ? nom : "").trim();
    }

    public boolean isActive() {
        return valide != null && valide;
    }

}