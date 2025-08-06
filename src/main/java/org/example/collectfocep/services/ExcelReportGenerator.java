package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.collectfocep.services.CommissionOrchestrator.CommissionClientDetail;
import org.example.collectfocep.services.CommissionOrchestrator.CommissionResult;
import org.example.collectfocep.services.RemunerationProcessor.RemunerationResult;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Générateur de rapports Excel réels selon spécification FOCEP
 * Plus de données mockées - que du vrai contenu basé sur les calculs
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelReportGenerator {

    private static final String FONT_NAME = "Calibri";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Génère le rapport Excel de commission selon la spec FOCEP
     */
    public byte[] generateCommissionReport(CommissionResult commissionResult) throws IOException {
        log.info("Génération rapport commission Excel - Collecteur: {}", commissionResult.getCollecteurId());

        try (Workbook workbook = new XSSFWorkbook()) {
            // Feuille principale : détail des commissions par client
            createCommissionDetailSheet(workbook, commissionResult);

            // Feuille synthèse : résumé collecteur
            createCommissionSummarySheet(workbook, commissionResult);

            // Export en bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            log.info("Rapport Excel généré - Taille: {} bytes", outputStream.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erreur génération rapport Excel: {}", e.getMessage(), e);
            throw new IOException("Erreur génération rapport Excel", e);
        }
    }

    /**
     * Génère le rapport Excel de rémunération 
     */
    public byte[] generateRemunerationReport(RemunerationResult remunerationResult, CommissionResult commissionResult) throws IOException {
        log.info("Génération rapport rémunération Excel - Collecteur: {}", remunerationResult.getCollecteurId());

        try (Workbook workbook = new XSSFWorkbook()) {
            // Feuille rémunération : détail Vi vs S
            createRemunerationDetailSheet(workbook, remunerationResult, commissionResult);

            // Feuille mouvements comptables
            createMovementsSheet(workbook, remunerationResult);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erreur génération rapport rémunération Excel: {}", e.getMessage(), e);
            throw new IOException("Erreur génération rapport rémunération", e);
        }
    }

    private void createCommissionDetailSheet(Workbook workbook, CommissionResult result) {
        Sheet sheet = workbook.createSheet("Détail Commissions");

        // Styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // En-tête rapport
        rowNum = createReportHeader(sheet, headerStyle, 
                "RAPPORT COMMISSION COLLECTE", 
                result.getCollecteurId(), result.getPeriode(), rowNum);

        // En-têtes colonnes
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Client", "Montant Épargne", "Commission (x)", "TVA (19,25%)", 
                           "Ancien Solde", "Nouveau Solde", "Type Paramètre"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données des clients  
        for (CommissionClientDetail detail : result.getCommissionsClients()) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(detail.getClientNom());
            setCurrencyCell(row, 1, detail.getMontantEpargne(), currencyStyle);
            setCurrencyCell(row, 2, detail.getCommissionX(), currencyStyle);
            setCurrencyCell(row, 3, detail.getTva(), currencyStyle);
            setCurrencyCell(row, 4, detail.getAncienSolde(), currencyStyle);
            setCurrencyCell(row, 5, detail.getNouveauSolde(), currencyStyle);
            row.createCell(6).setCellValue(detail.getParameterUsed());
        }

        // Ligne totaux
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue("TOTAUX");
        setCurrencyCell(totalRow, 2, result.getMontantSCollecteur(), currencyStyle);
        setCurrencyCell(totalRow, 3, result.getTotalTVA(), currencyStyle);
        
        // Style pour la ligne totaux
        for (int i = 0; i < 7; i++) {
            if (totalRow.getCell(i) != null) {
                totalRow.getCell(i).setCellStyle(headerStyle);
            }
        }

        // Auto-size colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCommissionSummarySheet(Workbook workbook, CommissionResult result) {
        Sheet sheet = workbook.createSheet("Synthèse Collecteur");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // En-tête
        rowNum = createReportHeader(sheet, headerStyle, 
                "SYNTHÈSE COMMISSION COLLECTEUR", 
                result.getCollecteurId(), result.getPeriode(), rowNum);

        // Métriques principales
        rowNum = createMetricRow(sheet, "Nombre de clients traités", 
                String.valueOf(result.getCommissionsClients().size()), headerStyle, rowNum);

        rowNum = createMetricRow(sheet, "Total commissions collectées (S)", 
                formatCurrency(result.getMontantSCollecteur()), headerStyle, rowNum);

        rowNum = createMetricRow(sheet, "Total TVA (19,25%)", 
                formatCurrency(result.getTotalTVA()), headerStyle, rowNum);

        rowNum = createMetricRow(sheet, "Date de calcul", 
                result.getDateCalcul().format(DATE_FORMAT), headerStyle, rowNum);

        // Informations comptes  
        rowNum++;
        Row compteRow = sheet.createRow(rowNum++);
        compteRow.createCell(0).setCellValue("INFORMATIONS COMPTES");
        compteRow.getCell(0).setCellStyle(headerStyle);

        rowNum = createMetricRow(sheet, "C.P.C.C (Commission)", "Crédité de " + formatCurrency(result.getMontantSCollecteur()), headerStyle, rowNum);
        rowNum = createMetricRow(sheet, "C.P.T (Taxe)", "Crédité de " + formatCurrency(result.getTotalTVA()), headerStyle, rowNum);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createRemunerationDetailSheet(Workbook workbook, RemunerationResult remResult, CommissionResult commResult) {
        Sheet sheet = workbook.createSheet("Rémunération Détail");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // En-tête
        rowNum = createReportHeader(sheet, headerStyle, 
                "RAPPORT RÉMUNÉRATION COLLECTEUR", 
                remResult.getCollecteurId(), "", rowNum);

        // Résumé rémunération
        rowNum = createMetricRow(sheet, "Montant S initial", formatCurrency(remResult.getMontantSInitial()), headerStyle, rowNum);
        rowNum = createMetricRow(sheet, "Total rubriques Vi", formatCurrency(remResult.getTotalRubriqueVi()), headerStyle, rowNum);
        rowNum = createMetricRow(sheet, "Surplus EMF", formatCurrency(remResult.getMontantEMF()), headerStyle, rowNum);

        // Détail des mouvements comptables
        rowNum++;
        Row mvtHeaderRow = sheet.createRow(rowNum++);
        mvtHeaderRow.createCell(0).setCellValue("MOUVEMENTS COMPTABLES");
        mvtHeaderRow.getCell(0).setCellStyle(headerStyle);

        Row mvtSubHeader = sheet.createRow(rowNum++);
        String[] mvtHeaders = {"Compte Source", "Compte Destination", "Montant", "Libellé"};
        for (int i = 0; i < mvtHeaders.length; i++) {
            Cell cell = mvtSubHeader.createCell(i);
            cell.setCellValue(mvtHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        // TODO: Ajouter les détails des mouvements quand la structure Mouvement sera finalisée

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createMovementsSheet(Workbook workbook, RemunerationResult remunerationResult) {
        Sheet sheet = workbook.createSheet("Mouvements Comptables");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Date", "Compte Débit", "Compte Crédit", "Montant", "Libellé"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // TODO: Remplir avec les vrais mouvements
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Méthodes utilitaires pour les styles

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0 \"FCFA\""));
        return style;
    }

    private int createReportHeader(Sheet sheet, CellStyle headerStyle, String title, Long collecteurId, String periode, int startRow) {
        int rowNum = startRow;
        
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue(title);
        titleRow.getCell(0).setCellStyle(headerStyle);
        
        if (collecteurId != null) {
            Row collecteurRow = sheet.createRow(rowNum++);
            collecteurRow.createCell(0).setCellValue("Collecteur ID: " + collecteurId);
        }
        
        if (periode != null && !periode.isEmpty()) {
            Row periodeRow = sheet.createRow(rowNum++);
            periodeRow.createCell(0).setCellValue("Période: " + periode);
        }
        
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Généré le: " + LocalDateTime.now().format(DATE_FORMAT));
        
        return rowNum + 1; // Ligne vide
    }

    private int createMetricRow(Sheet sheet, String label, String value, CellStyle style, int rowNum) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        row.getCell(0).setCellStyle(style);
        return rowNum + 1;
    }

    private void setCurrencyCell(Row row, int cellIndex, BigDecimal value, CellStyle currencyStyle) {
        if (value != null) {
            Cell cell = row.createCell(cellIndex);
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(currencyStyle);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount.doubleValue());
    }
}