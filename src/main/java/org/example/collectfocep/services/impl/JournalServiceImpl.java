package org.example.collectfocep.services.impl;

import lombok.extern.slf4j.Slf4j;
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
            log.info(" Journal existant trouvé: ID={}, Status={}", journal.getId(), journal.getStatut());
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
        LocalDate aujourdhui = LocalDate.now();
        return getOrCreateJournalDuJour(collecteurId, aujourdhui);
    }

    /**
     * CLÔTURE AUTOMATIQUE DU JOURNAL
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class
    )
    @CacheEvict(value = "journal-actuel", key = "#collecteurId")
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

    // MÉTHODES EXISTANTES CONSERVÉES POUR COMPATIBILITÉ
    @Override
    public List<Journal> getAllJournaux() {
        return journalRepository.findAll();
    }

    @Override
    public Page<Journal> getAllJournaux(Pageable pageable) {
        return journalRepository.findAll(pageable);
    }

    @Override
    @Cacheable(value = "journaux", key = "#id")
    public Optional<Journal> getJournalById(Long id) {
        return journalRepository.findById(id);
    }

    @Override
    @Transactional
    public Journal saveJournal(Journal journal) {
        return journalRepository.save(journal);
    }

    @Override
    @Transactional
    @CacheEvict(value = "journaux", key = "#journalId")
    public Journal cloturerJournal(Long journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal non trouvé"));

        journal.cloturerJournal();
        return journalRepository.save(journal);
    }

    @Override
    public List<Journal> getJournauxByCollecteurAndDateRange(
            Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {
        return journalRepository.findByCollecteurAndDateRange(collecteurId, dateDebut, dateFin);
    }

    @Override
    public Page<Journal> getJournauxByCollecteurAndDateRange(
            Long collecteurId, LocalDate dateDebut, LocalDate dateFin, Pageable pageable) {
        return journalRepository.findByCollecteurAndDateRange(collecteurId, dateDebut, dateFin, pageable);
    }

    @Override
    public List<Journal> getMonthlyEntries(Long collecteurId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return journalRepository.findByCollecteurAndDateRange(collecteurId, startDate, endDate);
    }

    @Override
    @Transactional
    public void deleteJournal(Long id) {
        journalRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Mouvement saveMouvement(Mouvement mouvement, Journal journal) {
        mouvement.setJournal(journal);
        return mouvementRepository.save(mouvement);
    }

    @Override
    public List<Journal> getJournauxByCollecteurAndDateRange(
            Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin) {
        return journalRepository.findByCollecteurAndDateDebutBetween(collecteur, dateDebut, dateFin);
    }
}