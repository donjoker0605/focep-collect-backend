package org.example.collectfocep.states;

/**
 * État d'une transaction complétée avec succès
 */
public class CompletedState implements TransactionState {

    @Override
    public void fail(Transaction transaction) {
        // Une transaction complétée ne peut plus échouer
        throw new IllegalStateException("Impossible de mettre en échec une transaction déjà complétée");
    }

    @Override
    public void cancel(Transaction transaction) {
        // Une transaction complétée ne peut plus être annulée
        throw new IllegalStateException("Impossible d'annuler une transaction déjà complétée");
    }
}