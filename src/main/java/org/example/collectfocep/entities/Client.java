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

    // Méthodes de validation
    @PrePersist
    private void validateConstraints() {
        if (collecteur == null) {
            throw new IllegalStateException("Un client doit être associé à un collecteur");
        }
        if (agence == null) {
            throw new IllegalStateException("Un client doit être associé à une agence");
        }
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        // Générer le numéro de compte s'il n'existe pas
        if (numeroCompte == null || numeroCompte.trim().isEmpty()) {
            numeroCompte = generateNumeroCompte();
        }
    }

    @PreUpdate
    private void updateModificationDate() {
        dateModification = LocalDateTime.now();
    }

    private String generateNumeroCompte() {
        // Format: CLI-{agenceId}-{timestamp}
        long timestamp = System.currentTimeMillis();
        Long agenceId = getAgenceId();
        return String.format("CLI-%d-%d", agenceId != null ? agenceId : 0, timestamp);
    }
}