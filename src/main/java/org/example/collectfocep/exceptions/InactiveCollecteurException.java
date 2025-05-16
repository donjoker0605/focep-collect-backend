package org.example.collectfocep.exceptions;

/**
 * Exception levée pour collecteur inactif
 */
public class InactiveCollecteurException extends RuntimeException {
    public InactiveCollecteurException(String message) {
        super(message);
    }
}