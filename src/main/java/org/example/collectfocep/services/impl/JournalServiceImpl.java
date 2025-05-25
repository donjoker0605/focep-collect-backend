package org.example.collectfocep.services.impl;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;

@Service
public class JournalServiceImpl implements JournalService {

    private final JournalRepository journalRepository;
    private final MouvementRepository mouvementRepository; // ✅ AJOUT pour saveMouvement

    @Autowired
    public JournalServiceImpl(JournalRepository journalRepository, MouvementRepository mouvementRepository) {
        this.journalRepository = journalRepository;
        this.mouvementRepository = mouvementRepository;
    }

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

        journal.setEstCloture(true);
        journal.setDateCloture(LocalDateTime.now());

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

    // MÉTHODES SUPPLÉMENTAIRES de l'ancienne classe

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

    // MÉTHODE pour la compatibilité avec Collecteur
    @Override
    public List<Journal> getJournauxByCollecteurAndDateRange(
            Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin) {
        return journalRepository.findByCollecteurAndDateDebutBetween(collecteur, dateDebut, dateFin);
    }

    @Override
    public Journal getJournalActif(Long collecteurId) {
        // Récupérer le journal actif (non clôturé) le plus récent pour le collecteur
        List<Journal> journauxActifs = journalRepository.findByCollecteurIdAndEstClotureFalseOrderByDateDebutDesc(collecteurId);

        if (journauxActifs.isEmpty()) {
            throw new ResourceNotFoundException("Aucun journal actif trouvé pour le collecteur: " + collecteurId);
        }

        return journauxActifs.get(0); // Retourner le plus récent
    }
}