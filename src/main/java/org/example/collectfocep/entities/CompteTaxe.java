package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * C.T - Compte Taxe
 * Ce compte reçoit les taxes lors de la rémunération
 * (transfert depuis C.P.T vers C.T)
 */
@Entity
@Table(name = "compte_taxe") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CompteTaxe extends Compte {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;
    
    @PrePersist
    private void prePersist() {
        if (getTypeCompte() == null) {
            setTypeCompte("TAXE");
        }
        if (getNomCompte() == null) {
            setNomCompte("Compte Taxe - " + agence.getNom());
        }
    }
}