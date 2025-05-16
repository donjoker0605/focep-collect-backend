package org.example.collectfocep.exceptions;

/**
 * Exception levée quand des commissions existent déjà pour une période
 */
public class CommissionAlreadyProcessedException extends RuntimeException {
    public CommissionAlreadyProcessedException(String message) {
        super(message);
    }
}