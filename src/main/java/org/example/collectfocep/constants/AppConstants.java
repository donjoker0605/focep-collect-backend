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
}
