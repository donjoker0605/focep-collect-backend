package org.example.collectfocep.exceptions;

/**
 * 🔐 Exception pour les erreurs de sécurité et d'autorisation
 * 
 * Utilisée pour signaler les erreurs de droits d'accès qui doivent retourner
 * un code HTTP 403 (Forbidden) au client.
 */
public class SecurityException extends RuntimeException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}