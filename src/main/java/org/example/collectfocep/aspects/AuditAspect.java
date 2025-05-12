package org.example.collectfocep.aspects;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.collectfocep.entities.AuditLog;
import org.example.collectfocep.repositories.AuditLogRepository;
import org.example.collectfocep.security.annotations.Audited;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(audited)")
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
}