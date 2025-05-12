package org.example.collectfocep.constants;

public final class ErrorMessages {
    private ErrorMessages() {}

    public static final String RESOURCE_NOT_FOUND = "La ressource demandée n'existe pas : %s";
    public static final String INSUFFICIENT_FUNDS = "Solde insuffisant. Solde actuel : %s, Montant demandé : %s";
    public static final String INVALID_AMOUNT = "Le montant doit être supérieur à zéro";
    public static final String UNAUTHORIZED_ACCESS = "Accès non autorisé à la ressource : %s";
    public static final String VALIDATION_ERROR = "Erreur de validation : %s";
}