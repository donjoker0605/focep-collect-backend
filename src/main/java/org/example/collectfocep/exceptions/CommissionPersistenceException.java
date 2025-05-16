package org.example.collectfocep.exceptions;

/**
 * Exception levée lors d'erreurs de persistance des commissions
 */
public class CommissionPersistenceException extends RuntimeException {
    public CommissionPersistenceException(String message) {
        super(message);
    }

    public CommissionPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}