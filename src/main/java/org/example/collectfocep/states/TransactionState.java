package org.example.collectfocep.states;

/**
 * Interface pour le pattern État (State) qui gère les transitions
 * entre les différents états d'une transaction
 */
public interface TransactionState {

    /**
     * Valide la transaction et passe à l'étape suivante ou échoue
     */
    default void validate(Transaction transaction) {
        // Implémentation par défaut - ne fait rien
    }

    /**
     * Traite la transaction après validation
     */
    default void process(Transaction transaction) {
        // Implémentation par défaut - ne fait rien
    }

    /**
     * Complète la transaction après traitement réussi
     */
    default void complete(Transaction transaction) {
        // Implémentation par défaut - ne fait rien
    }

    /**
     * Met la transaction en échec
     */
    default void fail(Transaction transaction) {
        transaction.setStatus(TransactionStatus.FAILED);
    }

    /**
     * Annule la transaction
     */
    default void cancel(Transaction transaction) {
        transaction.setStatus(TransactionStatus.CANCELLED);
    }
}