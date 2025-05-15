package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients", indexes = {
        @Index(name = "idx_client_cni", columnList = "numero_cni"),
        @Index(name = "idx_client_collecteur", columnList = "id_collecteur"),
        @Index(name = "idx_client_agence", columnList = "id_agence")
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

    @Column(nullable = false)
    @Builder.Default
    private boolean valide = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_collecteur", nullable = false)
    private Collecteur collecteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agence", nullable = false)
    private Agence agence;

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
    @PreUpdate
    private void validateConstraints() {
        if (collecteur == null) {
            throw new IllegalStateException("Un client doit être associé à un collecteur");
        }
        if (agence == null) {
            throw new IllegalStateException("Un client doit être associé à une agence");
        }
    }
}