package org.example.collectfocep.exceptions;

/**
 * 🔒 Exception pour les erreurs de validation business et techniques
 * 
 * Utilisée pour signaler les erreurs de validation qui doivent retourner
 * un code HTTP 400 (Bad Request) au client.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}