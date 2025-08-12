package org.example.collectfocep.exceptions;

import org.example.collectfocep.dto.TransferValidationResult;

/**
 * Exception lancée lors d'un dry-run pour déclencher le rollback
 * et retourner le résultat de validation
 */
public class DryRunException extends RuntimeException {
    private final TransferValidationResult result;
    
    public DryRunException(TransferValidationResult result) {
        super("Dry run - transaction will be rolled back");
        this.result = result;
    }
    
    public TransferValidationResult getResult() {
        return result;
    }
}