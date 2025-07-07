package org.example.collectfocep.services;

import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ActivitySummary;
import org.example.collectfocep.entities.JournalActivite;
import org.example.collectfocep.repositories.AdminRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.JournalActiviteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminActivityCache {

    private final JournalActiviteRepository journalRepository;
    private final AdminRepository adminRepository;
    private final CollecteurRepository collecteurRepository;

    private static final String CACHE_PREFIX = "admin:activities:";
    private static final Duration CACHE_TTL = Duration.ofHours(2);

    /**
     * Cache des activités historiques (> 7 jours)
     *
     * Note: Le paramètre collecteurId représente en fait userId dans le contexte
     * de l'entité JournalActivite. Dans ce système, userId = collecteur.id quand
     * userType = "COLLECTEUR"
     */
    @Cacheable(value = "admin-activities", key = "#adminId + ':' + #collecteurId + ':' + #date")
    public List<ActivitySummary> getHistoricalActivities(Long adminId, Long collecteurId, LocalDate date) {
        log.info("📚 Chargement activités historiques depuis DB: admin={}, collecteur={}, date={}",
                adminId, collecteurId, date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // collecteurId est passé comme userId car dans l'entité JournalActivite,
        // userId stocke l'ID du collecteur quand userType = "COLLECTEUR"
        return journalRepository.findByUserIdAndTimestampBetweenAsList(collecteurId, startOfDay, endOfDay)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les activités par agence pour une période
     * Utile pour les rapports administratifs globaux
     */
    @Cacheable(value = "admin-activities-agence", key = "#adminId + ':' + #agenceId + ':' + #date")
    public List<ActivitySummary> getHistoricalActivitiesByAgence(Long adminId, Long agenceId, LocalDate date) {
        log.info("📚 Chargement activités agence depuis DB: admin={}, agence={}, date={}",
                adminId, agenceId, date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        return journalRepository.findByAgenceIdAndTimestampBetweenAsList(agenceId, startOfDay, endOfDay)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * Pré-charger le cache pour les données fréquemment consultées
     */
    @Scheduled(cron = "0 0 1 * * ?") // Tous les jours à 1h du matin
    public void preloadFrequentData() {
        log.info("🔄 Pré-chargement cache activités admin");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate lastWeek = LocalDate.now().minusDays(7);

        // ✅ AMÉLIORATION : Pré-charger à deux niveaux (collecteur et agence)
        adminRepository.findAll().forEach(admin -> {
            Long agenceId = admin.getAgence().getId();
            List<Long> collecteurIds = collecteurRepository.findIdsByAgenceId(agenceId);

            // Pré-charger par collecteur
            collecteurIds.forEach(collecteurId -> {
                for (LocalDate date = lastWeek; !date.isAfter(yesterday); date = date.plusDays(1)) {
                    getHistoricalActivities(admin.getId(), collecteurId, date);
                }
            });

            // Pré-charger par agence (pour rapports globaux)
            for (LocalDate date = lastWeek; !date.isAfter(yesterday); date = date.plusDays(1)) {
                getHistoricalActivitiesByAgence(admin.getId(), agenceId, date);
            }
        });

        log.info("✅ Cache pré-chargé avec succès");
    }

    /**
     * Convertir JournalActivite en ActivitySummary
     *
     * Maintenant plus explicite sur la correspondance userId -> collecteurId
     */
    private ActivitySummary toSummary(JournalActivite journal) {
        return ActivitySummary.builder()
                .collecteurId(journal.getUserId()) // userId représente collecteur.id quand userType="COLLECTEUR"
                .action(journal.getAction())
                .timestamp(journal.getTimestamp())
                .details(journal.getDetails())
                .entityType(journal.getEntityType())
                .entityId(journal.getEntityId())
                .success(journal.getSuccess())
                .errorMessage(journal.getErrorMessage())
                .durationMs(journal.getDurationMs())
                .username(journal.getUsername())
                .userType(journal.getUserType())
                .ipAddress(journal.getIpAddress())
                .agenceId(journal.getAgenceId())
                .build();
    }
    /**
     * Vider le cache pour une date spécifique
     * Utile pour forcer un rechargement après des modifications
     */
    public void evictCacheForDate(Long adminId, Long collecteurId, LocalDate date) {
        log.info("🗑️ Vidage cache pour admin={}, collecteur={}, date={}", adminId, collecteurId, date);
    }
}