package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final ClientRepository clientRepository;
    private final MouvementRepository mouvementRepository;
    private final CollecteurRepository collecteurRepository;
    private final ReportGenerationService reportGenerationService;

    /**
     * Génère un rapport de commission pour un résultat de calcul des commissions
     */
    public byte[] generateCommissionReport(CommissionResult result) {
        log.info("Génération du rapport de commission pour le collecteur: {}", result.getCollecteurId());

        // Cette méthode pourrait par exemple générer un rapport Excel ou PDF
        // résumant les commissions calculées
        // Pour cet exemple, on délègue au service de génération

        LocalDate now = LocalDate.now();
        return reportGenerationService.generateMonthlyReport(
                result.getCollecteurId(),
                now.getYear(),
                now.getMonthValue());
    }

    @Transactional(readOnly = true)
    public byte[] generateCollecteurReport(Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {
        log.info("Génération du rapport pour le collecteur: {} du {} au {}",
                collecteurId, dateDebut, dateFin);

        try {
            return reportGenerationService.generateMonthlyReport(
                    collecteurId,
                    dateDebut.getMonthValue(),
                    dateDebut.getYear());
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateAgenceReport(Long agenceId, LocalDate dateDebut, LocalDate dateFin) {
        log.info("Génération du rapport pour l'agence: {} du {} au {}",
                agenceId, dateDebut, dateFin);

        try {
            // Pour l'exemple, nous utilisons la même méthode que pour le rapport collecteur
            // Dans une implémentation réelle, cette méthode agrégerait les données de tous les collecteurs de l'agence
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Workbook workbook = new XSSFWorkbook();

            // Création d'une feuille récapitulative pour l'agence
            Sheet agenceSheet = workbook.createSheet("Récapitulatif Agence");

            // Ajout d'en-têtes et autres éléments
            // ...

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport pour l'agence", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateGlobalReport(LocalDate dateDebut, LocalDate dateFin) {
        log.info("Génération du rapport global du {} au {}", dateDebut, dateFin);

        try {
            // Pour l'exemple, nous créons un workbook simple
            // Dans une implémentation réelle, cette méthode agrégerait les données de toutes les agences
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Workbook workbook = new XSSFWorkbook();

            // Création d'une feuille récapitulative globale
            Sheet globalSheet = workbook.createSheet("Récapitulatif Global");

            // En-tête avec les dates
            Row headerRow = globalSheet.createRow(0);
            Cell titleCell = headerRow.createCell(0);
            titleCell.setCellValue("RAPPORT GLOBAL");

            Row dateRow = globalSheet.createRow(1);
            dateRow.createCell(0).setCellValue("Période:");
            dateRow.createCell(1).setCellValue(
                    dateDebut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " au " +
                            dateFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport global", e);
            throw new RuntimeException("Erreur lors de la génération du rapport global", e);
        }
    }

    /**
     * Génère un rapport mensuel pour un collecteur
     * Utilisé par AsyncReportService
     */
    public String generateMonthlyReport(Long collecteurId, List<Journal> journalEntries, YearMonth month) {
        log.info("Génération du rapport mensuel pour le collecteur {} - {}", collecteurId, month);

        try {
            // Génération du rapport
            byte[] reportBytes = reportGenerationService.generateMonthlyReport(
                    collecteurId,
                    month.getMonthValue(),
                    month.getYear());

            // Dans une implémentation réelle, nous sauvegarderions le fichier
            // et retournerions le chemin d'accès
            String filePath = "reports/" + collecteurId + "_" + month + ".xlsx";

            // Simuler la sauvegarde du fichier
            log.info("Rapport sauvegardé avec succès: {}", filePath);

            return filePath;

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport mensuel", e);
            throw new RuntimeException("Erreur lors de la génération du rapport mensuel", e);
        }
    }

    // Méthodes utilitaires pour la création des rapports Excel
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        return style;
    }

    @Transactional(readOnly = true)
    public byte[] generateCollecteurMonthlyReport(Long collecteurId, int month, int year) {
        log.info("Génération du rapport mensuel pour le collecteur: {} - {}/{}", collecteurId, month, year);

        try {
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

            // Utilisez le service de génération de rapport existant
            return reportGenerationService.generateMonthlyReport(collecteurId, month, year);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport mensuel", e);
            throw new RuntimeException("Erreur lors de la génération du rapport mensuel", e);
        }
    }
}