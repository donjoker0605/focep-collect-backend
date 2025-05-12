package org.example.collectfocep.exceptions;

public class CollecteException extends RuntimeException {
    private final String code;

    public CollecteException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
