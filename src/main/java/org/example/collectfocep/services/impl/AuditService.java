package org.example.collectfocep.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.AuditLogRequest;
import org.example.collectfocep.dto.JournalActiviteDTO;
import org.example.collectfocep.entities.AuditLog;
import org.example.collectfocep.repositories.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    @Autowired
    private AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

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

    /**
     * Méthode améliorée pour logger avec plus de contexte
     */
    public void logActivityWithContext(String action, String entityType, Long entityId,
                                       Map<String, Object> context) {
        try {
            String details = objectMapper.writeValueAsString(context);
            logAction(action, entityType, entityId, details);
            log.info("Activité enregistrée: {} sur {} #{}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'activité avec contexte", e);
            // Fallback vers la méthode simple
            logAction(action, entityType, entityId, context.toString());
        }
    }

    /**
     * Logger une activité utilisateur avec détails enrichis
     */
    public void logUserActivity(String action, String entityType, Long entityId,
                                Object oldValue, Object newValue) {
        try {
            Map<String, Object> context = new HashMap<>();

            if (oldValue != null) {
                context.put("oldValue", oldValue);
            }
            if (newValue != null) {
                context.put("newValue", newValue);
            }

            // Ajouter des métadonnées supplémentaires
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() != null) {
                context.put("userRole", auth.getAuthorities().toString());
            }

            logActivityWithContext(action, entityType, entityId, context);
        } catch (Exception e) {
            log.error("Erreur lors du log d'activité utilisateur", e);
            logAction(action, entityType, entityId, "Erreur: " + e.getMessage());
        }
    }

    /**
     * Récupérer les activités par utilisateur et date (converti en DTO pour l'API)
     */
    public Page<JournalActiviteDTO> getActivitesByUser(String username, LocalDate date,
                                                       Pageable pageable) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        Page<AuditLog> logs = auditLogRepository.findByUsernameAndTimestampBetween(
                username, startOfDay, endOfDay, pageable
        );

        return logs.map(this::toJournalActiviteDTO);
    }

    /**
     * Récupérer les activités par agence et date
     */
    public Page<JournalActiviteDTO> getActivitesByAgence(Long agenceId, LocalDate date,
                                                         Pageable pageable) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // Adapter selon votre modèle - supposant que vous avez une relation avec l'agence
        Page<AuditLog> logs = auditLogRepository.findByTimestampBetween(
                startOfDay, endOfDay, pageable
        );

        // Filtrer par agence si nécessaire selon votre logique métier
        return logs.map(this::toJournalActiviteDTO);
    }

    /**
     * Obtenir les statistiques d'activité pour un utilisateur
     */
    public Map<String, Long> getUserActivityStats(String username, LocalDate startDate,
                                                  LocalDate endDate) {
        List<AuditLog> logs = getUserAuditLogs(
                username,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );

        Map<String, Long> stats = new HashMap<>();

        // Compter par type d'action
        logs.forEach(log -> {
            stats.merge(log.getAction(), 1L, Long::sum);
        });

        // Ajouter le total
        stats.put("TOTAL", (long) logs.size());

        return stats;
    }

    /**
     * Convertir AuditLog en JournalActiviteDTO pour l'API
     */
    private JournalActiviteDTO toJournalActiviteDTO(AuditLog log) {
        return JournalActiviteDTO.builder()
                .id(log.getId())
                .userId(getUserIdFromUsername(log.getUsername()))
                .userType(getUserTypeFromUsername(log.getUsername()))
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .agenceId(getAgenceIdFromContext(log))
                .actionDisplayName(getActionDisplayName(log.getAction()))
                .description(buildDescription(log))
                .build();
    }

    private String getActionDisplayName(String action) {
        switch (action) {
            case "CREATE": return "Création";
            case "UPDATE": return "Modification";
            case "DELETE": return "Suppression";
            case "LOGIN": return "Connexion";
            case "LOGOUT": return "Déconnexion";
            case "CREATE_CLIENT": return "Création client";
            case "MODIFY_CLIENT": return "Modification client";
            case "EPARGNE": return "Épargne";
            case "RETRAIT": return "Retrait";
            default: return action;
        }
    }

    /**
     * Méthode logActivity pour compatibilité avec AuditLogRequest
     */
    public void logActivity(AuditLogRequest request) {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("userId", request.getUserId());
            context.put("userType", request.getUserType());
            context.put("ipAddress", request.getIpAddress());
            context.put("userAgent", request.getUserAgent());
            context.put("agenceId", request.getAgenceId());

            logActivityWithContext(
                    request.getAction(),
                    request.getEntityType(),
                    request.getEntityId(),
                    context
            );

            log.info("Activité enregistrée: {}", request);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'activité: {}", e.getMessage());
        }
    }

    private String buildDescription(AuditLog log) {
        return String.format("%s effectuée sur %s #%d",
                getActionDisplayName(log.getAction()),
                log.getEntityType() != null ? log.getEntityType() : "système",
                log.getEntityId() != null ? log.getEntityId() : 0
        );
    }

    // Méthodes utilitaires - à adapter selon votre modèle
    private Long getUserIdFromUsername(String username) {
        // TODO: Implémenter selon votre logique
        // Par exemple, récupérer depuis UserRepository
        return null;
    }

    private String getUserTypeFromUsername(String username) {
        // TODO: Déterminer si c'est un COLLECTEUR ou ADMIN
        return "COLLECTEUR";
    }

    private Long getAgenceIdFromContext(AuditLog log) {
        // TODO: Extraire l'agenceId du contexte ou du user
        return null;
    }
}
