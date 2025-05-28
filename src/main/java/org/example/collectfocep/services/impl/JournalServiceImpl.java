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
     * ✅ MÉTHODE PRINCIPALE: Récupère ou crée automatiquement le journal du jour
     * Cette méthode garantit qu'il n'y a qu'un seul journal par collecteur/jour
     */
    @Override
    @Transactional
    public Journal getOrCreateJournalDuJour(Long collecteurId, LocalDate date) {
        log.info("🔍 Recherche/création journal pour collecteur {} date {}", collecteurId, date);

        // 1. Vérifier que le collecteur existe
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        // 2. Chercher un journal existant pour cette date
        Optional<Journal> journalExistant = journalRepository.findByCollecteurAndDate(collecteur, date);

        if (journalExistant.isPresent()) {
            Journal journal = journalExistant.get();
            log.info("✅ Journal existant trouvé: ID={}, Status={}", journal.getId(), journal.getStatut());
            return journal;
        }

        // 3. Créer un nouveau journal pour la date
        Journal nouveauJournal = creerJournalDuJour(collecteur, date);
        log.info("🆕 Nouveau journal créé: ID={} pour date {}", nouveauJournal.getId(), date);

        return nouveauJournal;
    }

    /**
     * ✅ CRÉATION AUTOMATIQUE DU JOURNAL
     */
    private Journal creerJournalDuJour(Collecteur collecteur, LocalDate date) {
        String reference = genererReference(collecteur, date);

        Journal journal = Journal.builder()
                .collecteur(collecteur)
                .dateDebut(date)
                .dateFin(date) // ✅ MÊME DATE pour début et fin
                .statut("OUVERT")
                .estCloture(false)
                .reference(reference)
                .build();

        return journalRepository.save(journal);
    }

    /**
     * ✅ GÉNÉRATION DE RÉFÉRENCE UNIQUE
     */
    private String genererReference(Collecteur collecteur, LocalDate date) {
        return String.format("J-%s-%s-%s",
                collecteur.getAgence().getCodeAgence(),
                collecteur.getId(),
                date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    }

    /**
     * ✅ RÉCUPÉRATION DU JOURNAL ACTUEL (aujourd'hui)
     */
    @Override
    @Cacheable(value = "journal-actuel", key = "#collecteurId")
    public Journal getJournalActif(Long collecteurId) {
        LocalDate aujourdhui = LocalDate.now();
        return getOrCreateJournalDuJour(collecteurId, aujourdhui);
    }

    /**
     * ✅ CLÔTURE AUTOMATIQUE DU JOURNAL
     */
    @Override
    @Transactional
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
        return journalRepository.save(journal);
    }

    // ✅ MÉTHODES EXISTANTES CONSERVÉES POUR COMPATIBILITÉ
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