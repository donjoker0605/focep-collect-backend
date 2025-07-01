package org.example.collectfocep.aspects;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.collectfocep.dto.AuditLogRequest;
import org.example.collectfocep.services.interfaces.JournalActiviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LogActivityAspect {

    private final JournalActiviteService journalActiviteService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(logActivity)")
    public Object logMethodActivity(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        Object result = null;

        try {
            // Exécuter la méthode
            result = joinPoint.proceed();

        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            log.error("Erreur lors de l'exécution de {}: {}", joinPoint.getSignature().getName(), e.getMessage());
            throw e; // Re-lancer l'exception

        } finally {
            // Enregistrer l'activité dans tous les cas
            try {
                long durationMs = System.currentTimeMillis() - startTime;
                logActivity(joinPoint, logActivity, result, success, errorMessage, durationMs);
            } catch (Exception e) {
                log.error("Erreur lors de l'enregistrement de l'activité", e);
            }
        }

        return result;
    }

    private void logActivity(ProceedingJoinPoint joinPoint, LogActivity logActivity,
                             Object result, boolean success, String errorMessage, long durationMs) {
        try {
            // Récupérer les informations de contexte
            String action = logActivity.action();
            String entityType = logActivity.entityType();
            Long entityId = extractEntityId(joinPoint, result);

            // Construire les détails
            Map<String, Object> details = new HashMap<>();
            details.put("method", joinPoint.getSignature().getName());
            details.put("class", joinPoint.getTarget().getClass().getSimpleName());

            if (logActivity.includeRequestDetails()) {
                details.put("requestArgs", sanitizeArguments(joinPoint.getArgs()));
            }

            if (logActivity.includeResponseDetails() && result != null) {
                details.put("response", sanitizeResponse(result));
            }

            if (!logActivity.description().isEmpty()) {
                details.put("description", logActivity.description());
            }

            // Informations utilisateur et contexte
            Long userId = getCurrentUserId();
            String userType = getCurrentUserType();
            String username = getCurrentUsername();
            Long agenceId = getCurrentUserAgenceId();
            String ipAddress = getCurrentIpAddress();
            String userAgent = getCurrentUserAgent();

            // Créer la requête d'audit
            AuditLogRequest auditRequest = AuditLogRequest.builder()
                    .userId(userId)
                    .userType(userType)
                    .username(username)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(objectMapper.writeValueAsString(details))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .agenceId(agenceId)
                    .success(success)
                    .errorMessage(errorMessage)
                    .durationMs(durationMs)
                    .build();

            // Enregistrer l'activité
            journalActiviteService.logActivity(auditRequest);

        } catch (Exception e) {
            log.error("Erreur lors de la construction du log d'activité", e);
        }
    }

    private Long extractEntityId(ProceedingJoinPoint joinPoint, Object result) {
        // Essayer d'extraire l'ID depuis les arguments
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long) {
                return (Long) arg;
            }

            // Essayer d'extraire l'ID depuis un DTO/Entity
            try {
                if (arg != null) {
                    var idField = arg.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    Object id = idField.get(arg);
                    if (id instanceof Long) {
                        return (Long) id;
                    }
                }
            } catch (Exception ignored) {
                // Ignorer si pas de champ id
            }
        }

        // Essayer d'extraire l'ID depuis la réponse
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> response = (ResponseEntity<?>) result;
            Object body = response.getBody();
            if (body != null) {
                try {
                    var idField = body.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    Object id = idField.get(body);
                    if (id instanceof Long) {
                        return (Long) id;
                    }
                } catch (Exception ignored) {
                    // Ignorer si pas de champ id
                }
            }
        }

        return null;
    }

    private Object sanitizeArguments(Object[] args) {
        Map<String, Object> sanitized = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg != null) {
                // Ne pas logger les mots de passe ou informations sensibles
                String argString = arg.toString();
                if (argString.toLowerCase().contains("password") ||
                        argString.toLowerCase().contains("token")) {
                    sanitized.put("arg" + i, "***HIDDEN***");
                } else {
                    sanitized.put("arg" + i, arg);
                }
            }
        }
        return sanitized;
    }

    private Object sanitizeResponse(Object response) {
        if (response instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("status", responseEntity.getStatusCode().value());
            responseInfo.put("hasBody", responseEntity.getBody() != null);
            return responseInfo;
        }
        return response;
    }

    // Méthodes utilitaires pour récupérer le contexte (à adapter selon votre implémentation)
    private Long getCurrentUserId() {
        // TODO: Implémenter selon votre système d'authentification
        try {
            // Exemple: récupérer depuis le token JWT ou session
            return 1L; // Placeholder
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUserType() {
        // TODO: Implémenter selon votre système
        return "COLLECTEUR"; // Placeholder
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private Long getCurrentUserAgenceId() {
        // TODO: Implémenter selon votre modèle
        return 1L; // Placeholder
    }

    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();

            // Gérer les proxies (X-Forwarded-For, X-Real-IP)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return "unknown";
        }
    }
}