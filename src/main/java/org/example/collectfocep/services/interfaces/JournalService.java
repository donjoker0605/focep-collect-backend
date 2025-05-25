package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface JournalService {

    List<Journal> getAllJournaux();

    Page<Journal> getAllJournaux(Pageable pageable);

    Optional<Journal> getJournalById(Long id);

    Journal saveJournal(Journal journal);

    Journal cloturerJournal(Long journalId);

    List<Journal> getJournauxByCollecteurAndDateRange(Long collecteurId, LocalDate dateDebut, LocalDate dateFin);

    Page<Journal> getJournauxByCollecteurAndDateRange(Long collecteurId, LocalDate dateDebut, LocalDate dateFin, Pageable pageable);

    List<Journal> getMonthlyEntries(Long collecteurId, YearMonth month);

    void deleteJournal(Long id);

    Mouvement saveMouvement(Mouvement mouvement, Journal journal);

    List<Journal> getJournauxByCollecteurAndDateRange(Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin);

    /**
     * Récupère le journal actif pour un collecteur donné
     */
    Journal getJournalActif(Long collecteurId);
}