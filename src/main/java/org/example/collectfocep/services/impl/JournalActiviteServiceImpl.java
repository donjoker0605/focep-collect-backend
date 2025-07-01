package org.example.collectfocep.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.AuditLogRequest;
import org.example.collectfocep.dto.JournalActiviteDTO;
import org.example.collectfocep.entities.JournalActivite;
import org.example.collectfocep.repositories.JournalActiviteRepository;
import org.example.collectfocep.services.interfaces.JournalActiviteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class JournalActiviteServiceImpl implements JournalActiviteService {

    private final JournalActiviteRepository journalActiviteRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void logActivity(AuditLogRequest request) {
        try {
            JournalActivite activite = JournalActivite.builder()
                    .userId(request.getUserId())
                    .userType(request.getUserType())
                    .username(request.getUsername())
                    .action(request.getAction())
                    .entityType(request.getEntityType())
                    .entityId(request.getEntityId())
                    .details(request.getDetails())
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .agenceId(request.getAgenceId())
                    .success(request.getSuccess())
                    .errorMessage(request.getErrorMessage())
                    .durationMs(request.getDurationMs())
                    .timestamp(LocalDateTime.now())
                    .build();

            journalActiviteRepository.save(activite);
            log.debug("Activité enregistrée: {} par utilisateur {}", request.getAction(), request.getUserId());

        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'activité", e);
        }
    }

    @Override
    public void logActivity(String action, String entityType, Long entityId, Object details) {
        try {
            // Récupérer les informations de contexte
            Long userId = getCurrentUserId();
            String userType = getCurrentUserType();
            String username = getCurrentUsername();
            Long agenceId = getCurrentUserAgenceId();
            String ipAddress = getCurrentIpAddress();
            String userAgent = getCurrentUserAgent();

            String detailsJson = null;
            if (details != null) {
                detailsJson = objectMapper.writeValueAsString(details);
            }

            AuditLogRequest request = AuditLogRequest.builder()
                    .userId(userId)
                    .userType(userType)
                    .username(username)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(detailsJson)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .agenceId(agenceId)
                    .success(true)
                    .build();

            logActivity(request);

        } catch (Exception e) {
            log.error("Erreur lors du log d'activité", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalActiviteDTO> getActivitesByUser(Long userId, LocalDate date, Pageable pageable) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        Page<JournalActivite> activites = journalActiviteRepository.findByUserIdAndTimestampBetween(
                userId, startOfDay, endOfDay, pageable);

        return activites.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalActiviteDTO> getActivitesByAgence(Long agenceId, LocalDate date, Pageable pageable) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        Page<JournalActivite> activites = journalActiviteRepository.findByAgenceIdAndTimestampBetween(
                agenceId, startOfDay, endOfDay, pageable);

        return activites.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalActiviteDTO> getActivitesWithFilters(Long userId, Long agenceId, String action,
                                                            String entityType, LocalDate dateDebut,
                                                            LocalDate dateFin, Pageable pageable) {
        LocalDateTime startDate = dateDebut.atStartOfDay();
        LocalDateTime endDate = dateFin.atTime(23, 59, 59);

        Page<JournalActivite> activites = journalActiviteRepository.findWithFilters(
                userId, agenceId, action, entityType, startDate, endDate, pageable);

        return activites.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getActivityStats(Long userId, LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime startDate = dateDebut.atStartOfDay();
        LocalDateTime endDate = dateFin.atTime(23, 59, 59);

        List<Object[]> stats = journalActiviteRepository.getActivityStatsByUser(userId, startDate, endDate);

        Map<String, Long> result = new HashMap<>();
        for (Object[] stat : stats) {
            result.put((String) stat[0], (Long) stat[1]);
        }

        return result;
    }

    // ===== MÉTHODES PRIVÉES =====

    private JournalActiviteDTO toDTO(JournalActivite activite) {
        return JournalActiviteDTO.builder()
                .id(activite.getId())
                .userId(activite.getUserId())
                .userType(activite.getUserType())
                .username(activite.getUsername())
                .action(activite.getAction())
                .actionDisplayName(getActionDisplayName(activite.getAction()))
                .entityType(activite.getEntityType())
                .entityId(activite.getEntityId())
                .entityDisplayName(getEntityDisplayName(activite.getEntityType(), activite.getEntityId()))
                .details(activite.getDetails())
                .ipAddress(activite.getIpAddress())
                .timestamp(activite.getTimestamp())
                .agenceId(activite.getAgenceId())
                .success(activite.getSuccess())
                .errorMessage(activite.getErrorMessage())
                .durationMs(activite.getDurationMs())
                .timeAgo(TimeUtils.getTimeAgo(activite.getTimestamp()))
                .actionIcon(getActionIcon(activite.getAction()))
                .actionColor(getActionColor(activite.getAction()))
                .build();
    }

    private String getActionDisplayName(String action) {
        Map<String, String> actionNames = Map.of(
                "CREATE_CLIENT", "Création client",
                "MODIFY_CLIENT", "Modification client",
                "DELETE_CLIENT", "Suppression client",
                "LOGIN", "Connexion",
                "LOGOUT", "Déconnexion",
                "TRANSACTION_EPARGNE", "Épargne",
                "TRANSACTION_RETRAIT", "Retrait",
                "VALIDATE_TRANSACTION", "Validation transaction"
        );
        return actionNames.getOrDefault(action, action);
    }

    private String getActionIcon(String action) {
        Map<String, String> actionIcons = Map.of(
                "CREATE_CLIENT", "person-add",
                "MODIFY_CLIENT", "create",
                "DELETE_CLIENT", "trash",
                "LOGIN", "log-in",
                "LOGOUT", "log-out",
                "TRANSACTION_EPARGNE", "arrow-down-circle",
                "TRANSACTION_RETRAIT", "arrow-up-circle"
        );
        return actionIcons.getOrDefault(action, "information-circle");
    }

    private String getActionColor(String action) {
        Map<String, String> actionColors = Map.of(
                "CREATE_CLIENT", "success",
                "MODIFY_CLIENT", "warning",
                "DELETE_CLIENT", "danger",
                "LOGIN", "primary",
                "LOGOUT", "medium",
                "TRANSACTION_EPARGNE", "success",
                "TRANSACTION_RETRAIT", "warning"
        );
        return actionColors.getOrDefault(action, "medium");
    }

    private String getEntityDisplayName(String entityType, Long entityId) {
        if (entityType == null || entityId == null) return "";
        return String.format("%s #%d", entityType, entityId);
    }

    // Méthodes utilitaires pour récupérer le contexte
    private Long getCurrentUserId() {
        // Implémentation selon votre système d'authentification
        return 1L; // TODO: Implémenter
    }

    private String getCurrentUserType() {
        // Implémentation selon votre système d'authentification
        return "COLLECTEUR"; // TODO: Implémenter
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private Long getCurrentUserAgenceId() {
        // Implémentation selon votre modèle
        return 1L; // TODO: Implémenter
    }

    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
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