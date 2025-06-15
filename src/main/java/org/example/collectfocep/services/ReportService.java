package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.collectfocep.dto.ReportDTO;
import org.example.collectfocep.dto.ReportRequestDTO;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final AgenceRepository agenceRepository;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final MouvementRepository mouvementRepository;
    private final JournalRepository journalRepository;
    private final SecurityService securityService;

    /**
     * ✅ RÉCUPÉRER LES RAPPORTS RÉCENTS PAR AGENCE
     */
    public Page<ReportDTO> getRecentReportsByAgence(Long agenceId, PageRequest pageRequest) {
        log.info("📋 Récupération des rapports récents pour l'agence: {}", agenceId);

        Page<Report> reportsPage = reportRepository.findByAgenceIdOrderByDateCreationDesc(agenceId, pageRequest);

        return reportsPage.map(this::convertToDTO);
    }

    /**
     * ✅ GÉNÉRER UN NOUVEAU RAPPORT
     */
    public ReportDTO generateReport(ReportRequestDTO request, Long agenceId) {
        log.info("📊 Génération d'un rapport de type: {} pour l'agence: {}", request.getType(), agenceId);

        // ✅ VALIDATION DE L'AGENCE
        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée"));

        // ✅ VALIDATION DU COLLECTEUR SI NÉCESSAIRE
        Collecteur collecteur = null;
        if (request.getCollecteurId() != null) {
            collecteur = collecteurRepository.findById(request.getCollecteurId())
                    .orElseThrow(() -> new RuntimeException("Collecteur non trouvé"));

            // ✅ VÉRIFIER QUE LE COLLECTEUR APPARTIENT À L'AGENCE
            if (!collecteur.getAgence().getId().equals(agenceId)) {
                throw new SecurityException("Collecteur n'appartient pas à cette agence");
            }
        }

        // ✅ CRÉER L'ENTITÉ RAPPORT
        Report report = Report.builder()
                .type(request.getType())
                .title(generateReportTitle(request, collecteur))
                .description(request.getDescription())
                .status(Report.ReportStatus.PENDING)
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .agence(agence)
                .collecteur(collecteur)
                .fileFormat(request.getFormat() != null ? request.getFormat() : "PDF")
                .createdBy(securityService.getCurrentUserEmail())
                .parametres(buildParametresJson(request))
                .build();

        // ✅ SAUVEGARDER LE RAPPORT
        Report savedReport = reportRepository.save(report);

        // ✅ DÉMARRER LA GÉNÉRATION ASYNCHRONE
        processReportGeneration(savedReport);

        log.info("✅ Rapport créé avec succès: {}", savedReport.getId());
        return convertToDTO(savedReport);
    }

    /**
     * ✅ MÉTHODE MANQUANTE POUR AsyncReportService
     * Génère un rapport mensuel pour un collecteur
     */
    public String generateMonthlyReport(Long collecteurId, List<Journal> journalEntries, YearMonth month) {
        log.info("📊 Génération du rapport mensuel pour collecteur: {} - mois: {}", collecteurId, month);

        try {
            // Récupérer le collecteur
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new RuntimeException("Collecteur non trouvé"));

            // Récupérer les données du mois
            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);

            // Récupérer tous les mouvements du mois
            List<Mouvement> mouvements = mouvementRepository.findByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfMonth, endOfMonth);

            // Récupérer les clients du collecteur
            List<Client> clients = clientRepository.findByCollecteurId(collecteurId);

            // Générer le fichier Excel
            byte[] excelData = generateMonthlyExcelReport(collecteur, clients, mouvements, journalEntries, month);

            // Sauvegarder le fichier (simulation)
            String fileName = String.format("rapport_mensuel_%d_%s.xlsx", collecteurId, month.toString());
            String filePath = "/reports/" + fileName;

            // TODO: Sauvegarder physiquement le fichier
            // Files.write(Paths.get(filePath), excelData);

            log.info("✅ Rapport mensuel généré: {}", filePath);
            return filePath;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du rapport mensuel", e);
            throw new RuntimeException("Erreur génération rapport mensuel: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ MÉTHODE MANQUANTE POUR EnhancedReportController
     * Génère un rapport mensuel Excel avec 31 colonnes pour les jours
     */
    public byte[] generateCollecteurMonthlyReport(Long collecteurId, int month, int year) {
        log.info("📊 Génération rapport Excel collecteur: {} - {}/{}", collecteurId, month, year);

        try {
            // Validation des paramètres
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Mois invalide: " + month);
            }

            YearMonth yearMonth = YearMonth.of(year, month);

            // Récupérer le collecteur
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new RuntimeException("Collecteur non trouvé"));

            // Récupérer les données du mois
            LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            // Récupérer les mouvements du mois
            List<Mouvement> mouvements = mouvementRepository.findByCollecteurIdAndDateOperationBetween(
                    collecteurId, startOfMonth, endOfMonth);

            // Récupérer les clients
            List<Client> clients = clientRepository.findByCollecteurId(collecteurId);

            // Récupérer les journaux du mois
            List<Journal> journaux = journalRepository.findByCollecteurIdAndPeriod(
                    collecteurId, startOfMonth.toLocalDate(), endOfMonth.toLocalDate());

            // Générer le fichier Excel
            return generateDetailedMonthlyExcelReport(collecteur, clients, mouvements, journaux, yearMonth);

        } catch (Exception e) {
            log.error("❌ Erreur génération rapport Excel collecteur", e);
            throw new RuntimeException("Erreur génération rapport Excel: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ GÉNÉRATION DU RAPPORT EXCEL MENSUEL DÉTAILLÉ
     */
    private byte[] generateDetailedMonthlyExcelReport(Collecteur collecteur, List<Client> clients,
                                                      List<Mouvement> mouvements, List<Journal> journaux,
                                                      YearMonth yearMonth) throws IOException {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Feuille principale - Vue d'ensemble
            Sheet overviewSheet = workbook.createSheet("Vue d'ensemble");
            createOverviewSheet(overviewSheet, collecteur, clients, mouvements, yearMonth);

            // Feuille détails journaliers - 31 colonnes
            Sheet dailySheet = workbook.createSheet("Détails journaliers");
            createDailyDetailsSheet(dailySheet, collecteur, clients, mouvements, yearMonth);

            // Feuille transactions
            Sheet transactionsSheet = workbook.createSheet("Transactions");
            createTransactionsSheet(transactionsSheet, mouvements, yearMonth);

            // Feuille clients
            Sheet clientsSheet = workbook.createSheet("Clients");
            createClientsSheet(clientsSheet, clients);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * ✅ CRÉER LA FEUILLE VUE D'ENSEMBLE
     */
    private void createOverviewSheet(Sheet sheet, Collecteur collecteur, List<Client> clients,
                                     List<Mouvement> mouvements, YearMonth yearMonth) {

        // Styles
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle currencyStyle = createCurrencyStyle(sheet.getWorkbook());

        int rowNum = 0;

        // En-tête
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("RAPPORT MENSUEL COLLECTEUR");
        headerRow.getCell(0).setCellStyle(headerStyle);

        // Informations collecteur
        rowNum++;
        createInfoRow(sheet, rowNum++, "Collecteur:", collecteur.getPrenom() + " " + collecteur.getNom());
        createInfoRow(sheet, rowNum++, "Période:", yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)));
        createInfoRow(sheet, rowNum++, "Agence:", collecteur.getAgence().getNomAgence());

        // Statistiques
        rowNum++;
        Row statsHeaderRow = sheet.createRow(rowNum++);
        statsHeaderRow.createCell(0).setCellValue("STATISTIQUES DU MOIS");
        statsHeaderRow.getCell(0).setCellStyle(headerStyle);

        // Calculs
        int totalClients = clients.size();
        double totalEpargne = mouvements.stream()
                .filter(m -> "EPARGNE".equals(m.getSens()) || "DEPOT".equals(m.getSens()))
                .mapToDouble(Mouvement::getMontant)
                .sum();
        double totalRetraits = mouvements.stream()
                .filter(m -> "RETRAIT".equals(m.getSens()))
                .mapToDouble(Mouvement::getMontant)
                .sum();

        createInfoRow(sheet, rowNum++, "Nombre de clients:", String.valueOf(totalClients));
        createInfoRowWithCurrency(sheet, rowNum++, "Total épargne:", totalEpargne, currencyStyle);
        createInfoRowWithCurrency(sheet, rowNum++, "Total retraits:", totalRetraits, currencyStyle);
        createInfoRowWithCurrency(sheet, rowNum++, "Solde net:", totalEpargne - totalRetraits, currencyStyle);
        createInfoRow(sheet, rowNum++, "Nombre de transactions:", String.valueOf(mouvements.size()));

        // Auto-ajuster les colonnes
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * ✅ CRÉER LA FEUILLE DÉTAILS JOURNALIERS (31 COLONNES)
     */
    private void createDailyDetailsSheet(Sheet sheet, Collecteur collecteur, List<Client> clients,
                                         List<Mouvement> mouvements, YearMonth yearMonth) {

        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle currencyStyle = createCurrencyStyle(sheet.getWorkbook());

        int rowNum = 0;

        // En-tête avec les 31 jours du mois
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Type");
        headerRow.getCell(0).setCellStyle(headerStyle);

        // Créer les colonnes pour chaque jour du mois
        int daysInMonth = yearMonth.lengthOfMonth();
        for (int day = 1; day <= 31; day++) {
            Cell cell = headerRow.createCell(day);
            if (day <= daysInMonth) {
                cell.setCellValue(String.valueOf(day));
            } else {
                cell.setCellValue("-");
            }
            cell.setCellStyle(headerStyle);
        }

        // Grouper les mouvements par jour
        Map<Integer, List<Mouvement>> mouvementsByDay = mouvements.stream()
                .collect(Collectors.groupingBy(m -> m.getDateOperation().getDayOfMonth()));

        // Ligne épargne
        Row epargneRow = sheet.createRow(rowNum++);
        epargneRow.createCell(0).setCellValue("Épargne");
        for (int day = 1; day <= 31; day++) {
            Cell cell = epargneRow.createCell(day);
            if (day <= daysInMonth) {
                double epargneDay = mouvementsByDay.getOrDefault(day, new ArrayList<>()).stream()
                        .filter(m -> "EPARGNE".equals(m.getSens()) || "DEPOT".equals(m.getSens()))
                        .mapToDouble(Mouvement::getMontant)
                        .sum();
                cell.setCellValue(epargneDay);
                cell.setCellStyle(currencyStyle);
            }
        }

        // Ligne retraits
        Row retraitsRow = sheet.createRow(rowNum++);
        retraitsRow.createCell(0).setCellValue("Retraits");
        for (int day = 1; day <= 31; day++) {
            Cell cell = retraitsRow.createCell(day);
            if (day <= daysInMonth) {
                double retraitsDay = mouvementsByDay.getOrDefault(day, new ArrayList<>()).stream()
                        .filter(m -> "RETRAIT".equals(m.getSens()))
                        .mapToDouble(Mouvement::getMontant)
                        .sum();
                cell.setCellValue(retraitsDay);
                cell.setCellStyle(currencyStyle);
            }
        }

        // Ligne nombre de transactions
        Row nbTransactionsRow = sheet.createRow(rowNum++);
        nbTransactionsRow.createCell(0).setCellValue("Nb Transactions");
        for (int day = 1; day <= 31; day++) {
            Cell cell = nbTransactionsRow.createCell(day);
            if (day <= daysInMonth) {
                long nbTransactions = mouvementsByDay.getOrDefault(day, new ArrayList<>()).size();
                cell.setCellValue(nbTransactions);
            }
        }

        // Auto-ajuster la première colonne
        sheet.autoSizeColumn(0);
    }

    /**
     * ✅ CRÉER LA FEUILLE TRANSACTIONS
     */
    private void createTransactionsSheet(Sheet sheet, List<Mouvement> mouvements, YearMonth yearMonth) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle currencyStyle = createCurrencyStyle(sheet.getWorkbook());

        int rowNum = 0;

        // En-tête
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Client", "Type", "Montant", "Libellé", "Statut"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Trier les mouvements par date
        mouvements.sort(Comparator.comparing(Mouvement::getDateOperation));

        // Données
        for (Mouvement mouvement : mouvements) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(mouvement.getDateOperation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String clientName = "N/A";
            if (mouvement.getClient() != null) {
                clientName = mouvement.getClient().getPrenom() + " " + mouvement.getClient().getNom();
            }
            row.createCell(1).setCellValue(clientName);

            row.createCell(2).setCellValue(mouvement.getSens() != null ? mouvement.getSens() : "N/A");

            Cell montantCell = row.createCell(3);
            montantCell.setCellValue(mouvement.getMontant());
            montantCell.setCellStyle(currencyStyle);

            row.createCell(4).setCellValue(mouvement.getLibelle() != null ? mouvement.getLibelle() : "");
            row.createCell(5).setCellValue("VALIDE"); // Statut par défaut
        }

        // Auto-ajuster les colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * ✅ CRÉER LA FEUILLE CLIENTS
     */
    private void createClientsSheet(Sheet sheet, List<Client> clients) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());

        int rowNum = 0;

        // En-tête
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Nom", "Prénom", "Téléphone", "Ville", "N° Compte", "Statut"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        for (Client client : clients) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(client.getNom() != null ? client.getNom() : "");
            row.createCell(1).setCellValue(client.getPrenom() != null ? client.getPrenom() : "");
            row.createCell(2).setCellValue(client.getTelephone() != null ? client.getTelephone() : "");
            row.createCell(3).setCellValue(client.getVille() != null ? client.getVille() : "");
            row.createCell(4).setCellValue(client.getNumeroCompte() != null ? client.getNumeroCompte() : "");
            row.createCell(5).setCellValue(client.getValide() ? "Actif" : "Inactif");
        }

        // Auto-ajuster les colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * ✅ MÉTHODES UTILITAIRES POUR EXCEL
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00 \"FCFA\""));
        return style;
    }

    private void createInfoRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private void createInfoRowWithCurrency(Sheet sheet, int rowNum, String label, double value, CellStyle currencyStyle) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(currencyStyle);
    }

    /**
     * ✅ GÉNÉRATION EXCEL SIMPLE POUR AsyncReportService
     */
    private byte[] generateMonthlyExcelReport(Collecteur collecteur, List<Client> clients,
                                              List<Mouvement> mouvements, List<Journal> journalEntries,
                                              YearMonth month) throws IOException {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Rapport Mensuel");

            // En-tête simple
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("RAPPORT MENSUEL - " + month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)));

            // Informations de base
            int rowNum = 2;
            createInfoRow(sheet, rowNum++, "Collecteur:", collecteur.getPrenom() + " " + collecteur.getNom());
            createInfoRow(sheet, rowNum++, "Nombre de clients:", String.valueOf(clients.size()));
            createInfoRow(sheet, rowNum++, "Nombre de transactions:", String.valueOf(mouvements.size()));

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * ✅ RÉCUPÉRER UN RAPPORT PAR ID
     */
    public ReportDTO getReportById(Long reportId, Long agenceId) {
        log.info("📋 Récupération du rapport: {} pour l'agence: {}", reportId, agenceId);

        Optional<Report> reportOpt = reportRepository.findByIdAndAgenceId(reportId, agenceId);

        return reportOpt.map(this::convertToDTO).orElse(null);
    }

    /**
     * ✅ SUPPRIMER UN RAPPORT
     */
    public boolean deleteReport(Long reportId, Long agenceId) {
        log.info("🗑️ Suppression du rapport: {} pour l'agence: {}", reportId, agenceId);

        Optional<Report> reportOpt = reportRepository.findByIdAndAgenceId(reportId, agenceId);

        if (reportOpt.isPresent()) {
            reportRepository.delete(reportOpt.get());
            return true;
        }

        return false;
    }

    /**
     * ✅ OBTENIR LES TYPES DE RAPPORTS DISPONIBLES
     */
    public List<String> getAvailableReportTypes() {
        return Arrays.asList("collecteur", "commission", "agence", "global");
    }

    /**
     * ✅ TRAITEMENT ASYNCHRONE DE LA GÉNÉRATION
     */
    private void processReportGeneration(Report report) {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simuler le traitement

                report.setStatus(Report.ReportStatus.COMPLETED);
                report.setNombreEnregistrements(generateMockRecordCount(report));
                report.setFileSize(generateMockFileSize());

                reportRepository.save(report);

                log.info("✅ Rapport {} généré avec succès", report.getId());

            } catch (InterruptedException e) {
                log.error("❌ Erreur lors de la génération du rapport {}", report.getId(), e);
                report.setStatus(Report.ReportStatus.FAILED);
                reportRepository.save(report);
            }
        }).start();
    }

    /**
     * ✅ GÉNÉRER UN TITRE DE RAPPORT
     */
    private String generateReportTitle(ReportRequestDTO request, Collecteur collecteur) {
        String baseTitle = switch (request.getType()) {
            case "collecteur" -> "Rapport Collecteur";
            case "commission" -> "Rapport Commissions";
            case "agence" -> "Rapport Agence";
            case "global" -> "Rapport Global";
            default -> "Rapport";
        };

        if (collecteur != null) {
            baseTitle += " - " + collecteur.getPrenom() + " " + collecteur.getNom();
        }

        return baseTitle;
    }

    /**
     * ✅ CONSTRUIRE LES PARAMÈTRES JSON
     */
    private String buildParametresJson(ReportRequestDTO request) {
        return String.format(
                "{\"type\":\"%s\",\"collecteurId\":%s,\"format\":\"%s\"}",
                request.getType(),
                request.getCollecteurId(),
                request.getFormat() != null ? request.getFormat() : "PDF"
        );
    }

    /**
     * ✅ GÉNÉRER UN NOMBRE D'ENREGISTREMENTS FICTIF
     */
    private Integer generateMockRecordCount(Report report) {
        return switch (report.getType()) {
            case "collecteur" -> (int) (Math.random() * 100) + 50;
            case "commission" -> (int) (Math.random() * 50) + 10;
            case "agence" -> (int) (Math.random() * 500) + 100;
            default -> (int) (Math.random() * 200) + 25;
        };
    }

    /**
     * ✅ GÉNÉRER UNE TAILLE DE FICHIER FICTIVE
     */
    private Long generateMockFileSize() {
        return (long) (Math.random() * 1000000) + 50000; // Entre 50KB et 1MB
    }

    /**
     * ✅ CONVERTIR ENTITÉ VERS DTO
     */
    private ReportDTO convertToDTO(Report report) {
        return ReportDTO.builder()
                .id(report.getId())
                .type(report.getType())
                .title(report.getTitle())
                .description(report.getDescription())
                .status(report.getStatus().name().toLowerCase())
                .dateCreation(report.getDateCreation())
                .dateDebut(report.getDateDebut())
                .dateFin(report.getDateFin())
                .agenceId(report.getAgence().getId())
                .nomAgence(report.getAgence().getNomAgence())
                .collecteurId(report.getCollecteur() != null ? report.getCollecteur().getId() : null)
                .nomCollecteur(report.getCollecteur() != null ?
                        report.getCollecteur().getPrenom() + " " + report.getCollecteur().getNom() : null)
                .downloadUrl(report.getFilePath())
                .tailleFichier(report.getFileSize())
                .formatFichier(report.getFileFormat())
                .createdBy(report.getCreatedBy())
                .nombreEnregistrements(report.getNombreEnregistrements())
                .parametres(report.getParametres())
                .build();
    }
}