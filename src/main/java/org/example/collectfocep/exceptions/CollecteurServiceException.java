package org.example.collectfocep.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CollecteurServiceException extends RuntimeException {
    public CollecteurServiceException(String message) {
        super(message);
    }

    public CollecteurServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}