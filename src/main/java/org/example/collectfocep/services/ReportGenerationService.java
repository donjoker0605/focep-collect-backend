package org.example.collectfocep.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.MouvementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportGenerationService {

    private final MouvementRepository mouvementRepository;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;

    /**
     * Génère un rapport mensuel pour un collecteur selon le format décrit dans le cahier des charges
     */
    @Transactional(readOnly = true)
    public byte[] generateMonthlyReport(Long collecteurId, int year, int month) {
        log.info("Génération du rapport mensuel pour le collecteur {} - {}/{}", collecteurId, month, year);

        // 1. Récupérer les données nécessaires
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        int daysInMonth = endDate.getDayOfMonth();

        List<Client> clients = clientRepository.findByCollecteur(collecteur);

        // 2. Créer le workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            // Créer les styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);

            // Créer la feuille principale
            Sheet sheet = workbook.createSheet("Rapport " + month + "/" + year);

            // Ajouter l'en-tête
            createReportHeader(sheet, collecteur, startDate, endDate);

            // Ajouter la ligne de titres des colonnes
            int rowIndex = 5;
            Row headerRow = sheet.createRow(rowIndex++);
            createReportHeaderRow(headerRow, headerStyle, daysInMonth);

            // Récupérer tous les mouvements pour les clients sur la période
            Map<Long, Map<Integer, Double>> movementsByClientAndDay = getMovementsByClientAndDay(
                    clients, startDate, endDate);

            // Totaux par jour et totaux généraux
            double[] dailyTotals = new double[daysInMonth + 1];  // +1 pour le total mensuel
            double totalCommissions = 0;
            double totalTVA = 0;

            // Remplir les données pour chaque client
            for (Client client : clients) {
                Row clientRow = sheet.createRow(rowIndex++);
                Map<Integer, Double> dailyMovements = movementsByClientAndDay.getOrDefault(client.getId(),
                        new HashMap<>());

                ClientReportData data = generateClientReportData(client, dailyMovements, startDate, endDate);
                fillClientRow(clientRow, client, data, numberStyle, daysInMonth, dailyTotals);

                totalCommissions += data.getCommission();
                totalTVA += data.getTva();
            }

            // Ajouter la ligne des totaux
            Row totalRow = sheet.createRow(rowIndex);
            fillTotalRow(totalRow, totalStyle, dailyTotals, totalCommissions, totalTVA);

            // Ajuster les largeurs de colonnes
            adjustColumnWidths(sheet, daysInMonth);

            // Écrire le workbook dans un stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    /**
     * Crée l'en-tête du rapport avec les informations du collecteur et de la période
     */
    private void createReportHeader(Sheet sheet, Collecteur collecteur, LocalDate startDate, LocalDate endDate) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RAPPORT DE COLLECTE JOURNALIÈRE");

        Row collecteurRow = sheet.createRow(2);
        collecteurRow.createCell(0).setCellValue("Collecteur:");
        collecteurRow.createCell(1).setCellValue(collecteur.getNom() + " " + collecteur.getPrenom());

        Row periodRow = sheet.createRow(3);
        periodRow.createCell(0).setCellValue("Période:");
        periodRow.createCell(1).setCellValue(
                startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " au " +
                        endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
    }

    /**
     * Crée la ligne d'en-tête du tableau avec les 31 colonnes pour les jours du mois
     */
    private void createReportHeaderRow(Row headerRow, CellStyle style, int daysInMonth) {
        // Colonnes d'information client
        int colIdx = 0;
        createHeaderCell(headerRow, colIdx++, "N° Compte", style);
        createHeaderCell(headerRow, colIdx++, "Code Client", style);
        createHeaderCell(headerRow, colIdx++, "Code Produit", style);
        createHeaderCell(headerRow, colIdx++, "Nom et Prénom", style);

        // Colonnes pour chaque jour du mois
        for (int day = 1; day <= daysInMonth; day++) {
            createHeaderCell(headerRow, colIdx++, String.valueOf(day), style);
        }

        // Colonnes des totaux et informations financières
        createHeaderCell(headerRow, colIdx++, "Montant Total", style);
        createHeaderCell(headerRow, colIdx++, "Commission", style);
        createHeaderCell(headerRow, colIdx++, "TVA", style);
        createHeaderCell(headerRow, colIdx++, "Net à Payer Mois", style);
        createHeaderCell(headerRow, colIdx++, "Report", style);
        createHeaderCell(headerRow, colIdx++, "Retrait Report", style);
        createHeaderCell(headerRow, colIdx++, "Retrait Mois", style);
        createHeaderCell(headerRow, colIdx++, "Retrait Total", style);
        createHeaderCell(headerRow, colIdx++, "NAP Total", style);
    }

    /**
     * Récupère les mouvements organisés par client et par jour
     */
    private Map<Long, Map<Integer, Double>> getMovementsByClientAndDay(
            List<Client> clients, LocalDate startDate, LocalDate endDate) {

        Map<Long, Map<Integer, Double>> result = new HashMap<>();

        List<Long> clientIds = clients.stream()
                .map(Client::getId)
                .toList();

        // Dans une implémentation réelle, nous récupérerions les données depuis la BD
        // Pour l'exemple, nous allons utiliser des données fictives
        for (Client client : clients) {
            Map<Integer, Double> dailyAmounts = new HashMap<>();

            // Générer des montants aléatoires pour certains jours du mois
            for (int day = 1; day <= endDate.getDayOfMonth(); day++) {
                if (Math.random() > 0.7) { // 30% de chance d'avoir un montant pour ce jour
                    double amount = Math.round(Math.random() * 50000) / 100.0;
                    dailyAmounts.put(day, amount);
                }
            }

            result.put(client.getId(), dailyAmounts);
        }

        return result;
    }

    /**
     * Génère les données de rapport pour un client
     */
    private ClientReportData generateClientReportData(
            Client client, Map<Integer, Double> dailyMovements,
            LocalDate startDate, LocalDate endDate) {

        ClientReportData data = new ClientReportData();
        data.setDailyAmounts(dailyMovements);

        // Calculer le montant total collecté
        double totalCollected = dailyMovements.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        data.setTotalCollected(totalCollected);

        // Calculer la commission (simulation - normalement via le service de commission)
        double commission = totalCollected * 0.02; // 2% exemple
        data.setCommission(commission);

        // Calculer la TVA
        double tva = commission * 0.1925; // 19.25%
        data.setTva(tva);

        // Calculer le net à payer
        data.setNetAPayer(totalCollected - commission - tva);

        // Simuler les valeurs de report et retraits
        data.setPreviousBalance(50000 + Math.random() * 20000);
        data.setWithdrawalsPrevious(10000 + Math.random() * 5000);
        data.setWithdrawalsMonth(Math.random() * 15000);
        data.setTotalWithdrawals(data.getWithdrawalsPrevious() + data.getWithdrawalsMonth());

        // Calculer le Net à Payer Total
        data.setNapTotal(data.getPreviousBalance() + data.getNetAPayer() - data.getWithdrawalsMonth());

        return data;
    }

    /**
     * Remplit une ligne de données client dans le rapport
     */
    private void fillClientRow(
            Row row, Client client, ClientReportData data,
            CellStyle numberStyle, int daysInMonth, double[] dailyTotals) {

        int colIdx = 0;

        // Informations client
        row.createCell(colIdx++).setCellValue("COMPTE-" + client.getId());
        row.createCell(colIdx++).setCellValue(client.getNumeroCni());
        row.createCell(colIdx++).setCellValue("EPARGNE-J");
        row.createCell(colIdx++).setCellValue(client.getNom() + " " + client.getPrenom());

        // Montants journaliers
        for (int day = 1; day <= daysInMonth; day++) {
            Cell cell = row.createCell(colIdx++);

            Double amount = data.getDailyAmounts().get(day);
            if (amount != null && amount > 0) {
                cell.setCellValue(amount);
                cell.setCellStyle(numberStyle);

                // Ajouter au total journalier
                dailyTotals[day-1] += amount;
            }
        }

        // Montant total collecté
        Cell totalCell = row.createCell(colIdx++);
        totalCell.setCellValue(data.getTotalCollected());
        totalCell.setCellStyle(numberStyle);
        dailyTotals[daysInMonth] += data.getTotalCollected();

        // Commission
        Cell commissionCell = row.createCell(colIdx++);
        commissionCell.setCellValue(data.getCommission());
        commissionCell.setCellStyle(numberStyle);

        // TVA
        Cell tvaCell = row.createCell(colIdx++);
        tvaCell.setCellValue(data.getTva());
        tvaCell.setCellStyle(numberStyle);

        // Net à payer mois
        Cell napCell = row.createCell(colIdx++);
        napCell.setCellValue(data.getNetAPayer());
        napCell.setCellStyle(numberStyle);

        // Report
        Cell reportCell = row.createCell(colIdx++);
        reportCell.setCellValue(data.getPreviousBalance());
        reportCell.setCellStyle(numberStyle);

        // Retrait report
        Cell retraitReportCell = row.createCell(colIdx++);
        retraitReportCell.setCellValue(data.getWithdrawalsPrevious());
        retraitReportCell.setCellStyle(numberStyle);

        // Retrait mois
        Cell retraitMoisCell = row.createCell(colIdx++);
        retraitMoisCell.setCellValue(data.getWithdrawalsMonth());
        retraitMoisCell.setCellStyle(numberStyle);

        // Retrait total
        Cell retraitTotalCell = row.createCell(colIdx++);
        retraitTotalCell.setCellValue(data.getTotalWithdrawals());
        retraitTotalCell.setCellStyle(numberStyle);

        // NAP total
        Cell napTotalCell = row.createCell(colIdx++);
        napTotalCell.setCellValue(data.getNapTotal());
        napTotalCell.setCellStyle(numberStyle);
    }

    /**
     * Remplit la ligne des totaux
     */
    private void fillTotalRow(
            Row row, CellStyle style, double[] dailyTotals,
            double totalCommissions, double totalTVA) {

        // Entête des totaux
        row.createCell(0).setCellValue("TOTAUX");
        Cell totalLabelCell = row.createCell(3);
        totalLabelCell.setCellValue("TOTAUX");
        totalLabelCell.setCellStyle(style);

        // Totaux par jour
        int daysInMonth = dailyTotals.length - 1;
        for (int day = 0; day < daysInMonth; day++) {
            if (dailyTotals[day] > 0) {
                Cell cell = row.createCell(day + 4); // +4 car les 4 premières colonnes sont pour les infos client
                cell.setCellValue(dailyTotals[day]);
                cell.setCellStyle(style);
            }
        }

        // Total mensuel
        Cell totalMonthCell = row.createCell(daysInMonth + 4);
        totalMonthCell.setCellValue(dailyTotals[daysInMonth]);
        totalMonthCell.setCellStyle(style);

        // Total commissions
        Cell totalCommissionsCell = row.createCell(daysInMonth + 5);
        totalCommissionsCell.setCellValue(totalCommissions);
        totalCommissionsCell.setCellStyle(style);

        // Total TVA
        Cell totalTVACell = row.createCell(daysInMonth + 6);
        totalTVACell.setCellValue(totalTVA);
        totalTVACell.setCellStyle(style);

        // Net à payer total
        Cell napTotalCell = row.createCell(daysInMonth + 7);
        napTotalCell.setCellValue(dailyTotals[daysInMonth] - totalCommissions - totalTVA);
        napTotalCell.setCellStyle(style);
    }

    /**
     * Ajuste les largeurs des colonnes du rapport
     */
    private void adjustColumnWidths(Sheet sheet, int daysInMonth) {
        // Colonnes d'information client
        sheet.setColumnWidth(0, 15 * 256); // N° Compte
        sheet.setColumnWidth(1, 15 * 256); // Code Client
        sheet.setColumnWidth(2, 15 * 256); // Code Produit
        sheet.setColumnWidth(3, 30 * 256); // Nom et Prénom

        // Colonnes des jours (largeur réduite)
        for (int i = 4; i < 4 + daysInMonth; i++) {
            sheet.setColumnWidth(i, 7 * 256);
        }

        // Colonnes des totaux et informations financières
        for (int i = 4 + daysInMonth; i < 4 + daysInMonth + 10; i++) {
            sheet.setColumnWidth(i, 15 * 256);
        }
    }

    /**
     * Crée une cellule d'en-tête avec style
     */
    private void createHeaderCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // Classe interne pour stocker les données de rapport d'un client
    @Data
    public static class ClientReportData {
        private double totalCollected;
        private double commission;
        private double tva;
        private double netAPayer;
        private double previousBalance;
        private double withdrawalsPrevious;
        private double withdrawalsMonth;
        private double totalWithdrawals;
        private double napTotal;
        private Map<Integer, Double> dailyAmounts = new HashMap<>();
    }

    // Méthodes de création de styles
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
}