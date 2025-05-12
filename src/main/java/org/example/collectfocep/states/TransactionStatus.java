package org.example.collectfocep.states;

public enum TransactionStatus {
    INITIATED,
    PENDING_VALIDATION,
    VALIDATED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}