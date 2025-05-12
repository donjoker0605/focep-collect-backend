package org.example.collectfocep.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@Table(name = "compte_systeme")
public class CompteSysteme extends Compte {

    // Constructeur par défaut explicite
    public CompteSysteme() {
        super();
    }

    public static CompteSysteme create(String typeCompte, String nomCompte, String numeroCompte) {
        CompteSysteme compte = new CompteSysteme();
        compte.setTypeCompte(typeCompte);
        compte.setNomCompte(nomCompte);
        compte.setNumeroCompte(numeroCompte);
        compte.setSolde(0.0);
        return compte;
    }
}