package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "compte_collecteur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CompteCollecteur extends Compte {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_collecteur", nullable = false)
    private Collecteur collecteur;


    // Méthodes utilitaires pour l'Option A
    public boolean isService() {
        return "SERVICE".equals(getTypeCompte());
    }

    public boolean isManquant() {
        return "MANQUANT".equals(getTypeCompte());
    }

    public boolean isAttente() {
        return "ATTENTE".equals(getTypeCompte());
    }

    public boolean isRemuneration() {
        return "REMUNERATION".equals(getTypeCompte());
    }

    public boolean isCharge() {
        return "CHARGE".equals(getTypeCompte());
    }
}