package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * C.P.C.C - Compte Passage Commission Collecte
 * Ce compte reçoit les commissions "x" débité de tous les clients
 * lors du calcul de commission avant répartition
 */
@Entity
@Table(name = "compte_passage_commission_collecte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ComptePassageCommissionCollecte extends Compte {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;

    @Column(name = "periode_courante")
    private String periodeCourante; // Format YYYY-MM pour tracking
    
    @PrePersist
    private void prePersist() {
        if (getTypeCompte() == null) {
            setTypeCompte("PASSAGE_COMMISSION_COLLECTE");
        }
        if (getNomCompte() == null) {
            setNomCompte("Compte Passage Commission Collecte - " + agence.getNom());
        }
    }
}