package org.example.collectfocep.aspects;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.example.collectfocep.dto.AuditLogRequest;
import org.example.collectfocep.security.annotations.AuditActivity;
import org.example.collectfocep.services.interfaces.JournalActiviteService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionAuditAspect {

    private final JournalActiviteService journalActiviteService;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(auditActivity)", returning = "result")
    public void logTransactionActivity(JoinPoint joinPoint, AuditActivity auditActivity, Object result) {
        try {
            log.debug("🔍 Audit automatique déclenché pour: {}", auditActivity.action());

            // Récupérer les informations d'authentification
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                log.warn("⚠️ Pas d'authentification pour l'audit de: {}", auditActivity.action());
                return;
            }

            // Construire la requête d'audit
            AuditLogRequest auditRequest = buildAuditRequest(joinPoint, auditActivity, result, auth);

            if (auditRequest != null) {
                // Enregistrer l'activité
                journalActiviteService.logActivity(auditRequest);
                log.info("✅ Activité automatiquement auditée: {}", auditActivity.action());
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'audit automatique de {}: {}",
                    auditActivity.action(), e.getMessage(), e);
        }
    }

    private AuditLogRequest buildAuditRequest(JoinPoint joinPoint, AuditActivity auditActivity,
                                              Object result, Authentication auth) {
        try {
            // Extraire les informations utilisateur
            Long userId = extractUserId(auth);
            String userType = extractUserType(auth);
            String username = auth.getName();
            Long agenceId = extractAgenceId(auth);

            // Extraire l'ID de l'entité depuis le résultat ou les arguments
            Long entityId = extractEntityId(result, joinPoint.getArgs());

            // Construire les détails de l'activité
            Map<String, Object> details = buildActivityDetails(joinPoint.getArgs(), result, auditActivity);
            String detailsJson = objectMapper.writeValueAsString(details);

            // Récupérer les informations de la requête HTTP
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();

            return AuditLogRequest.builder()
                    .userId(userId)
                    .userType(userType)
                    .username(username)
                    .action(auditActivity.action())
                    .entityType(auditActivity.entityType())
                    .entityId(entityId)
                    .details(detailsJson)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .agenceId(agenceId)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur construction requête audit: {}", e.getMessage());
            return null;
        }
    }

    private Long extractUserId(Authentication auth) {
        try {
            if (auth.getPrincipal() instanceof org.example.collectfocep.security.filters.JwtAuthenticationFilter.JwtUserPrincipal) {
                var principal = (org.example.collectfocep.security.filters.JwtAuthenticationFilter.JwtUserPrincipal) auth.getPrincipal();
                return principal.getUserId();
            }

            if (auth.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) auth.getDetails();
                Object userId = details.get("userId");
                if (userId instanceof Number) {
                    return ((Number) userId).longValue();
                }
            }

            return null;
        } catch (Exception e) {
            log.error("❌ Erreur extraction userId: {}", e.getMessage());
            return null;
        }
    }

    private String extractUserType(Authentication auth) {
        try {
            String role = auth.getAuthorities().iterator().next().getAuthority();
            if ("ROLE_COLLECTEUR".equals(role)) return "COLLECTEUR";
            if ("ROLE_ADMIN".equals(role)) return "ADMIN";
            if ("ROLE_SUPER_ADMIN".equals(role)) return "SUPER_ADMIN";
            return "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private Long extractAgenceId(Authentication auth) {
        try {
            if (auth.getPrincipal() instanceof org.example.collectfocep.security.filters.JwtAuthenticationFilter.JwtUserPrincipal) {
                var principal = (org.example.collectfocep.security.filters.JwtAuthenticationFilter.JwtUserPrincipal) auth.getPrincipal();
                return principal.getAgenceId();
            }

            if (auth.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) auth.getDetails();
                Object agenceId = details.get("agenceId");
                if (agenceId instanceof Number) {
                    return ((Number) agenceId).longValue();
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractEntityId(Object result, Object[] args) {
        try {
            // Si le résultat contient un ID
            if (result != null && hasField(result, "id")) {
                Object id = getFieldValue(result, "id");
                if (id instanceof Number) {
                    return ((Number) id).longValue();
                }
            }

            // Chercher dans les arguments (souvent les DTOs de requête)
            for (Object arg : args) {
                if (arg != null && hasField(arg, "clientId")) {
                    Object id = getFieldValue(arg, "clientId");
                    if (id instanceof Number) {
                        return ((Number) id).longValue();
                    }
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildActivityDetails(Object[] args, Object result, AuditActivity auditActivity) {
        Map<String, Object> details = new HashMap<>();

        try {
            details.put("action", auditActivity.action());
            details.put("entityType", auditActivity.entityType());
            details.put("timestamp", LocalDateTime.now());

            // Extraire les informations des arguments
            for (Object arg : args) {
                if (arg != null) {
                    extractArgumentDetails(arg, details);
                }
            }

            // Extraire les informations du résultat
            if (result != null) {
                extractResultDetails(result, details);
            }

        } catch (Exception e) {
            log.warn("⚠️ Erreur extraction détails: {}", e.getMessage());
        }

        return details;
    }

    private void extractArgumentDetails(Object arg, Map<String, Object> details) {
        try {
            if (hasField(arg, "montant")) {
                details.put("montant", getFieldValue(arg, "montant"));
            }
            if (hasField(arg, "clientId")) {
                details.put("clientId", getFieldValue(arg, "clientId"));
            }
            if (hasField(arg, "collecteurId")) {
                details.put("collecteurId", getFieldValue(arg, "collecteurId"));
            }
        } catch (Exception e) {
            // Ignorer les erreurs d'extraction
        }
    }

    private void extractResultDetails(Object result, Map<String, Object> details) {
        try {
            if (hasField(result, "id")) {
                details.put("resultId", getFieldValue(result, "id"));
            }
        } catch (Exception e) {
            // Ignorer les erreurs d'extraction
        }
    }

    private boolean hasField(Object obj, String fieldName) {
        try {
            obj.getClass().getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignorer
        }
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            // Ignorer
        }
        return "unknown";
    }
}