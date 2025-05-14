package org.example.collectfocep.exceptions;

public class CompteGenerationException extends RuntimeException {
    public CompteGenerationException(String message) {
        super(message);
    }

    public CompteGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}