package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * C.S.C - Compte Salaire Collecteur (renommé selon spec FOCEP)
 * Anciennement CompteRemuneration - reçoit la rémunération du collecteur
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "compte_salaire_collecteur")
public class CompteSalaireCollecteur extends Compte {
    
    @OneToOne
    @JoinColumn(name = "id_collecteur")
    private Collecteur collecteur;
    
    @PrePersist
    private void prePersist() {
        if (getTypeCompte() == null) {
            setTypeCompte("SALAIRE_COLLECTEUR");
        }
        if (getNomCompte() == null) {
            setNomCompte("Compte Salaire Collecteur - " + collecteur.getNom());
        }
    }
}