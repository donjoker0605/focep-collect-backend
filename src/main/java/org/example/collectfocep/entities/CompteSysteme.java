package org.example.collectfocep.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "compte_systeme")
@Getter
@Setter
@SuperBuilder
public class CompteSysteme extends Compte {

    public CompteSysteme() {
        super();
    }

    /**
     * Méthode factory pour créer un compte système valide
     * Utilisez cette méthode au lieu du constructeur direct
     */
    public static CompteSysteme create(String typeCompte, String nomCompte, String numeroCompte) {
        // Validation stricte des paramètres
        if (typeCompte == null || typeCompte.trim().isEmpty()) {
            throw new IllegalArgumentException("Le type de compte ne peut pas être null ou vide");
        }
        if (nomCompte == null || nomCompte.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de compte ne peut pas être null ou vide");
        }
        if (numeroCompte == null || numeroCompte.trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro de compte ne peut pas être null ou vide");
        }

        return CompteSysteme.builder()
                .typeCompte(typeCompte.trim())
                .nomCompte(nomCompte.trim())
                .numeroCompte(numeroCompte.trim())
                .solde(0.0)
                .version(0L)
                .build();
    }

    /**
     * Méthode de validation pour s'assurer que l'instance est valide
     */
    public boolean isValid() {
        return this.getNomCompte() != null && !this.getNomCompte().trim().isEmpty() &&
                this.getNumeroCompte() != null && !this.getNumeroCompte().trim().isEmpty() &&
                this.getTypeCompte() != null && !this.getTypeCompte().trim().isEmpty();
    }
}