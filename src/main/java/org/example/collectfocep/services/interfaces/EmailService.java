package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.EmailNotification;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
    /**
     * Envoi email synchrone
     */
    boolean send(EmailNotification email);

    /**
     * Envoi email asynchrone
     */
    CompletableFuture<Boolean> sendAsync(EmailNotification email);

    /**
     * Envoi email simple
     */
    boolean sendSimple(String destinataire, String sujet, String contenu);

    /**
     * Validation email
     */
    boolean isValidEmail(String email);

    /**
     * Template pour notifications admin
     */
    String buildNotificationTemplate(String adminNom, String titre, String message, String details);
}
