package org.example.collectfocep.services;

import org.example.collectfocep.entities.AdminNotification;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildCriticalNotificationEmail(AdminNotification notification, String adminNom) {
        StringBuilder sb = new StringBuilder();

        sb.append("Bonjour ").append(adminNom).append(",\n\n");

        sb.append("🚨 ALERTE CRITIQUE - ").append(notification.getTitle()).append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("Type: ").append(notification.getType().getDescription()).append("\n");
        sb.append("Priorité: ").append(notification.getPriority().getLabel()).append("\n");
        sb.append("Date: ").append(notification.getDateCreation().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");

        sb.append("Message:\n");
        sb.append(notification.getMessage()).append("\n\n");

        if (notification.getData() != null) {
            sb.append("Détails techniques:\n");
            sb.append(notification.getData()).append("\n\n");
        }

        sb.append("Action requise: Connectez-vous immédiatement à votre tableau de bord pour traiter cette alerte.\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("FOCEP Collecte - Système de notifications automatiques\n");
        sb.append("Ne pas répondre à cet email.\n");

        return sb.toString();
    }

    public String buildDailySummaryEmail(String adminNom, int totalNotifications, int criticalCount) {
        StringBuilder sb = new StringBuilder();

        sb.append("Bonjour ").append(adminNom).append(",\n\n");

        sb.append("📊 RÉSUMÉ QUOTIDIEN DES NOTIFICATIONS\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("Total des notifications: ").append(totalNotifications).append("\n");
        sb.append("Notifications critiques: ").append(criticalCount).append("\n\n");

        if (criticalCount > 0) {
            sb.append("⚠️ Vous avez des notifications critiques en attente.\n");
            sb.append("Veuillez vous connecter pour les traiter.\n\n");
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("FOCEP Collecte\n");

        return sb.toString();
    }
}
