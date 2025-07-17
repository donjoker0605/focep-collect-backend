package org.example.collectfocep.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 🏦 Compte Agence - Représente les fonds versés par les collecteurs à l'agence
 *
 * LOGIQUE MÉTIER :
 * - Ce compte reçoit tous les versements des collecteurs
 * - Selon la demande utilisateur, ce compte doit rester "négatif"
 * - Chaque agence a un compte agence unique
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "compte_agence")
public class CompteAgence extends Compte {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;

    /**
     * Factory method pour créer un compte agence
     */
    public static CompteAgence createForAgence(Agence agence) {
        return CompteAgence.builder()
                .agence(agence)
                .typeCompte("AGENCE")
                .nomCompte("Compte Agence " + agence.getNomAgence())
                .numeroCompte("AGC-" + agence.getCodeAgence() + "-" + System.currentTimeMillis())
                .solde(0.0)
                .version(0L)
                .build();
    }

    /**
     * Vérifie si le compte est dans l'état attendu (négatif selon logique utilisateur)
     */
    public boolean isInExpectedState() {
        // Selon la logique utilisateur, le compte agence doit être négatif
        return getSolde() <= 0;
    }

    /**
     * Calcule le montant total versé (valeur absolue du solde si négatif)
     */
    public double getMontantTotalVerse() {
        return getSolde() < 0 ? Math.abs(getSolde()) : getSolde();
    }
}