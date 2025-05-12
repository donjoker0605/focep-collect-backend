package org.example.collectfocep.states;

/**
 * État initial d'une transaction
 */
public class InitiatedState implements TransactionState {

    @Override
    public void validate(Transaction transaction) {
        // Validation checks
        if (validateBusinessRules(transaction)) {
            transaction.setStatus(TransactionStatus.PENDING_VALIDATION);
            transaction.setState(new PendingValidationState());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setState(new FailedState());
        }
    }

    private boolean validateBusinessRules(Transaction transaction) {
        // Implémentation des règles de validation métier
        return true;
    }
}