package org.example.collectfocep.constants;

public final class AppConstants {
    private AppConstants() {} // Constructeur privé pour éviter l'instanciation

    // Formats de date standard
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_FORMAT = "HH:mm:ss";

    // Tailles de pagination par défaut
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Délais et durées
    public static final int TOKEN_VALIDITY_HOURS = 24;
    public static final int PASSWORD_MIN_LENGTH = 8;
    
    // =====================================
    // TYPES DE MOUVEMENTS - AJOUTÉ POUR ÉVITER ERREURS
    // =====================================
    
    /**
     * Types de sens de mouvement utilisés en base de données
     * ATTENTION: Ne pas modifier ces valeurs sans migration de données
     */
    public static final class MouvementSens {
        public static final String EPARGNE = "epargne";
        public static final String RETRAIT = "retrait";
        public static final String DEBIT = "DEBIT";  // Pour les mouvements comptables
        public static final String CREDIT = "CREDIT";  // Pour les mouvements comptables
        public static final String VERSEMENT_NORMAL = "versement_normal";
        public static final String VERSEMENT_MANQUANT = "versement_manquant";
        public static final String AJUSTEMENT_MANQUANT = "ajustement_manquant";
    }
    
    /**
     * Types de mouvement métier
     */
    public static final class TypeMouvement {
        public static final String EPARGNE = "EPARGNE";
        public static final String RETRAIT = "RETRAIT";
    }
}
