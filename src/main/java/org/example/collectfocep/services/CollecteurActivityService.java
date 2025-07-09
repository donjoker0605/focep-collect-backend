package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CollecteurActivitySummaryDTO;
import org.example.collectfocep.dto.JournalActiviteDTO;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.JournalActivite;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.JournalActiviteRepository;
import org.example.collectfocep.security.config.RoleConfig;
import org.example.collectfocep.security.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎯 Service pour la supervision des activités des collecteurs par les admins
 *
 * FONCTIONNALITÉS :
 * 1. Résumé des activités de tous les collecteurs accessibles
 * 2. Statistiques détaillées par collecteur
 * 3. Détection d'activités critiques/suspectes
 * 4. Cache optimisé pour les performances
 *
 * SÉCURITÉ :
 * - Respect des permissions par agence pour les ADMIN
 * - Accès global pour les SUPER_ADMIN
 * - Isolation stricte des données
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollecteurActivityService {

    private final CollecteurRepository collecteurRepository;
    private final JournalActiviteRepository journalActiviteRepository;
    private final SecurityService securityService;

    // Configuration pour la détection d'activités critiques
    private static final int SEUIL_ACTIVITES_SUSPECTES = 50; // Activités/jour
    private static final int SEUIL_INACTIVITE_HEURES = 24;
    private static final List<String> ACTIONS_CRITIQUES = Arrays.asList(
            "DELETE_CLIENT", "MODIFY_SOLDE", "TRANSFER_COMPTE", "LOGIN_FAILED"
    );

    // =====================================
    // MÉTHODES PRINCIPALES
    // =====================================

    /**
     * 📊 Résumé des activités de tous les collecteurs accessibles à l'admin
     *
     * @param authentication Context de sécurité pour déterminer les permissions
     * @param dateDebut Date de début d'analyse
     * @param dateFin Date de fin d'analyse
     * @return Liste des résumés d'activité par collecteur
     */
    @Cacheable(value = "collecteursActivitySummary", key = "#authentication.name + '_' + #dateDebut + '_' + #dateFin")
    public List<CollecteurActivitySummaryDTO> getCollecteursActivitySummary(
            Authentication authentication, LocalDate dateDebut, LocalDate dateFin) {

        log.debug("📈 Génération résumé activités collecteurs du {} au {} pour {}",
                dateDebut, dateFin, authentication.getName());

        try {
            // 🔍 Récupération des collecteurs accessibles selon les permissions
            List<Collecteur> collecteursAccessibles = getAccessibleCollecteurs(authentication);

            log.debug("👥 {} collecteurs accessibles trouvés", collecteursAccessibles.size());

            // 📊 Génération du résumé pour chaque collecteur
            List<CollecteurActivitySummaryDTO> summaries = collecteursAccessibles.parallelStream()
                    .map(collecteur -> generateCollecteurSummary(collecteur, dateDebut, dateFin))
                    .sorted(Comparator.comparing(CollecteurActivitySummaryDTO::getCollecteurNom))
                    .collect(Collectors.toList());

            log.info("✅ Résumé généré pour {} collecteurs", summaries.size());
            return summaries;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé d'activités: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la génération du résumé d'activités", e);
        }
    }

    /**
     * 📈 Statistiques détaillées pour un collecteur spécifique
     *
     * @param collecteurId ID du collecteur
     * @param dateDebut Date de début d'analyse
     * @param dateFin Date de fin d'analyse
     * @return Map contenant toutes les statistiques détaillées
     */
    @Cacheable(value = "collecteurDetailedStats", key = "#collecteurId + '_' + #dateDebut + '_' + #dateFin")
    public Map<String, Object> getCollecteurDetailedStats(Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {

        log.debug("📊 Génération stats détaillées collecteur {} du {} au {}", collecteurId, dateDebut, dateFin);

        try {
            LocalDateTime startDateTime = dateDebut.atStartOfDay();
            LocalDateTime endDateTime = dateFin.atTime(23, 59, 59);

            // 📋 Récupération de toutes les activités sur la période
            List<JournalActivite> activites = journalActiviteRepository
                    .findByUserIdAndTimestampBetween(collecteurId, startDateTime, endDateTime, Pageable.unpaged())
                    .getContent();

            // 📊 Calcul des statistiques
            Map<String, Object> stats = new HashMap<>();

            // Statistiques générales
            stats.put("totalActivites", activites.size());
            stats.put("activitesParJour", calculateActivitesParJour(activites, dateDebut, dateFin));
            stats.put("joursActifs", calculateJoursActifs(activites, dateDebut, dateFin));

            // Répartition par type d'action
            stats.put("repartitionActions", calculateRepartitionActions(activites));

            // Répartition par type d'entité
            stats.put("repartitionEntites", calculateRepartitionEntites(activites));

            // Analyse des heures d'activité
            stats.put("heuresActivites", calculateHeuresActivites(activites));

            // Détection d'anomalies
            stats.put("activitesSuspectes", detectActivitesSuspectes(activites));

            // Tendances
            stats.put("tendanceActivite", calculateTendanceActivite(activites, dateDebut, dateFin));

            // Métriques de performance
            stats.put("tempsReponse", calculateMetriquesPerformance(activites));

            log.info("✅ Stats détaillées générées pour collecteur {} ({} activités analysées)",
                    collecteurId, activites.size());

            return stats;

        } catch (Exception e) {
            log.error("❌ Erreur lors du calcul des stats détaillées pour collecteur {}: {}",
                    collecteurId, e.getMessage(), e);
            throw new RuntimeException("Erreur lors du calcul des statistiques détaillées", e);
        }
    }

    /**
     * 🚨 Détection d'activités critiques pour un collecteur
     *
     * @param collecteurId ID du collecteur
     * @param dateDebut Date de début d'analyse
     * @param dateFin Date de fin d'analyse
     * @param limit Nombre maximum d'activités à retourner
     * @return Liste des activités critiques détectées
     */
    public List<JournalActiviteDTO> getCriticalActivities(Long collecteurId, LocalDate dateDebut,
                                                          LocalDate dateFin, int limit) {

        log.debug("🚨 Détection activités critiques collecteur {} du {} au {}", collecteurId, dateDebut, dateFin);

        try {
            LocalDateTime startDateTime = dateDebut.atStartOfDay();
            LocalDateTime endDateTime = dateFin.atTime(23, 59, 59);

            // 🔍 Récupération des activités critiques
            List<JournalActivite> activites = journalActiviteRepository
                    .findByUserIdAndTimestampBetweenAsList(collecteurId, startDateTime, endDateTime);

            // 🚨 Filtrage des activités critiques
            List<JournalActivite> criticalActivities = activites.stream()
                    .filter(this::isActivityCritical)
                    .sorted(Comparator.comparing(JournalActivite::getTimestamp).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            // 📋 Conversion en DTO
            List<JournalActiviteDTO> result = criticalActivities.stream()
                    .map(this::toJournalActiviteDTO)
                    .collect(Collectors.toList());

            log.info("⚠️ {} activités critiques détectées pour collecteur {} sur {} activités totales",
                    result.size(), collecteurId, activites.size());

            return result;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la détection d'activités critiques pour collecteur {}: {}",
                    collecteurId, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la détection d'activités critiques", e);
        }
    }

    // =====================================
    // MÉTHODES PRIVÉES - LOGIQUE MÉTIER
    // =====================================

    /**
     * 👥 Récupère la liste des collecteurs accessibles selon les permissions de l'utilisateur
     */
    private List<Collecteur> getAccessibleCollecteurs(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // SUPER_ADMIN peut voir tous les collecteurs
        if (securityService.hasRole(authorities, RoleConfig.SUPER_ADMIN)) {
            log.debug("🔑 Super Admin - Accès à tous les collecteurs");
            return collecteurRepository.findAllWithAgence();
        }

        // ADMIN ne peut voir que les collecteurs de son agence
        if (securityService.hasRole(authorities, RoleConfig.ADMIN)) {
            Long agenceId = securityService.getCurrentUserAgenceId(authentication);
            if (agenceId != null) {
                log.debug("🏢 Admin agence {} - Accès aux collecteurs de l'agence", agenceId);
                return collecteurRepository.findByAgenceIdWithAgence(agenceId);
            }
        }

        log.warn("❌ Aucun collecteur accessible pour l'utilisateur {}", authentication.getName());
        return Collections.emptyList();
    }

    /**
     * 📊 Génère le résumé d'activité pour un collecteur
     */
    private CollecteurActivitySummaryDTO generateCollecteurSummary(Collecteur collecteur,
                                                                   LocalDate dateDebut, LocalDate dateFin) {
        try {
            LocalDateTime startDateTime = dateDebut.atStartOfDay();
            LocalDateTime endDateTime = dateFin.atTime(23, 59, 59);

            // 📋 Récupération des activités
            List<JournalActivite> activites = journalActiviteRepository
                    .findByUserIdAndTimestampBetweenAsList(collecteur.getId(), startDateTime, endDateTime);

            // 📊 Calculs de base
            int totalActivites = activites.size();
            long joursActifs = activites.stream()
                    .map(a -> a.getTimestamp().toLocalDate())
                    .distinct()
                    .count();

            // 🚨 Détection d'activités critiques
            long activitesCritiques = activites.stream()
                    .filter(this::isActivityCritical)
                    .count();

            // ⏰ Dernière activité
            Optional<LocalDateTime> derniereActivite = activites.stream()
                    .map(JournalActivite::getTimestamp)
                    .max(LocalDateTime::compareTo);

            // 🎯 Statut du collecteur
            String statut = determineCollecteurStatus(activites, derniereActivite);

            return CollecteurActivitySummaryDTO.builder()
                    .collecteurId(collecteur.getId())
                    .collecteurNom(collecteur.getNom() + " " + collecteur.getPrenom())
                    .agenceNom(collecteur.getAgence().getNom())
                    .totalActivites(totalActivites)
                    .joursActifs((int) joursActifs)
                    .activitesCritiques((int) activitesCritiques)
                    .derniereActivite(derniereActivite.orElse(null))
                    .statut(statut)
                    .couleurStatut(getStatusColor(statut))
                    .iconeStatut(getStatusIcon(statut))
                    .scoreActivite(calculateScoreActivite(totalActivites, joursActifs, activitesCritiques))
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du résumé pour collecteur {}: {}",
                    collecteur.getId(), e.getMessage(), e);

            // Retour d'un résumé par défaut en cas d'erreur
            return CollecteurActivitySummaryDTO.builder()
                    .collecteurId(collecteur.getId())
                    .collecteurNom(collecteur.getNom() + " " + collecteur.getPrenom())
                    .agenceNom(collecteur.getAgence().getNom())
                    .statut("ERREUR")
                    .couleurStatut("#FF0000")
                    .iconeStatut("⚠️")
                    .build();
        }
    }

    /**
     * 🚨 Détermine si une activité est critique
     */
    private boolean isActivityCritical(JournalActivite activite) {
        // Actions considérées comme critiques
        if (ACTIONS_CRITIQUES.contains(activite.getAction())) {
            return true;
        }

        // Échecs de connexion
        if (activite.getSuccess() != null && !activite.getSuccess()) {
            return true;
        }

        // Activités en dehors des heures normales (avant 6h ou après 22h)
        int heure = activite.getTimestamp().getHour();
        if (heure < 6 || heure > 22) {
            return true;
        }

        // Durée d'exécution anormalement longue (> 10 secondes)
        if (activite.getDurationMs() != null && activite.getDurationMs() > 10000) {
            return true;
        }

        return false;
    }

    /**
     * 🎯 Détermine le statut d'un collecteur basé sur ses activités
     */
    private String determineCollecteurStatus(List<JournalActivite> activites,
                                             Optional<LocalDateTime> derniereActivite) {

        // Aucune activité
        if (activites.isEmpty()) {
            return "INACTIF";
        }

        // Dernière activité trop ancienne
        if (derniereActivite.isPresent()) {
            long heuresDepuisDerniereActivite = java.time.Duration
                    .between(derniereActivite.get(), LocalDateTime.now())
                    .toHours();

            if (heuresDepuisDerniereActivite > SEUIL_INACTIVITE_HEURES) {
                return "INACTIF";
            }
        }

        // Trop d'activités critiques (> 10% du total)
        long critiques = activites.stream().filter(this::isActivityCritical).count();
        if (activites.size() > 0 && (critiques * 100.0 / activites.size()) > 10) {
            return "ATTENTION";
        }

        // Activité normale
        return "ACTIF";
    }

    /**
     * 🎨 Retourne la couleur associée à un statut
     */
    private String getStatusColor(String statut) {
        switch (statut) {
            case "ACTIF": return "#4CAF50";    // Vert
            case "ATTENTION": return "#FF9800"; // Orange
            case "INACTIF": return "#F44336";   // Rouge
            default: return "#9E9E9E";          // Gris
        }
    }

    /**
     * 🔖 Retourne l'icône associée à un statut
     */
    private String getStatusIcon(String statut) {
        switch (statut) {
            case "ACTIF": return "✅";
            case "ATTENTION": return "⚠️";
            case "INACTIF": return "🔴";
            default: return "❓";
        }
    }

    /**
     * 📊 Calcule un score d'activité (0-100)
     */
    private int calculateScoreActivite(int totalActivites, long joursActifs, long activitesCritiques) {
        int score = 50; // Score de base

        // Bonus pour activité
        score += Math.min(totalActivites / 2, 30); // Max +30 points
        score += Math.min(joursActifs * 5, 20);    // Max +20 points

        // Malus pour activités critiques
        score -= Math.min(activitesCritiques * 10, 40); // Max -40 points

        return Math.max(0, Math.min(100, score));
    }

    // =====================================
    // MÉTHODES DE CALCUL STATISTIQUES
    // =====================================

    private Map<LocalDate, Integer> calculateActivitesParJour(List<JournalActivite> activites,
                                                              LocalDate dateDebut, LocalDate dateFin) {
        return activites.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getTimestamp().toLocalDate(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    private int calculateJoursActifs(List<JournalActivite> activites, LocalDate dateDebut, LocalDate dateFin) {
        return (int) activites.stream()
                .map(a -> a.getTimestamp().toLocalDate())
                .distinct()
                .count();
    }

    private Map<String, Integer> calculateRepartitionActions(List<JournalActivite> activites) {
        return activites.stream()
                .collect(Collectors.groupingBy(
                        JournalActivite::getAction,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    private Map<String, Integer> calculateRepartitionEntites(List<JournalActivite> activites) {
        return activites.stream()
                .filter(a -> a.getEntityType() != null)
                .collect(Collectors.groupingBy(
                        JournalActivite::getEntityType,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    private Map<Integer, Integer> calculateHeuresActivites(List<JournalActivite> activites) {
        return activites.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getTimestamp().getHour(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    private List<String> detectActivitesSuspectes(List<JournalActivite> activites) {
        List<String> anomalies = new ArrayList<>();

        // Trop d'activités en une journée
        Map<LocalDate, Long> activitesParJour = activites.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getTimestamp().toLocalDate(),
                        Collectors.counting()
                ));

        activitesParJour.forEach((date, count) -> {
            if (count > SEUIL_ACTIVITES_SUSPECTES) {
                anomalies.add("Activité excessive le " + date + " (" + count + " activités)");
            }
        });

        // Activités en dehors des heures normales
        long activitesNocturnes = activites.stream()
                .filter(a -> a.getTimestamp().getHour() < 6 || a.getTimestamp().getHour() > 22)
                .count();

        if (activitesNocturnes > 0) {
            anomalies.add(activitesNocturnes + " activité(s) en dehors des heures normales");
        }

        return anomalies;
    }

    private String calculateTendanceActivite(List<JournalActivite> activites, LocalDate dateDebut, LocalDate dateFin) {
        if (activites.size() < 2) return "STABLE";

        // Comparaison première vs dernière moitié de la période
        LocalDate milieu = dateDebut.plusDays(java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin) / 2);

        long premiereMoitie = activites.stream()
                .filter(a -> a.getTimestamp().toLocalDate().isBefore(milieu))
                .count();

        long deuxiemeMoitie = activites.stream()
                .filter(a -> a.getTimestamp().toLocalDate().isAfter(milieu))
                .count();

        if (deuxiemeMoitie > premiereMoitie * 1.2) return "CROISSANTE";
        if (deuxiemeMoitie < premiereMoitie * 0.8) return "DÉCROISSANTE";
        return "STABLE";
    }

    private Map<String, Object> calculateMetriquesPerformance(List<JournalActivite> activites) {
        Map<String, Object> metriques = new HashMap<>();

        List<Long> durations = activites.stream()
                .filter(a -> a.getDurationMs() != null)
                .map(JournalActivite::getDurationMs)
                .collect(Collectors.toList());

        if (!durations.isEmpty()) {
            metriques.put("tempsReponseMin", Collections.min(durations));
            metriques.put("tempsReponseMax", Collections.max(durations));
            metriques.put("tempsReponseMoyen", durations.stream().mapToLong(Long::longValue).average().orElse(0));
        }

        return metriques;
    }

    /**
     * 📋 Conversion JournalActivite vers DTO
     */
    private JournalActiviteDTO toJournalActiviteDTO(JournalActivite activite) {
        return JournalActiviteDTO.builder()
                .id(activite.getId())
                .userId(activite.getUserId())
                .userType(activite.getUserType())
                .username(activite.getUsername())
                .action(activite.getAction())
                .entityType(activite.getEntityType())
                .entityId(activite.getEntityId())
                .details(activite.getDetails())
                .ipAddress(activite.getIpAddress())
                .timestamp(activite.getTimestamp())
                .agenceId(activite.getAgenceId())
                .success(activite.getSuccess())
                .errorMessage(activite.getErrorMessage())
                .durationMs(activite.getDurationMs())
                .build();
    }
}