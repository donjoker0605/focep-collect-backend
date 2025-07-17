package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.VersementCollecteur;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * 🔔 Service de notification après clôture de journal
 * Informe le collecteur qu'il peut aller verser à l'agence
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationVersementService {

    // TODO: Injecter le service de notification FCM/SMS
    // private final FCMService fcmService;
    // private final SMSService smsService;

    /**
     * 📲 Notifie le collecteur après clôture réussie
     */
    public void notifierCollecteurApresClotureOK(Collecteur collecteur, VersementCollecteur versement) {
        log.info("📲 Notification clôture réussie pour collecteur: {}", collecteur.getId());

        try {
            String message = construireMessageSucces(collecteur, versement);
            String title = "✅ Journal clôturé - Autorisation de versement";

            // Notification push FCM
            // if (collecteur.getFcmToken() != null) {
            //     fcmService.sendNotification(collecteur.getFcmToken(), title, message);
            // }

            // Notification SMS de backup
            // if (collecteur.getTelephone() != null) {
            //     smsService.sendSMS(collecteur.getTelephone(), message);
            // }

            // Pour l'instant, juste log
            log.info("📲 MESSAGE: {}", message);
            log.info("🎫 TICKET AUTORISATION: {}", versement.getNumeroAutorisation());

        } catch (Exception e) {
            log.error("❌ Erreur lors de la notification: {}", e.getMessage(), e);
        }
    }

    /**
     * ⚠️ Notifie en cas de manquant important
     */
    public void notifierManquantImportant(Collecteur collecteur, VersementCollecteur versement) {
        log.warn("⚠️ Notification manquant important pour collecteur: {}", collecteur.getId());

        try {
            String message = construireMessageManquant(collecteur, versement);
            String title = "⚠️ Manquant détecté - Contact agence requis";

            // Notifier le collecteur ET l'admin
            log.warn("⚠️ MESSAGE MANQUANT: {}", message);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la notification manquant: {}", e.getMessage(), e);
        }
    }

    /**
     * 💰 Notifie en cas d'excédent
     */
    public void notifierExcedent(Collecteur collecteur, VersementCollecteur versement) {
        log.info("💰 Notification excédent pour collecteur: {}", collecteur.getId());

        try {
            String message = construireMessageExcedent(collecteur, versement);
            String title = "💰 Excédent détecté - Versement autorisé";

            log.info("💰 MESSAGE EXCÉDENT: {}", message);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la notification excédent: {}", e.getMessage(), e);
        }
    }

    /**
     * 🎫 Génère le ticket d'autorisation pour le collecteur
     */
    public String genererTicketAutorisation(VersementCollecteur versement) {
        Collecteur collecteur = versement.getCollecteur();

        StringBuilder ticket = new StringBuilder();
        ticket.append("🎫 TICKET AUTORISATION VERSEMENT\n");
        ticket.append("═══════════════════════════════════\n");
        ticket.append(String.format("📅 Date: %s\n",
                versement.getDateVersement().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        ticket.append(String.format("👤 Collecteur: %s %s\n",
                collecteur.getPrenom(), collecteur.getNom()));
        ticket.append(String.format("🏢 Agence: %s\n",
                collecteur.getAgence().getNomAgence()));
        ticket.append(String.format("💰 Montant à verser: %.0f FCFA\n",
                versement.getMontantVerse()));
        ticket.append(String.format("🔐 N° Autorisation: %s\n",
                versement.getNumeroAutorisation()));

        if (versement.hasManquant()) {
            ticket.append(String.format("⚠️ Manquant: %.0f FCFA\n", versement.getManquant()));
        }
        if (versement.hasExcedent()) {
            ticket.append(String.format("💰 Excédent: %.0f FCFA\n", versement.getExcedent()));
        }

        ticket.append("═══════════════════════════════════\n");
        ticket.append("✅ Présentez ce ticket à l'agence\n");
        ticket.append("⏰ Valable jusqu'à 17h aujourd'hui");

        return ticket.toString();
    }

    // =====================================
    // MÉTHODES PRIVÉES DE CONSTRUCTION DE MESSAGES
    // =====================================

    private String construireMessageSucces(Collecteur collecteur, VersementCollecteur versement) {
        return String.format(
                "✅ Votre journal du %s a été clôturé avec succès.\n" +
                        "💰 Montant à verser: %.0f FCFA\n" +
                        "🎫 N° Autorisation: %s\n" +
                        "🏢 Rendez-vous à l'agence %s pour effectuer le versement.\n" +
                        "⏰ Valable jusqu'à 17h aujourd'hui.",
                versement.getDateVersement().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                versement.getMontantVerse(),
                versement.getNumeroAutorisation(),
                collecteur.getAgence().getNomAgence()
        );
    }

    private String construireMessageManquant(Collecteur collecteur, VersementCollecteur versement) {
        return String.format(
                "⚠️ Journal clôturé avec MANQUANT détecté.\n" +
                        "💰 Montant collecté: %.0f FCFA\n" +
                        "💸 Montant versé: %.0f FCFA\n" +
                        "📉 Manquant: %.0f FCFA\n" +
                        "🎫 N° Autorisation: %s\n" +
                        "🏢 Contactez l'agence %s IMMÉDIATEMENT.\n" +
                        "⚠️ Justification du manquant requise.",
                versement.getMontantCollecte(),
                versement.getMontantVerse(),
                versement.getManquant(),
                versement.getNumeroAutorisation(),
                collecteur.getAgence().getNomAgence()
        );
    }

    private String construireMessageExcedent(Collecteur collecteur, VersementCollecteur versement) {
        return String.format(
                "💰 Journal clôturé avec EXCÉDENT détecté.\n" +
                        "💰 Montant collecté: %.0f FCFA\n" +
                        "💸 Montant versé: %.0f FCFA\n" +
                        "📈 Excédent: %.0f FCFA\n" +
                        "🎫 N° Autorisation: %s\n" +
                        "🏢 Rendez-vous à l'agence %s.\n" +
                        "✅ L'excédent a été crédité à votre compte.",
                versement.getMontantCollecte(),
                versement.getMontantVerse(),
                versement.getExcedent(),
                versement.getNumeroAutorisation(),
                collecteur.getAgence().getNomAgence()
        );
    }
}