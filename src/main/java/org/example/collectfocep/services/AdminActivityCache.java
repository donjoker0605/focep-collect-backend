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
     */
    @Cacheable(value = "admin-activities", key = "#adminId + ':' + #collecteurId + ':' + #date") // ✅ MAINTENANT OK
    public List<ActivitySummary> getHistoricalActivities(Long adminId, Long collecteurId, LocalDate date) {
        log.info("📚 Chargement activités historiques depuis DB: admin={}, collecteur={}, date={}",
                adminId, collecteurId, date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        return journalRepository.findByCollecteurIdAndTimestampBetween(collecteurId, startOfDay, endOfDay)
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

        // Pré-charger les données de la semaine dernière pour tous les admins
        adminRepository.findAll().forEach(admin -> {
            Long agenceId = admin.getAgence().getId();
            List<Long> collecteurIds = collecteurRepository.findIdsByAgenceId(agenceId);

            collecteurIds.forEach(collecteurId -> {
                for (LocalDate date = lastWeek; !date.isAfter(yesterday); date = date.plusDays(1)) {
                    getHistoricalActivities(admin.getId(), collecteurId, date);
                }
            });
        });

        log.info("✅ Cache pré-chargé avec succès");
    }

    /**
     * Convertir JournalActivite en ActivitySummary
     */
    private ActivitySummary toSummary(JournalActivite journal) {
        return ActivitySummary.builder()
                .collecteurId(journal.getUserId())
                .action(journal.getAction())
                .timestamp(journal.getTimestamp())
                .details(journal.getDetails())
                .build();
    }
}