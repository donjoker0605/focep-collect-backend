package org.example.collectfocep.states;

/**
 * État d'une transaction en cours de traitement
 */
public class ProcessingState implements TransactionState {

    @Override
    public void complete(Transaction transaction) {
        // Complete the transaction
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setState(new CompletedState());
    }
}