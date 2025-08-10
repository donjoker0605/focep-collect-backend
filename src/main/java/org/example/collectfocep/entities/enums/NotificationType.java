package org.example.collectfocep.entities.enums;

public enum NotificationType {
    SOLDE_NEGATIF("Solde négatif"),
    MONTANT_ELEVE("Montant élevé"),
    INACTIVITE("Inactivité"),
    CONNEXION_ECHEC("Échec de connexion"),
    TRANSACTION_SUSPECTE("Transaction suspecte"),
    NOUVEAU_CLIENT("Nouveau client"),
    RETRAIT_IMPORTANT("Retrait important"),
    COLLECTEUR_INACTIF("Collecteur inactif"),
    TRANSACTION_CRITIQUE("Transaction critique"),
    RAPPORT_HEBDOMADAIRE("Rapport hebdomadaire");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}