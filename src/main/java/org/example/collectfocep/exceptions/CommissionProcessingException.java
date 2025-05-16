package org.example.collectfocep.exceptions;

/**
 * Exception levée lors d'erreurs dans le traitement des commissions
 */
public class CommissionProcessingException extends RuntimeException {
    public CommissionProcessingException(String message) {
        super(message);
    }

    public CommissionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}