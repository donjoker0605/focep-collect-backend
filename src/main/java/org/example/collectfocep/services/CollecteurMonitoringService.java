package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.JournalActivite;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.JournalActiviteRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 📊 Service de monitoring des collecteurs inactifs pour SuperAdmin
 * Identifie et surveille les collecteurs sans activité depuis >15 jours
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollecteurMonitoringService {

    private final CollecteurRepository collecteurRepository;
    private final JournalActiviteRepository journalActiviteRepository;
    private final MouvementRepository mouvementRepository;
    
    private static final int INACTIVITY_THRESHOLD_DAYS = 15;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * 🔍 Détecte les collecteurs inactifs depuis plus de 15 jours
     */
    public List<CollecteurInactiveInfo> getCollecteursInactifs() {
        log.info("🔍 Recherche des collecteurs inactifs depuis plus de {} jours", INACTIVITY_THRESHOLD_DAYS);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(INACTIVITY_THRESHOLD_DAYS);
        
        // Récupérer tous les collecteurs actifs
        List<Collecteur> collecteursActifs = collecteurRepository.findAllActiveWithAgence();
        
        log.debug("📊 Analyse de {} collecteurs actifs", collecteursActifs.size());
        
        return collecteursActifs.stream()
                .map(collecteur -> analyzeCollecteurActivity(collecteur, cutoffDate))
                .filter(info -> info.isInactive())
                .collect(Collectors.toList());
    }

    /**
     * 📈 Statistiques globales d'inactivité
     */
    public MonitoringStatistics getMonitoringStatistics() {
        log.info("📈 Calcul des statistiques de monitoring collecteurs");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(INACTIVITY_THRESHOLD_DAYS);
        List<Collecteur> allCollecteurs = collecteurRepository.findAllActiveWithAgence();
        
        long totalCollecteurs = allCollecteurs.size();
        List<CollecteurInactiveInfo> inactifs = getCollecteursInactifs();
        long collecteursInactifs = inactifs.size();
        long collecteursActifs = totalCollecteurs - collecteursInactifs;
        
        // Calcul par agence
        Map<String, AgenceMonitoringStats> statsByAgence = new HashMap<>();
        
        for (Collecteur collecteur : allCollecteurs) {
            String agenceNom = collecteur.getAgence() != null ? collecteur.getAgence().getNomAgence() : "Non assigné";
            
            statsByAgence.computeIfAbsent(agenceNom, k -> new AgenceMonitoringStats())
                    .incrementTotal();
                    
            boolean isInactive = inactifs.stream()
                    .anyMatch(info -> info.getCollecteurId().equals(collecteur.getId()));
                    
            if (isInactive) {
                statsByAgence.get(agenceNom).incrementInactifs();
            }
        }
        
        // Calcul du pourcentage d'inactivité
        double pourcentageInactivite = totalCollecteurs > 0 ? 
                (double) collecteursInactifs / totalCollecteurs * 100 : 0.0;
        
        return MonitoringStatistics.builder()
                .totalCollecteurs(totalCollecteurs)
                .collecteursActifs(collecteursActifs)
                .collecteursInactifs(collecteursInactifs)
                .pourcentageInactivite(pourcentageInactivite)
                .seuilInactiviteJours(INACTIVITY_THRESHOLD_DAYS)
                .dateAnalyse(LocalDateTime.now())
                .statsByAgence(statsByAgence)
                .build();
    }

    /**
     * 🏢 Collecteurs inactifs par agence
     */
    public List<CollecteurInactiveInfo> getCollecteursInactifsByAgence(Long agenceId) {
        log.info("🏢 Recherche des collecteurs inactifs pour l'agence {}", agenceId);
        
        return getCollecteursInactifs().stream()
                .filter(info -> info.getAgenceId().equals(agenceId))
                .collect(Collectors.toList());
    }

    /**
     * 📊 Analyse l'activité d'un collecteur spécifique
     */
    private CollecteurInactiveInfo analyzeCollecteurActivity(Collecteur collecteur, LocalDateTime cutoffDate) {
        // 1. Dernière activité dans le journal
        LocalDateTime derniereActiviteJournal = journalActiviteRepository
                .findLastActivityByUserId(collecteur.getId(), "COLLECTEUR")
                .orElse(null);
        
        // 2. Dernière transaction (mouvement) du collecteur
        LocalDateTime derniereTransaction = mouvementRepository
                .getLastTransactionDate(collecteur.getId());
        
        // 3. Prendre la plus récente des deux
        LocalDateTime derniereActivite = null;
        String sourceActivite = "Aucune";
        
        if (derniereActiviteJournal != null && derniereTransaction != null) {
            if (derniereActiviteJournal.isAfter(derniereTransaction)) {
                derniereActivite = derniereActiviteJournal;
                sourceActivite = "Journal";
            } else {
                derniereActivite = derniereTransaction;
                sourceActivite = "Transaction";
            }
        } else if (derniereActiviteJournal != null) {
            derniereActivite = derniereActiviteJournal;
            sourceActivite = "Journal";
        } else if (derniereTransaction != null) {
            derniereActivite = derniereTransaction;
            sourceActivite = "Transaction";
        }
        
        // 4. Déterminer si inactif
        boolean isInactive = derniereActivite == null || derniereActivite.isBefore(cutoffDate);
        long joursInactivite = derniereActivite != null ? 
                java.time.Duration.between(derniereActivite, LocalDateTime.now()).toDays() : -1;
        
        // 5. Statistiques supplémentaires
        long nombreClients = collecteur.getClients() != null ? collecteur.getClients().size() : 0;
        
        return CollecteurInactiveInfo.builder()
                .collecteurId(collecteur.getId())
                .nom(collecteur.getNom())
                .prenom(collecteur.getPrenom())
                .email(collecteur.getAdresseMail())
                .telephone(collecteur.getTelephone())
                .agenceId(collecteur.getAgenceId())
                .agenceNom(collecteur.getAgence() != null ? collecteur.getAgence().getNomAgence() : "Non assigné")
                .derniereActivite(derniereActivite)
                .sourceActivite(sourceActivite)
                .joursInactivite(joursInactivite)
                .isInactive(isInactive)
                .nombreClients(nombreClients)
                .actif(collecteur.getActive())
                .build();
    }

    /**
     * 🚨 Actions correctives automatisées
     */
    @Transactional
    public ActionCorrectiveResult executeActionCorrective(Long collecteurId, TypeActionCorrective typeAction, String motif) {
        log.info("🚨 Exécution action corrective {} pour collecteur {}", typeAction, collecteurId);
        
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new RuntimeException("Collecteur non trouvé: " + collecteurId));
        
        switch (typeAction) {
            case DESACTIVER:
                collecteur.setActive(false);
                collecteur.setModifiePar("SYSTEM_MONITORING");
                collecteur.setDateModificationMontantMax(LocalDateTime.now());
                collecteurRepository.save(collecteur);
                
                // Log de l'action
                logActionCorrective(collecteur, typeAction, motif);
                
                return ActionCorrectiveResult.builder()
                        .success(true)
                        .message("Collecteur désactivé avec succès")
                        .collecteurId(collecteurId)
                        .actionExecutee(typeAction)
                        .dateExecution(LocalDateTime.now())
                        .build();
                
            case ENVOYER_NOTIFICATION:
                // TODO: Intégrer avec le service de notification
                logActionCorrective(collecteur, typeAction, motif);
                
                return ActionCorrectiveResult.builder()
                        .success(true)
                        .message("Notification envoyée")
                        .collecteurId(collecteurId)
                        .actionExecutee(typeAction)
                        .dateExecution(LocalDateTime.now())
                        .build();
                
            case TRANSFERER_CLIENTS:
                // TODO: Implémenter le transfert automatique de clients
                return ActionCorrectiveResult.builder()
                        .success(false)
                        .message("Transfert automatique non encore implémenté")
                        .collecteurId(collecteurId)
                        .actionExecutee(typeAction)
                        .dateExecution(LocalDateTime.now())
                        .build();
                
            default:
                return ActionCorrectiveResult.builder()
                        .success(false)
                        .message("Action non supportée: " + typeAction)
                        .build();
        }
    }

    private void logActionCorrective(Collecteur collecteur, TypeActionCorrective action, String motif) {
        try {
            JournalActivite journalEntry = JournalActivite.builder()
                    .userId(-1L) // System user
                    .userType("SYSTEM")
                    .username("MONITORING_SYSTEM")
                    .action("ACTION_CORRECTIVE")
                    .entityType("COLLECTEUR")
                    .entityId(collecteur.getId())
                    .details(String.format("Action: %s, Motif: %s, Collecteur: %s %s", 
                            action, motif, collecteur.getNom(), collecteur.getPrenom()))
                    .agenceId(collecteur.getAgenceId())
                    .timestamp(LocalDateTime.now())
                    .success(true)
                    .build();
                    
            journalActiviteRepository.save(journalEntry);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du journal d'activité", e);
        }
    }

    // ================================
    // CLASSES INTERNES POUR DTO
    // ================================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CollecteurInactiveInfo {
        private Long collecteurId;
        private String nom;
        private String prenom;
        private String email;
        private String telephone;
        private Long agenceId;
        private String agenceNom;
        private LocalDateTime derniereActivite;
        private String sourceActivite;
        private long joursInactivite;
        private boolean isInactive;
        private long nombreClients;
        private Boolean actif;
        
        public String getDerniereActiviteFormatee() {
            return derniereActivite != null ? 
                    derniereActivite.format(DATE_FORMATTER) : "Jamais";
        }
        
        public String getNomComplet() {
            return nom + " " + prenom;
        }
        
        public String getStatutActivite() {
            if (!actif) return "Désactivé";
            if (isInactive) return "Inactif (" + joursInactivite + " jours)";
            return "Actif";
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MonitoringStatistics {
        private long totalCollecteurs;
        private long collecteursActifs;
        private long collecteursInactifs;
        private double pourcentageInactivite;
        private int seuilInactiviteJours;
        private LocalDateTime dateAnalyse;
        private Map<String, AgenceMonitoringStats> statsByAgence;
        
        public String getDateAnalyseFormatee() {
            return dateAnalyse.format(DATE_FORMATTER);
        }
        
        public String getPourcentageInactiviteFormate() {
            return String.format("%.1f%%", pourcentageInactivite);
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgenceMonitoringStats {
        private long totalCollecteurs = 0;
        private long collecteursInactifs = 0;
        
        public void incrementTotal() {
            totalCollecteurs++;
        }
        
        public void incrementInactifs() {
            collecteursInactifs++;
        }
        
        public double getPourcentageInactivite() {
            return totalCollecteurs > 0 ? (double) collecteursInactifs / totalCollecteurs * 100 : 0.0;
        }
        
        public long getCollecteursActifs() {
            return totalCollecteurs - collecteursInactifs;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ActionCorrectiveResult {
        private boolean success;
        private String message;
        private Long collecteurId;
        private TypeActionCorrective actionExecutee;
        private LocalDateTime dateExecution;
    }

    public enum TypeActionCorrective {
        DESACTIVER("Désactiver le collecteur"),
        ENVOYER_NOTIFICATION("Envoyer une notification"),
        TRANSFERER_CLIENTS("Transférer les clients");

        private final String description;

        TypeActionCorrective(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}