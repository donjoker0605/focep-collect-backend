package org.example.collectfocep.aspects;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.example.collectfocep.dto.AuditLogRequest;
import org.example.collectfocep.services.interfaces.JournalActiviteService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.example.collectfocep.security.filters.JwtAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class UnifiedAuditAspect {

    private final JournalActiviteService journalActiviteService;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "@annotation(logActivity)", returning = "result")
    public void auditActivity(JoinPoint joinPoint, LogActivity logActivity, Object result) {
        try {
            log.debug("🔍 Audit automatique: {}", logActivity.action());

            // Récupérer l'authentification
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                log.warn("⚠️ Pas d'authentification pour audit: {}", logActivity.action());
                return;
            }

            // Construire la requête d'audit
            AuditLogRequest auditRequest = buildAuditRequest(joinPoint, logActivity, result, auth);

            if (auditRequest != null) {
                // IMPORTANT: Utiliser JournalActiviteService, pas AuditService
                journalActiviteService.logActivity(auditRequest);
                log.info("✅ Audit automatique réussi: {}", logActivity.action());
            }

        } catch (Exception e) {
            log.error("❌ Erreur audit automatique {}: {}", logActivity.action(), e.getMessage());
        }
    }

    private AuditLogRequest buildAuditRequest(JoinPoint joinPoint, LogActivity logActivity,
                                              Object result, Authentication auth) {
        try {
            // Extraire les informations utilisateur depuis JWT
            Long userId = extractUserId(auth);
            String userType = extractUserType(auth);
            String username = auth.getName();
            Long agenceId = extractAgenceId(auth);

            // Extraire l'ID de l'entité
            Long entityId = extractEntityId(result, joinPoint.getArgs());

            // Construire les détails
            Map<String, Object> details = buildActivityDetails(joinPoint, result, logActivity);
            String detailsJson = objectMapper.writeValueAsString(details);

            // Informations de requête HTTP
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();

            return AuditLogRequest.builder()
                    .userId(userId)
                    .userType(userType)
                    .username(username)
                    .action(logActivity.action())
                    .entityType(logActivity.entityType())
                    .entityId(entityId)
                    .details(detailsJson)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .agenceId(agenceId)
                    .success(isSuccessfulResult(result))
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur construction audit: {}", e.getMessage());
            return null;
        }
    }

    // ===== EXTRACTION DES INFORMATIONS JWT =====

    private Long extractUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof JwtAuthenticationFilter.JwtUserPrincipal) {
            return ((JwtAuthenticationFilter.JwtUserPrincipal) auth.getPrincipal()).getUserId();
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
    }

    private String extractUserType(Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        if ("ROLE_COLLECTEUR".equals(role)) return "COLLECTEUR";
        if ("ROLE_ADMIN".equals(role)) return "ADMIN";
        if ("ROLE_SUPER_ADMIN".equals(role)) return "SUPER_ADMIN";
        return "UNKNOWN";
    }

    private Long extractAgenceId(Authentication auth) {
        if (auth.getPrincipal() instanceof JwtAuthenticationFilter.JwtUserPrincipal) {
            return ((JwtAuthenticationFilter.JwtUserPrincipal) auth.getPrincipal()).getAgenceId();
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
    }

    // ===== EXTRACTION DES DONNÉES =====

    private Long extractEntityId(Object result, Object[] args) {
        // 1. Essayer d'extraire depuis le résultat
        Long entityId = extractIdFromResult(result);
        if (entityId != null) return entityId;

        // 2. Essayer d'extraire depuis les arguments
        return extractIdFromArgs(args);
    }

    private Long extractIdFromResult(Object result) {
        try {
            if (result instanceof org.springframework.http.ResponseEntity) {
                Object body = ((org.springframework.http.ResponseEntity<?>) result).getBody();
                if (body instanceof org.example.collectfocep.util.ApiResponse) {
                    Object data = ((org.example.collectfocep.util.ApiResponse<?>) body).getData();
                    return getIdFromObject(data);
                }
            }
            return getIdFromObject(result);
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractIdFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
            Long id = getIdFromObject(arg);
            if (id != null) return id;
        }
        return null;
    }

    private Long getIdFromObject(Object obj) {
        if (obj == null) return null;

        try {
            // Essayer getId()
            Object id = obj.getClass().getMethod("getId").invoke(obj);
            if (id instanceof Number) {
                return ((Number) id).longValue();
            }
        } catch (Exception e) {
            // Ignorer
        }

        return null;
    }

    private Map<String, Object> buildActivityDetails(JoinPoint joinPoint, Object result, LogActivity logActivity) {
        Map<String, Object> details = new HashMap<>();

        details.put("action", logActivity.action());
        details.put("entityType", logActivity.entityType());
        details.put("timestamp", LocalDateTime.now());
        details.put("method", joinPoint.getSignature().getName());

        // Extraire des informations des arguments (montant, clientId, etc.)
        extractArgumentDetails(joinPoint.getArgs(), details);

        return details;
    }

    private void extractArgumentDetails(Object[] args, Map<String, Object> details) {
        for (Object arg : args) {
            if (arg == null) continue;

            // Extraire des champs spécifiques des DTOs
            extractFieldIfExists(arg, "montant", details);
            extractFieldIfExists(arg, "clientId", details);
            extractFieldIfExists(arg, "collecteurId", details);
        }
    }

    private void extractFieldIfExists(Object obj, String fieldName, Map<String, Object> details) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            if (value != null) {
                details.put(fieldName, value);
            }
        } catch (Exception e) {
            // Ignorer si le champ n'existe pas
        }
    }

    private boolean isSuccessfulResult(Object result) {
        if (result instanceof org.springframework.http.ResponseEntity) {
            var response = (org.springframework.http.ResponseEntity<?>) result;
            return response.getStatusCode().is2xxSuccessful();
        }
        return true; // Par défaut, considérer comme succès
    }

    // ===== INFORMATIONS HTTP =====

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return getIpFromRequest(request);
            }
        } catch (Exception e) {
            // Ignorer
        }
        return "unknown";
    }

    private String getIpFromRequest(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
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