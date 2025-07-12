package org.example.collectfocep.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.JournalDTO;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.JournalRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.services.interfaces.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class JournalServiceImpl implements JournalService {

    private final JournalRepository journalRepository;
    private final MouvementRepository mouvementRepository;
    private final CollecteurRepository collecteurRepository;

    @Autowired
    public JournalServiceImpl(JournalRepository journalRepository,
                              MouvementRepository mouvementRepository,
                              CollecteurRepository collecteurRepository) {
        this.journalRepository = journalRepository;
        this.mouvementRepository = mouvementRepository;
        this.collecteurRepository = collecteurRepository;
    }

    /**
     * MÉTHODE PRINCIPALE CORRIGÉE: Journal automatique par jour
     * Cette méthode est appelée automatiquement lors de chaque opération
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public Journal getOrCreateJournalDuJour(Long collecteurId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        log.info("🔍 Recherche/création journal automatique - Collecteur: {}, Date: {}", collecteurId, date);

        // 1. Vérifier que le collecteur existe
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        // 2. Chercher un journal existant pour cette date (avec retry en cas de concurrence)
        Optional<Journal> journalExistant = journalRepository.findByCollecteurAndDate(collecteur, date);

        if (journalExistant.isPresent()) {
            Journal journal = journalExistant.get();
            log.info("✅ Journal existant trouvé: ID={}, Status={}", journal.getId(), journal.getStatut());
            return journal;
        }

        // 3. CRÉATION SÉCURISÉE avec gestion des concurrence
        return creerJournalDuJourSecurise(collecteur, date);
    }

    /**
     * CRÉATION AUTOMATIQUE DU JOURNAL
     */
    private Journal creerJournalDuJourSecurise(Collecteur collecteur, LocalDate date) {
        try {
            // Double-check : vérifier encore une fois avant création
            Optional<Journal> existingCheck = journalRepository.findByCollecteurAndDate(collecteur, date);
            if (existingCheck.isPresent()) {
                log.info("✅ Journal créé par thread concurrent - Utilisation: {}", existingCheck.get().getId());
                return existingCheck.get();
            }

            // Créer un nouveau journal
            String reference = genererReference(collecteur, date);

            Journal nouveauJournal = Journal.builder()
                    .collecteur(collecteur)
                    .dateDebut(date)
                    .dateFin(date)
                    .statut("OUVERT")
                    .estCloture(false)
                    .reference(reference)
                    .build();

            Journal journalSauvegarde = journalRepository.save(nouveauJournal);
            log.info("🆕 Nouveau journal créé avec succès: ID={}, Référence={}",
                    journalSauvegarde.getId(), reference);

            return journalSauvegarde;

        } catch (Exception e) {
            log.error("❌ Erreur création journal pour collecteur {}: {}", collecteur.getId(), e.getMessage());

            // Dernière tentative de récupération si création échoue
            Optional<Journal> fallbackJournal = journalRepository.findByCollecteurAndDate(collecteur, date);
            if (fallbackJournal.isPresent()) {
                log.warn("⚠️ Utilisation journal existant après échec création: {}", fallbackJournal.get().getId());
                return fallbackJournal.get();
            }

            throw new RuntimeException("Impossible de créer ou récupérer le journal du jour", e);
        }
    }

    /**
     * GÉNÉRATION DE RÉFÉRENCE UNIQUE
     */
    private String genererReference(Collecteur collecteur, LocalDate date) {
        return String.format("J-%s-%s-%s",
                collecteur.getAgence() != null ? collecteur.getAgence().getCodeAgence() : "DEFAULT",
                collecteur.getId(),
                date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    }

    /**
     * RÉCUPÉRATION DU JOURNAL ACTUEL (aujourd'hui)
     */
    @Override
    @Cacheable(value = "journal-actuel", key = "#collecteurId")
    public Journal getJournalActif(Long collecteurId) {
        log.info("📋 Récupération journal actif pour collecteur: {}", collecteurId);
        LocalDate aujourdhui = LocalDate.now();
        return getOrCreateJournalDuJour(collecteurId, aujourdhui);
    }

    /**
     * ✅ MÉTHODE POUR AsyncReportService
     * Récupère les entrées mensuelles d'un journal pour un collecteur
     */
    @Override
    @Cacheable(value = "monthly-entries", key = "#collecteurId + '-' + #month")
    public List<Journal> getMonthlyEntries(Long collecteurId, YearMonth month) {
        log.info("📋 Récupération des entrées mensuelles pour collecteur: {} - mois: {}", collecteurId, month);

        try {
            LocalDate startDate = month.atDay(1);
            LocalDate endDate = month.atEndOfMonth();

            // ✅ UTILISER LA MÉTHODE EXISTANTE
            List<Journal> journals = journalRepository.findByCollecteurAndDateRange(collecteurId, startDate, endDate);

            log.info("✅ {} entrées de journal trouvées pour {}/{}",
                    journals.size(), month.getMonthValue(), month.getYear());

            return journals;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des entrées mensuelles: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur récupération entrées mensuelles: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ NOUVELLE MÉTHODE POUR ReportService
     * Créer un nouveau journal pour un collecteur (aujourd'hui par défaut)
     */
    @Override
    @Transactional
    public Journal createJournal(Long collecteurId) {
        log.info("📋 Création d'un nouveau journal pour collecteur: {}", collecteurId);

        try {
            LocalDate aujourdhui = LocalDate.now();
            return getOrCreateJournalDuJour(collecteurId, aujourdhui);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du journal pour collecteur {}: {}", collecteurId, e.getMessage(), e);
            throw new RuntimeException("Erreur création journal: " + e.getMessage(), e);
        }
    }

    /**
     * CLÔTURE AUTOMATIQUE DU JOURNAL
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class
    )
    @CacheEvict(value = {"journal-actuel", "monthly-entries"}, key = "#collecteurId")
    public Journal cloturerJournalDuJour(Long collecteurId, LocalDate date) {
        log.info("🔒 Clôture journal collecteur {} pour date {}", collecteurId, date);

        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        Journal journal = journalRepository.findByCollecteurAndDate(collecteur, date)
                .orElseThrow(() -> new ResourceNotFoundException("Journal non trouvé pour cette date"));

        if (journal.isEstCloture()) {
            log.warn("⚠️ Journal déjà clôturé: {}", journal.getId());
            return journal;
        }

        journal.cloturerJournal();
        Journal journalCloture = journalRepository.save(journal);

        log.info("✅ Journal clôturé avec succès: ID={}", journalCloture.getId());
        return journalCloture;
    }

    // =====================================
    // MÉTHODES EXISTANTES CONSERVÉES POUR COMPATIBILITÉ
    // =====================================

    @Override
    public List<Journal> getAllJournaux() {
        log.debug("📋 Récupération de tous les journaux");
        return journalRepository.findAll();
    }

    @Override
    public Page<Journal> getAllJournaux(Pageable pageable) {
        log.debug("📋 Récupération paginée de tous les journaux");
        return journalRepository.findAll(pageable);
    }

    @Override
    @Cacheable(value = "journaux", key = "#id")
    public Optional<Journal> getJournalById(Long id) {
        log.debug("📋 Récupération journal par ID: {}", id);
        return journalRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"journaux", "journal-actuel", "monthly-entries"}, allEntries = true)
    public Journal saveJournal(Journal journal) {
        log.info("💾 Sauvegarde journal: {}", journal.getId());
        return journalRepository.save(journal);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"journaux", "journal-actuel", "monthly-entries"}, key = "#journalId")
    public Journal cloturerJournal(Long journalId) {
        log.info("🔒 Clôture journal par ID: {}", journalId);

        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal non trouvé"));

        if (!journal.isEstCloture()) {
            journal.cloturerJournal();
            journal = journalRepository.save(journal);
            log.info("✅ Journal {} clôturé avec succès", journalId);
        } else {
            log.warn("⚠️ Journal {} déjà clôturé", journalId);
        }

        return journal;
    }

    @Override
    @Cacheable(value = "journal-range", key = "#collecteurId + '-' + #dateDebut + '-' + #dateFin")
    public List<Journal> getJournauxByCollecteurAndDateRange(
            Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {
        log.debug("📋 Récupération journaux collecteur {} entre {} et {}", collecteurId, dateDebut, dateFin);
        return journalRepository.findByCollecteurAndDateRange(collecteurId, dateDebut, dateFin);
    }

    @Override
    public Page<Journal> getJournauxByCollecteurAndDateRange(
            Long collecteurId, LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        log.debug("📋 Récupération paginée journaux collecteur {} entre {} et {}", collecteurId, dateDebut, dateFin);
        return journalRepository.findByCollecteurAndDateRange(collecteurId, dateDebut, dateFin, pageable);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"journaux", "journal-actuel", "monthly-entries"}, allEntries = true)
    public void deleteJournal(Long id) {
        log.info("🗑️ Suppression journal: {}", id);

        Optional<Journal> journal = journalRepository.findById(id);
        if (journal.isPresent()) {
            if (journal.get().isEstCloture()) {
                throw new IllegalStateException("Impossible de supprimer un journal clôturé");
            }
            journalRepository.deleteById(id);
            log.info("✅ Journal {} supprimé avec succès", id);
        } else {
            log.warn("⚠️ Journal {} non trouvé pour suppression", id);
        }
    }

    @Override
    @Transactional
    public Mouvement saveMouvement(Mouvement mouvement, Journal journal) {
        log.debug("💾 Sauvegarde mouvement dans journal: {}", journal.getId());

        if (journal.isEstCloture()) {
            throw new IllegalStateException("Impossible d'ajouter un mouvement à un journal clôturé");
        }

        mouvement.setJournal(journal);
        return mouvementRepository.save(mouvement);
    }

    @Override
    @Cacheable(value = "journal-collecteur-range", key = "#collecteur.id + '-' + #dateDebut + '-' + #dateFin")
    public List<Journal> getJournauxByCollecteurAndDateRange(
            Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin) {
        log.debug("📋 Récupération journaux collecteur {} entre {} et {}", collecteur.getId(), dateDebut, dateFin);
        return journalRepository.findByCollecteurAndDateDebutBetween(collecteur, dateDebut, dateFin);
    }

    /**
     * ✅ MÉTHODES UTILITAIRES POUR LE MONITORING
     */

    /**
     * Compte le nombre de journaux ouverts pour un collecteur
     */
    public long countJournauxOuverts(Long collecteurId) {
        return journalRepository.countByCollecteurIdAndEstClotureIsFalse(collecteurId);
    }

    /**
     * Récupère les journaux non clôturés depuis plus de X jours
     */
    public List<Journal> getJournauxNonCloturesAnciens(int nombreJours) {
        LocalDate seuilDate = LocalDate.now().minusDays(nombreJours);
        return journalRepository.findByEstClotureIsFalseAndDateDebutBefore(seuilDate);
    }

    /**
     * Clôture automatique des journaux anciens
     */
    @Transactional
    @CacheEvict(value = {"journal-actuel", "monthly-entries", "journaux"}, allEntries = true)
    public int cloturerJournauxAnciens(int nombreJours) {
        log.info("🔒 Clôture automatique des journaux anciens (plus de {} jours)", nombreJours);

        List<Journal> journauxAnciens = getJournauxNonCloturesAnciens(nombreJours);
        int nombreClotures = 0;

        for (Journal journal : journauxAnciens) {
            try {
                if (!journal.isEstCloture()) {
                    journal.cloturerJournal();
                    journalRepository.save(journal);
                    nombreClotures++;
                    log.info("✅ Journal {} clôturé automatiquement", journal.getId());
                }
            } catch (Exception e) {
                log.error("❌ Erreur lors de la clôture automatique du journal {}: {}",
                        journal.getId(), e.getMessage());
            }
        }

        log.info("✅ Clôture automatique terminée: {} journaux clôturés", nombreClotures);
        return nombreClotures;
    }

    @Override
    public Page<JournalDTO> getJournauxByCollecteurPaginated(Long collecteurId, Pageable pageable) {
        log.info("📋 Récupération paginée des journaux pour collecteur: {}", collecteurId);

        try {
            // Récupérer les journaux avec pagination
            Page<Journal> journauxPage = journalRepository.findByCollecteurId(collecteurId, pageable);

            // Convertir en DTO
            return journauxPage.map(journal -> JournalDTO.builder()
                    .id(journal.getId())
                    .collecteurId(journal.getCollecteur().getId())
                    .dateDebut(journal.getDateDebut())
                    .dateFin(journal.getDateFin())
                    .statut(journal.getStatut())
                    .estCloture(journal.isEstCloture())
                    .reference(journal.getReference())
                    .build());

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des journaux paginés: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur récupération journaux: " + e.getMessage(), e);
        }
    }
}