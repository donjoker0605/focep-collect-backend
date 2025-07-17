package org.example.collectfocep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour l'Option A
 */
@Configuration
@ConfigurationProperties(prefix = "compte.option-a")
public class CompteConfig {

    /**
     * Types de comptes supportés dans l'Option A
     */
    public static final String[] TYPES_COMPTES_COLLECTEUR = {
            "SERVICE", "MANQUANT", "ATTENTE", "REMUNERATION", "CHARGE"
    };

    /**
     * Préfixes des numéros de compte pour chaque type
     */
    public static final String PREFIX_SERVICE = "373";
    public static final String PREFIX_MANQUANT = "374";
    public static final String PREFIX_ATTENTE = "ATT";
    public static final String PREFIX_REMUNERATION = "375";
    public static final String PREFIX_CHARGE = "376";

    /**
     * Vérifie si un type de compte est valide
     */
    public static boolean isValidTypeCompte(String typeCompte) {
        if (typeCompte == null) return false;

        for (String type : TYPES_COMPTES_COLLECTEUR) {
            if (type.equals(typeCompte.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtient le préfixe pour un type de compte
     */
    public static String getPrefixForType(String typeCompte) {
        if (typeCompte == null) return "UNK";

        switch (typeCompte.toUpperCase()) {
            case "SERVICE": return PREFIX_SERVICE;
            case "MANQUANT": return PREFIX_MANQUANT;
            case "ATTENTE": return PREFIX_ATTENTE;
            case "REMUNERATION": return PREFIX_REMUNERATION;
            case "CHARGE": return PREFIX_CHARGE;
            default: return "UNK";
        }
    }
}