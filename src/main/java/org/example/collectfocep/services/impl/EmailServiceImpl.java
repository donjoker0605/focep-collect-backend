package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.EmailNotification;
import org.example.collectfocep.services.interfaces.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@focep-collecte.com}")
    private String defaultFrom;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.mail.timeout:5000}")
    private int emailTimeout;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =====================================
    // MÉTHODES PRINCIPALES
    // =====================================

    @Override
    public boolean send(EmailNotification email) {
        if (!emailEnabled) {
            log.warn("📧 Email désactivé, simulation envoi à: {}", email.getDestinataire());
            return true;
        }

        if (!isValidEmail(email.getDestinataire())) {
            log.error("❌ Email invalide: {}", email.getDestinataire());
            return false;
        }

        try {
            log.info("📧 Envoi email: {} -> {}", email.getSujet(), email.getDestinataire());

            if (StringUtils.hasText(email.getContenuHtml())) {
                return sendHtmlEmail(email);
            } else {
                return sendTextEmail(email);
            }

        } catch (Exception e) {
            log.error("❌ Erreur envoi email: {}", e.getMessage(), e);
            email.incrementerTentatives();
            return false;
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendAsync(EmailNotification email) {
        try {
            boolean result = send(email);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("❌ Erreur envoi email async: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public boolean sendSimple(String destinataire, String sujet, String contenu) {
        EmailNotification email = EmailNotification.builder()
                .destinataire(destinataire)
                .sujet(sujet)
                .contenu(contenu)
                .expediteur(defaultFrom)
                .build();

        return send(email);
    }

    @Override
    public boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    // =====================================
    // TEMPLATES
    // =====================================

    @Override
    public String buildNotificationTemplate(String adminNom, String titre, String message, String details) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset='UTF-8'>");
        sb.append("<title>").append(titre).append("</title>");
        sb.append("<style>");
        sb.append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }");
        sb.append(".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        sb.append(".header { background: #d32f2f; color: white; padding: 20px; border-radius: 8px 8px 0 0; }");
        sb.append(".content { padding: 20px; }");
        sb.append(".footer { background: #f8f9fa; padding: 15px; border-radius: 0 0 8px 8px; font-size: 12px; color: #666; }");
        sb.append(".alert { background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 4px; margin: 15px 0; }");
        sb.append(".details { background: #f8f9fa; padding: 15px; border-radius: 4px; margin: 15px 0; font-family: monospace; }");
        sb.append("</style>");
        sb.append("</head>");
        sb.append("<body>");

        sb.append("<div class='container'>");

        // Header
        sb.append("<div class='header'>");
        sb.append("<h1>🚨 ").append(titre).append("</h1>");
        sb.append("<p>Notification FOCEP Collecte</p>");
        sb.append("</div>");

        // Content
        sb.append("<div class='content'>");
        sb.append("<p>Bonjour <strong>").append(adminNom).append("</strong>,</p>");

        sb.append("<div class='alert'>");
        sb.append("<p>").append(message).append("</p>");
        sb.append("</div>");

        if (StringUtils.hasText(details)) {
            sb.append("<div class='details'>");
            sb.append("<h3>Détails:</h3>");
            sb.append("<pre>").append(details).append("</pre>");
            sb.append("</div>");
        }

        sb.append("<p>Connectez-vous à votre tableau de bord pour plus d'informations.</p>");
        sb.append("</div>");

        // Footer
        sb.append("<div class='footer'>");
        sb.append("<p>FOCEP Collecte - Système de notifications automatiques</p>");
        sb.append("<p>Email envoyé le ").append(java.time.LocalDateTime.now().format(FORMATTER)).append("</p>");
        sb.append("</div>");

        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");

        return sb.toString();
    }

    // =====================================
    // MÉTHODES PRIVÉES
    // =====================================

    private boolean sendTextEmail(EmailNotification email) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(email.getExpediteur() != null ? email.getExpediteur() : defaultFrom);
            message.setTo(email.getDestinataire());
            message.setSubject(email.getSujet());
            message.setText(email.getContenu());

            if (email.getCopiesCachees() != null && !email.getCopiesCachees().isEmpty()) {
                message.setBcc(email.getCopiesCachees().toArray(new String[0]));
            }

            mailSender.send(message);
            log.info("✅ Email text envoyé avec succès");
            return true;

        } catch (Exception e) {
            log.error("❌ Erreur envoi email text: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean sendHtmlEmail(EmailNotification email) throws MessagingException { // ✅ MAINTENANT OK
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(email.getExpediteur() != null ? email.getExpediteur() : defaultFrom);
        helper.setTo(email.getDestinataire());
        helper.setSubject(email.getSujet());
        helper.setText(email.getContenu(), email.getContenuHtml());

        if (email.getCopiesCachees() != null && !email.getCopiesCachees().isEmpty()) {
            helper.setBcc(email.getCopiesCachees().toArray(new String[0]));
        }

        mailSender.send(message);
        log.info("✅ Email HTML envoyé avec succès");
        return true;
    }
}