package org.example.collectfocep.exceptions;



public class BusinessException extends RuntimeException {
    private final String code;
    private final String details;

    public BusinessException(String message, String code) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public BusinessException(String message, String code, String details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
        this.details = null;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = "BUSINESS_ERROR";
        this.details = null;
    }

    public BusinessException(String message, String code, String details, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = details;
    }

    public String getCode() { // ✅ Nom cohérent avec l'usage
        return code;
    }

    public String getDetails() {
        return details;
    }
}