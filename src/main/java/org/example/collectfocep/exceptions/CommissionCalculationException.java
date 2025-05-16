package org.example.collectfocep.exceptions;

/**
 * Exception levée lors d'erreurs dans le calcul des commissions
 */
public class CommissionCalculationException extends RuntimeException {
    public CommissionCalculationException(String message) {
        super(message);
    }

    public CommissionCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}