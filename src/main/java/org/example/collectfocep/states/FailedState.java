package org.example.collectfocep.states;

/**
 * État d'une transaction ayant échoué
 */
public class FailedState implements TransactionState {

    @Override
    public void validate(Transaction transaction) {
        // Une transaction échouée ne peut plus être validée
        throw new IllegalStateException("Impossible de valider une transaction en échec");
    }

    @Override
    public void process(Transaction transaction) {
        // Une transaction échouée ne peut plus être traitée
        throw new IllegalStateException("Impossible de traiter une transaction en échec");
    }

    @Override
    public void complete(Transaction transaction) {
        // Une transaction échouée ne peut plus être complétée
        throw new IllegalStateException("Impossible de compléter une transaction en échec");
    }
}