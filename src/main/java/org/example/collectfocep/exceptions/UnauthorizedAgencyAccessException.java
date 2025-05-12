package org.example.collectfocep.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAgencyAccessException extends RuntimeException {
    public UnauthorizedAgencyAccessException(String message) {
        super(message);
    }
}