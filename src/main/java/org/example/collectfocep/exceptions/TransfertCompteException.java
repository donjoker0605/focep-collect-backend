package org.example.collectfocep.exceptions;

public class TransfertCompteException extends CollecteException {
    public TransfertCompteException(String message) {
        super(message, "TRANSFERT_ERROR");
    }
}