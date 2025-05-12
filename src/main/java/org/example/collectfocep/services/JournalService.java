package org.example.collectfocep.services;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.repositories.JournalRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class JournalService {
    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private MouvementRepository mouvementRepository;

    public List<Journal> getJournauxByCollecteurAndDateRange(Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin) {
        return journalRepository.findByCollecteurAndDateDebutBetween(collecteur, dateDebut, dateFin);
    }

    public Optional<Journal> getJournalById(Long id) {
        return journalRepository.findById(id);
    }

    public Journal saveJournal(Journal journal) {
        return journalRepository.save(journal);
    }

    public void deleteJournal(Long id) {
        journalRepository.deleteById(id);
    }

    public List<Journal> getAllJournaux() {
        return journalRepository.findAll();
    }

    public Mouvement saveMouvement(Mouvement mouvement, Journal journal) {
        mouvement.setJournal(journal);
        return mouvementRepository.save(mouvement);
    }

    /**
     * Récupère toutes les entrées de journal pour un mois donné
     * Utilisé par le service AsyncReportService
     */
    public List<Journal> getMonthlyEntries(Long collecteurId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        return journalRepository.findByCollecteurAndDateRange(collecteurId, startDate, endDate);
    }
}