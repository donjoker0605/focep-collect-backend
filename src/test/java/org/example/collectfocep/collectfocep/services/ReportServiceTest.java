package org.example.collectfocep.collectfocep.services;

import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.services.ReportGenerationService;
import org.example.collectfocep.services.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private ReportGenerationService reportGenerationService;

    @Mock
    private CollecteurRepository collecteurRepository;

    @InjectMocks
    private ReportService reportService;

    private CommissionResult commissionResult;
    private Collecteur collecteur;

    @BeforeEach
    void setUp() {
        collecteur = new Collecteur();
        collecteur.setId(1L);
        collecteur.setNom("Nom Collecteur");
        collecteur.setPrenom("Prénom Collecteur");

        commissionResult = new CommissionResult();
        commissionResult.setCollecteurId(1L);
        commissionResult.setMontantCommission(100.0);
        commissionResult.setMontantTVA(19.25);
        commissionResult.setMontantNet(80.75);
    }

    @Test
    void testGenerateCommissionReport() {
        // Arrange
        byte[] reportBytes = "Rapport test".getBytes();
        when(reportGenerationService.generateMonthlyReport(anyLong(), anyInt(), anyInt()))
                .thenReturn(reportBytes);

        // Act
        byte[] result = reportService.generateCommissionReport(commissionResult);

        // Assert
        assertNotNull(result);
        assertEquals(reportBytes.length, result.length);
        verify(reportGenerationService).generateMonthlyReport(eq(1L), anyInt(), anyInt());
    }

    @Test
    void testGenerateCollecteurReport() {
        // Arrange
        byte[] reportBytes = "Rapport collecteur".getBytes();
        LocalDate dateDebut = LocalDate.now().minusMonths(1);
        LocalDate dateFin = LocalDate.now();

        when(reportGenerationService.generateMonthlyReport(eq(1L), eq(dateDebut.getMonthValue()), eq(dateDebut.getYear())))
                .thenReturn(reportBytes);

        // Act
        byte[] result = reportService.generateCollecteurReport(1L, dateDebut, dateFin);

        // Assert
        assertNotNull(result);
        assertEquals(reportBytes.length, result.length);
        verify(reportGenerationService).generateMonthlyReport(eq(1L), eq(dateDebut.getMonthValue()), eq(dateDebut.getYear()));
    }

    @Test
    void testGenerateAgenceReport() {
        // Arrange
        byte[] reportBytes = "Rapport agence".getBytes();
        LocalDate dateDebut = LocalDate.now().minusMonths(1);
        LocalDate dateFin = LocalDate.now();

        // Pour cet exemple, simulons une méthode qui crée un rapport directement
        // Dans une implémentation réelle, cette méthode pourrait agréger les rapports de tous les collecteurs de l'agence

        // Act
        byte[] result = reportService.generateAgenceReport(1L, dateDebut, dateFin);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testGenerateGlobalReport() {
        // Arrange
        byte[] reportBytes = "Rapport global".getBytes();
        LocalDate dateDebut = LocalDate.now().minusMonths(1);
        LocalDate dateFin = LocalDate.now();

        // Act
        byte[] result = reportService.generateGlobalReport(dateDebut, dateFin);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testGenerateCollecteurMonthlyReport() {
        // Arrange
        byte[] reportBytes = "Rapport mensuel collecteur".getBytes();
        int month = 3;
        int year = 2025;

        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(collecteur));
        when(reportGenerationService.generateMonthlyReport(1L, month, year))
                .thenReturn(reportBytes);

        // Act
        byte[] result = reportService.generateCollecteurMonthlyReport(1L, month, year);

        // Assert
        assertNotNull(result);
        assertEquals(reportBytes.length, result.length);
        verify(reportGenerationService).generateMonthlyReport(1L, month, year);
    }
}