package org.example.collectfocep.states;

/**
 * État d'une transaction en attente de validation
 */
public class PendingValidationState implements TransactionState {

    @Override
    public void process(Transaction transaction) {
        try {
            // Process transaction
            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction.setState(new ProcessingState());
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setState(new FailedState());
        }
    }
}