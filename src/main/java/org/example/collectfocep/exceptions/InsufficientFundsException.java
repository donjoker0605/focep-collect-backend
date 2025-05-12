package org.example.collectfocep.exceptions;

public class InsufficientFundsException extends RuntimeException {
    private final double soldeActuel;
    private final double montantDemande;

    public InsufficientFundsException(String message, double soldeActuel, double montantDemande) {
        super(message);
        this.soldeActuel = soldeActuel;
        this.montantDemande = montantDemande;
    }

    public double getSoldeActuel() {
        return soldeActuel;
    }

    public double getMontantDemande() {
        return montantDemande;
    }
}
