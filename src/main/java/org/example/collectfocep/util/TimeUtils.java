package org.example.collectfocep.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Calcule le temps écoulé en français
     */
    public static String getTimeAgo(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "Date inconnue";
        }

        Duration duration = Duration.between(timestamp, LocalDateTime.now());

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) {
            return "À l'instant";
        } else if (minutes < 60) {
            return "Il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        } else if (hours < 24) {
            return "Il y a " + hours + " heure" + (hours > 1 ? "s" : "");
        } else if (days < 7) {
            return "Il y a " + days + " jour" + (days > 1 ? "s" : "");
        } else {
            return timestamp.format(FORMATTER);
        }
    }

    /**
     * Formate la durée en millisecondes
     */
    public static String formatDuration(Long milliseconds) {
        if (milliseconds == null || milliseconds == 0) {
            return "0ms";
        }

        if (milliseconds < 1000) {
            return milliseconds + "ms";
        }

        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }
}