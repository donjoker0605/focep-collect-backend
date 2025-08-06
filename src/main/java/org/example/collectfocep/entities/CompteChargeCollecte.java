package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * C.C.C - Compte Charge Collecte (renommé selon spec FOCEP)
 * Anciennement CompteCharge - utilisé lors de rémunération quand Vi >= S
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "compte_charge_collecte")
public class CompteChargeCollecte extends Compte {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;
    
    @PrePersist
    private void prePersist() {
        if (getTypeCompte() == null) {
            setTypeCompte("CHARGE_COLLECTE");
        }
        if (getNomCompte() == null) {
            setNomCompte("Compte Charge Collecte - " + agence.getNom());
        }
    }
}