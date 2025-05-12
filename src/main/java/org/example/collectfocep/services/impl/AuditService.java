package org.example.collectfocep.services.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.example.collectfocep.entities.AuditLog;
import org.example.collectfocep.repositories.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logAction(String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);

        // Récupérer l'utilisateur actuel
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.setUsername(auth.getName());
        } else {
            log.setUsername("system");
        }

        // Récupérer les informations de la requête HTTP si disponible
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                log.setIpAddress(request.getRemoteAddr());
                log.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            // Ignorer si la requête n'est pas disponible
        }

        auditLogRepository.save(log);
    }

    public List<AuditLog> getUserAuditLogs(String username, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByUsernameAndTimestampBetween(username, start, end);
    }

    public List<AuditLog> getEntityAuditLogs(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}