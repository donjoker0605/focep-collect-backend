package org.example.collectfocep.collectfocep.services;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.JournalRepository;
import org.example.collectfocep.services.impl.JournalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class JournalServiceTest {

    @Mock
    private JournalRepository journalRepository;

    @InjectMocks
    private JournalServiceImpl journalService;

    private Journal journal;
    private Collecteur collecteur;
    private List<Journal> journaux;

    @BeforeEach
    void setUp() {
        // Initialisation des objets pour les tests
        collecteur = new Collecteur();
        collecteur.setId(1L);
        collecteur.setNom("Nom Collecteur");
        collecteur.setPrenom("Prénom Collecteur");

        journal = new Journal();
        journal.setId(1L);
        journal.setCollecteur(collecteur);
        journal.setDateDebut(LocalDate.now().minusDays(1));
        journal.setDateFin(LocalDate.now());
        journal.setEstCloture(false);

        Journal journal2 = new Journal();
        journal2.setId(2L);
        journal2.setCollecteur(collecteur);
        journal2.setDateDebut(LocalDate.now().minusDays(2));
        journal2.setDateFin(LocalDate.now().minusDays(1));
        journal2.setEstCloture(true);
        journal2.setDateCloture(LocalDateTime.now().minusDays(1));

        journaux = Arrays.asList(journal, journal2);
    }

    @Test
    void testGetAllJournaux() {
        // Arrange
        when(journalRepository.findAll()).thenReturn(journaux);

        // Act
        List<Journal> result = journalService.getAllJournaux();

        // Assert
        assertEquals(2, result.size());
        verify(journalRepository).findAll();
    }

    @Test
    void testGetAllJournauxPaginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Journal> journauxPage = new PageImpl<>(journaux, pageable, journaux.size());
        when(journalRepository.findAll(pageable)).thenReturn(journauxPage);

        // Act
        Page<Journal> result = journalService.getAllJournaux(pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        verify(journalRepository).findAll(pageable);
    }

    @Test
    void testGetJournalById_Exists() {
        // Arrange
        when(journalRepository.findById(1L)).thenReturn(Optional.of(journal));

        // Act
        Optional<Journal> result = journalService.getJournalById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void testGetJournalById_NotExists() {
        // Arrange
        when(journalRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Journal> result = journalService.getJournalById(999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testSaveJournal() {
        // Arrange
        when(journalRepository.save(any(Journal.class))).thenReturn(journal);

        // Act
        Journal result = journalService.saveJournal(journal);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(journalRepository).save(journal);
    }

    @Test
    void testCloturerJournal() {
        // Arrange
        when(journalRepository.findById(1L)).thenReturn(Optional.of(journal));
        when(journalRepository.save(any(Journal.class))).thenReturn(journal);

        // Act
        Journal result = journalService.cloturerJournal(1L);

        // Assert
        assertTrue(result.isEstCloture());
        assertNotNull(result.getDateCloture());
        verify(journalRepository).save(journal);
    }

    @Test
    void testCloturerJournal_NotFound() {
        // Arrange
        when(journalRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> journalService.cloturerJournal(999L));
        verify(journalRepository, never()).save(any(Journal.class));
    }

    @Test
    void testGetJournauxByCollecteurAndDateRange() {
        // Arrange
        LocalDate dateDebut = LocalDate.now().minusMonths(1);
        LocalDate dateFin = LocalDate.now();
        when(journalRepository.findByCollecteurAndDateRange(
                anyLong(), eq(dateDebut), eq(dateFin))).thenReturn(journaux);

        // Act
        List<Journal> result = journalService.getJournauxByCollecteurAndDateRange(
                1L, dateDebut, dateFin);

        // Assert
        assertEquals(2, result.size());
        verify(journalRepository).findByCollecteurAndDateRange(1L, dateDebut, dateFin);
    }

    @Test
    void testGetJournauxByCollecteurAndDateRangePaginated() {
        // Arrange
        LocalDate dateDebut = LocalDate.now().minusMonths(1);
        LocalDate dateFin = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Journal> journauxPage = new PageImpl<>(journaux, pageable, journaux.size());

        when(journalRepository.findByCollecteurAndDateRange(
                anyLong(), eq(dateDebut), eq(dateFin), eq(pageable))).thenReturn(journauxPage);

        // Act
        Page<Journal> result = journalService.getJournauxByCollecteurAndDateRange(
                1L, dateDebut, dateFin, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        verify(journalRepository).findByCollecteurAndDateRange(1L, dateDebut, dateFin, pageable);
    }

    @Test
    void testGetMonthlyEntries() {
        // Arrange
        YearMonth month = YearMonth.now();
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        when(journalRepository.findByCollecteurAndDateRange(
                anyLong(), eq(startDate), eq(endDate))).thenReturn(journaux);

        // Act
        List<Journal> result = journalService.getMonthlyEntries(1L, month);

        // Assert
        assertEquals(2, result.size());
        verify(journalRepository).findByCollecteurAndDateRange(1L, startDate, endDate);
    }
}