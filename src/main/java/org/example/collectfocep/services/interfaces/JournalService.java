package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.dto.JournalDTO;
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

    /**
     * MÉTHODE PRINCIPALE: Récupère ou crée le journal du jour automatiquement
     */
    Journal getOrCreateJournalDuJour(Long collecteurId, LocalDate date);

    /**
     * Récupère le journal actif (aujourd'hui) pour un collecteur
     */
    Journal getJournalActif(Long collecteurId);

    /**
     * Clôture automatique du journal d'une date donnée
     */
    Journal cloturerJournalDuJour(Long collecteurId, LocalDate date);

    /**
     * NOUVELLE MÉTHODE POUR AdminCollecteurController
     */
    Page<JournalDTO> getJournauxByCollecteurPaginated(Long collecteurId, Pageable pageable);

    /**
     * NOUVELLE MÉTHODE POUR AsyncReportService
     */
    List<Journal> getMonthlyEntries(Long collecteurId, YearMonth month);

    /**
     * NOUVELLE MÉTHODE POUR ReportService
     */
    Journal createJournal(Long collecteurId);

    // MÉTHODES EXISTANTES CONSERVÉES
    List<Journal> getAllJournaux();
    Page<Journal> getAllJournaux(Pageable pageable);
    Optional<Journal> getJournalById(Long id);
    Journal saveJournal(Journal journal);
    Journal cloturerJournal(Long journalId);
    List<Journal> getJournauxByCollecteurAndDateRange(Long collecteurId, LocalDate dateDebut, LocalDate dateFin);
    Page<Journal> getJournauxByCollecteurAndDateRange(Long collecteurId, LocalDate dateDebut, LocalDate dateFin, Pageable pageable);
    void deleteJournal(Long id);
    Mouvement saveMouvement(Mouvement mouvement, Journal journal);
    List<Journal> getJournauxByCollecteurAndDateRange(Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin);
}