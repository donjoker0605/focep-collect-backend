package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * C.P.T - Compte Passage Taxe
 * Ce compte reçoit la TVA (19,25% de "x") débité de tous les clients
 * lors du calcul de commission
 */
@Entity
@Table(name = "compte_passage_taxe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ComptePassageTaxe extends Compte {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;

    @Column(name = "taux_tva")
    @Builder.Default
    private double tauxTVA = 0.1925; // 19,25% par défaut
    
    @PrePersist
    private void prePersist() {
        if (getTypeCompte() == null) {
            setTypeCompte("PASSAGE_TAXE");
        }
        if (getNomCompte() == null) {
            setNomCompte("Compte Passage Taxe - " + agence.getNom());
        }
    }
}