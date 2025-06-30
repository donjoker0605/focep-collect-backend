package org.example.collectfocep.aspects;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.collectfocep.entities.AuditLog;
import org.example.collectfocep.repositories.AuditLogRepository;
import org.example.collectfocep.security.annotations.Audited;
import org.example.collectfocep.services.impl.AuditService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;


    @Around("@annotation(audited)")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object logAction(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String username = "anonymous";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        // Utiliser le builder pattern de Lombok
        AuditLog.AuditLogBuilder logBuilder = AuditLog.builder()
                .username(username)
                .timestamp(LocalDateTime.now())
                .action(audited.action())
                .entityType(audited.entityType());

        // Récupérer les informations de la requête HTTP
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logBuilder
                        .ipAddress(request.getRemoteAddr())
                        .userAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.warn("Impossible de récupérer les informations de la requête", e);
        }

        // Extraire l'ID de l'entité si possible
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long) {
                logBuilder.entityId((Long) arg);
                break;
            } else if (arg != null) {
                try {
                    Object id = arg.getClass().getDeclaredMethod("getId").invoke(arg);
                    if (id instanceof Long) {
                        logBuilder.entityId((Long) id);
                        break;
                    }
                } catch (Exception e) {
                    // Ignorer si la méthode getId n'existe pas
                }
            }
        }

        AuditLog auditLog = logBuilder.build();

        try {
            Object result = joinPoint.proceed();
            auditLog.setDetails("SUCCESS");
            auditLogRepository.save(auditLog);
            return result;
        } catch (Throwable e) {
            auditLog.setDetails("ERROR: " + e.getMessage());
            auditLogRepository.save(auditLog);
            throw e;
        }
    }

    @AfterReturning(
            pointcut = "@annotation(logActivity)",
            returning = "result"
    )
    public void logActivityNew(ProceedingJoinPoint joinPoint, LogActivity logActivity, Object result) {
        try {
            // Utiliser le service d'audit amélioré
            Map<String, Object> context = new HashMap<>();

            // Ajouter les arguments de la méthode
            context.put("methodName", joinPoint.getSignature().getName());
            context.put("args", captureMethodArguments(joinPoint.getArgs()));

            // Ajouter le résultat si disponible
            if (result != null) {
                context.put("result", extractResultInfo(result));
            }

            // Extraire l'ID de l'entité
            Long entityId = extractEntityIdFromResult(result);
            if (entityId == null) {
                entityId = extractEntityId(joinPoint.getArgs());
            }

            // Utiliser la méthode enrichie du service
            auditService.logActivityWithContext(
                    logActivity.action(),
                    logActivity.entityType(),
                    entityId,
                    context
            );

        } catch (Exception e) {
            log.error("Erreur lors de l'audit via @LogActivity", e);
        }
    }

    // ===== MÉTHODES UTILITAIRES =====

    private Long extractEntityId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            } else if (arg != null) {
                try {
                    Object id = arg.getClass().getDeclaredMethod("getId").invoke(arg);
                    if (id instanceof Long) {
                        return (Long) id;
                    } else if (id instanceof Integer) {
                        return ((Integer) id).longValue();
                    }
                } catch (Exception e) {
                    // Ignorer si la méthode getId n'existe pas
                }
            }
        }
        return null;
    }

    private Long extractEntityIdFromResult(Object result) {
        if (result == null) return null;

        try {
            // Si c'est une ResponseEntity
            if (result instanceof org.springframework.http.ResponseEntity) {
                Object body = ((org.springframework.http.ResponseEntity<?>) result).getBody();
                if (body instanceof org.example.collectfocep.util.ApiResponse) {
                    Object data = ((org.example.collectfocep.util.ApiResponse<?>) body).getData();
                    return extractEntityId(new Object[]{data});
                }
            }

            // Si c'est directement un objet avec un ID
            return extractEntityId(new Object[]{result});
        } catch (Exception e) {
            log.debug("Impossible d'extraire l'ID du résultat", e);
            return null;
        }
    }

    private String captureMethodArguments(Object[] args) {
        try {
            Map<String, Object> argsMap = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    // Éviter de logger des objets trop volumineux
                    if (args[i] instanceof String || args[i] instanceof Number ||
                            args[i] instanceof Boolean) {
                        argsMap.put("arg" + i, args[i]);
                    } else {
                        argsMap.put("arg" + i, args[i].getClass().getSimpleName());
                    }
                }
            }
            return objectMapper.writeValueAsString(argsMap);
        } catch (Exception e) {
            return "Unable to capture arguments";
        }
    }

    private String buildSuccessDetails(String argsDetails, Object result) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "SUCCESS");
            details.put("args", argsDetails);

            if (result != null) {
                details.put("resultType", result.getClass().getSimpleName());

                // Si c'est une ResponseEntity, extraire le statut
                if (result instanceof org.springframework.http.ResponseEntity) {
                    details.put("httpStatus",
                            ((org.springframework.http.ResponseEntity<?>) result).getStatusCode().toString());
                }
            }

            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            return "SUCCESS | Args: " + argsDetails;
        }
    }

    private Map<String, Object> extractResultInfo(Object result) {
        Map<String, Object> info = new HashMap<>();

        try {
            if (result instanceof org.springframework.http.ResponseEntity) {
                org.springframework.http.ResponseEntity<?> response =
                        (org.springframework.http.ResponseEntity<?>) result;
                info.put("httpStatus", response.getStatusCode().value());
                info.put("hasBody", response.hasBody());
            } else if (result != null) {
                info.put("type", result.getClass().getSimpleName());
                if (result instanceof java.util.Collection) {
                    info.put("size", ((java.util.Collection<?>) result).size());
                }
            }
        } catch (Exception e) {
            log.debug("Erreur extraction info résultat", e);
        }

        return info;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "Unknown";

        // Vérifier les headers de proxy
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Prendre la première IP si plusieurs (cas des proxies chaînés)
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}