package org.example.collectfocep.exceptions;



public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final String details;

    // ✅ CONSTRUCTEUR AVEC 3 PARAMÈTRES (celui que vous utilisez)
    public BusinessException(String message, String errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    // ✅ CONSTRUCTEUR AVEC 1 PARAMÈTRE (pour compatibilité)
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.details = null;
    }

    // ✅ CONSTRUCTEUR AVEC MESSAGE ET CAUSE
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_ERROR";
        this.details = null;
    }

    // ✅ CONSTRUCTEUR COMPLET
    public BusinessException(String message, String errorCode, String details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }
}
