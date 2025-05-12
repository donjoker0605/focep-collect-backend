package org.example.collectfocep.services.impl;

import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.JournalRepository;
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

    @Autowired
    public JournalServiceImpl(JournalRepository journalRepository) {
        this.journalRepository = journalRepository;
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

    public List<Journal> getMonthlyEntries(long collecteurId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return journalRepository.findByCollecteurAndDateRange(collecteurId, startDate, endDate);
    }
}