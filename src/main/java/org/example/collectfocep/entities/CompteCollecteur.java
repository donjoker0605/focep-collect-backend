package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "comptes_collecteur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CompteCollecteur extends Compte {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collecteur_id", nullable = false)
    private Collecteur collecteur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_compte_collecteur", nullable = false)
    private TypeCompteCollecteur typeCompteCollecteur;

    // ENUM pour les types de comptes
    public enum TypeCompteCollecteur {
        SERVICE("Compte de service"),
        MANQUANT("Compte manquant"),
        ATTENTE("Compte attente"),
        REMUNERATION("Compte rémunération"),
        CHARGE("Compte charge");

        private final String libelle;

        TypeCompteCollecteur(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    // Méthodes utilitaires
    public boolean isService() {
        return TypeCompteCollecteur.SERVICE.equals(typeCompteCollecteur);
    }

    public boolean isManquant() {
        return TypeCompteCollecteur.MANQUANT.equals(typeCompteCollecteur);
    }

    public boolean isAttente() {
        return TypeCompteCollecteur.ATTENTE.equals(typeCompteCollecteur);
    }

    public boolean allowsNegativeBalance() {
        return TypeCompteCollecteur.MANQUANT.equals(typeCompteCollecteur) ||
                TypeCompteCollecteur.CHARGE.equals(typeCompteCollecteur);
    }
}
