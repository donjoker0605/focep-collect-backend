package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * C.P.C - Compte Produit Collecte  
 * Ce compte reçoit la part de la microfinance EMF
 * lors de la rémunération (reste de S après rémunération collecteur)
 */
@Entity
@Table(name = "compte_produit_collecte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CompteProduitCollecte extends Compte {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;
    
    @PrePersist
    private void prePersist() {
        if (getTypeCompte() == null) {
            setTypeCompte("PRODUIT_COLLECTE");
        }
        if (getNomCompte() == null) {
            setNomCompte("Compte Produit Collecte - " + agence.getNom());
        }
    }
}