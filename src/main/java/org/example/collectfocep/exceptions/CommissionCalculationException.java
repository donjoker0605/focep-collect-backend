package org.example.collectfocep.exceptions;

public class CommissionCalculationException extends CollecteException {
    public CommissionCalculationException(String message) {
        super(message, "COMMISSION_CALC_ERROR");
    }
}
