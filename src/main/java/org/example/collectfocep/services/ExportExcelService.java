package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 📊 Service d'export Excel pour SuperAdmin
 * Génère des fichiers Excel multi-onglets pour intégration core banking
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExportExcelService {

    private final AgenceRepository agenceRepository;
    private final AdminRepository adminRepository;
    private final CollecteurRepository collecteurRepository;
    private final ClientRepository clientRepository;
    private final MouvementRepository mouvementRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    /**
     * 📊 Export complet multi-onglets pour core banking
     */
    public byte[] exportCompleteData(ExportFilters filters) throws IOException {
        log.info("🚀 Début export Excel complet avec filtres: {}", filters);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // Styles pour les en-têtes et données
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Création des onglets
            createAgencesSheet(workbook, headerStyle, dataStyle, dateStyle, filters);
            createAdminsSheet(workbook, headerStyle, dataStyle, dateStyle, filters);
            createCollecteursSheet(workbook, headerStyle, dataStyle, dateStyle, currencyStyle, filters);
            createClientsSheet(workbook, headerStyle, dataStyle, dateStyle, currencyStyle, filters);
            createTransactionsSheet(workbook, headerStyle, dataStyle, dateStyle, currencyStyle, filters);
            createSummarySheet(workbook, headerStyle, dataStyle, currencyStyle, filters);

            // Conversion en bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            log.info("✅ Export Excel terminé - Taille: {} KB", outputStream.size() / 1024);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'export Excel: {}", e.getMessage(), e);
            throw new IOException("Erreur lors de la génération du fichier Excel", e);
        }
    }

    /**
     * 🏢 Onglet Agences
     */
    private void createAgencesSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, 
                                   CellStyle dateStyle, ExportFilters filters) {
        Sheet sheet = workbook.createSheet("Agences");
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "Code Agence", "Nom Agence", "Ville", "Quartier", "Adresse", 
            "Téléphone", "Responsable", "Statut", "Date Création", 
            "Nb Admins", "Nb Collecteurs", "Nb Clients", "Montant Total Épargnes"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        List<Agence> agences = getFilteredAgences(filters);
        int rowNum = 1;
        
        for (Agence agence : agences) {
            Row row = sheet.createRow(rowNum++);
            int cellNum = 0;
            
            createCell(row, cellNum++, agence.getId(), dataStyle);
            createCell(row, cellNum++, agence.getCodeAgence(), dataStyle);
            createCell(row, cellNum++, agence.getNomAgence(), dataStyle);
            createCell(row, cellNum++, agence.getVille(), dataStyle);
            createCell(row, cellNum++, agence.getQuartier(), dataStyle);
            createCell(row, cellNum++, agence.getAdresse(), dataStyle);
            createCell(row, cellNum++, agence.getTelephone(), dataStyle);
            createCell(row, cellNum++, agence.getResponsable(), dataStyle);
            createCell(row, cellNum++, agence.isActive() ? "Actif" : "Inactif", dataStyle);
            createCell(row, cellNum++, agence.getDateCreation(), dateStyle);
            createCell(row, cellNum++, agence.getNombreCollecteurs(), dataStyle);
            createCell(row, cellNum++, agence.getNombreCollecteursActifs(), dataStyle);
            createCell(row, cellNum++, agence.getNombreClients(), dataStyle);
            createCell(row, cellNum++, calculateTotalEpargnesAgence(agence.getId()), dataStyle);
        }
        
        // Auto-size colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 👥 Onglet Admins
     */
    private void createAdminsSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, 
                                  CellStyle dateStyle, ExportFilters filters) {
        Sheet sheet = workbook.createSheet("Admins");
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "Nom", "Prénom", "CNI", "Email", "Téléphone", 
            "Agence", "Date Création", "Nb Collecteurs Supervisés"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        List<Admin> admins = getFilteredAdmins(filters);
        int rowNum = 1;
        
        for (Admin admin : admins) {
            Row row = sheet.createRow(rowNum++);
            int cellNum = 0;
            
            createCell(row, cellNum++, admin.getId(), dataStyle);
            createCell(row, cellNum++, admin.getNom(), dataStyle);
            createCell(row, cellNum++, admin.getPrenom(), dataStyle);
            createCell(row, cellNum++, admin.getNumeroCni(), dataStyle);
            createCell(row, cellNum++, admin.getAdresseMail(), dataStyle);
            createCell(row, cellNum++, admin.getTelephone(), dataStyle);
            createCell(row, cellNum++, admin.getAgence() != null ? admin.getAgence().getNomAgence() : "Non assigné", dataStyle);
            createCell(row, cellNum++, admin.getDateCreation(), dateStyle);
            createCell(row, cellNum++, countCollecteursByAdmin(admin.getId()), dataStyle);
        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 👨‍💼 Onglet Collecteurs
     */
    private void createCollecteursSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, 
                                       CellStyle dateStyle, CellStyle currencyStyle, ExportFilters filters) {
        Sheet sheet = workbook.createSheet("Collecteurs");
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "Nom", "Prénom", "CNI", "Email", "Téléphone", "Agence", 
            "Statut", "Ancienneté (mois)", "Montant Max Retrait", "Date Création",
            "Nb Clients", "Nb Clients Actifs", "Total Épargnes Collectées"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        List<Collecteur> collecteurs = getFilteredCollecteurs(filters);
        int rowNum = 1;
        
        for (Collecteur collecteur : collecteurs) {
            Row row = sheet.createRow(rowNum++);
            int cellNum = 0;
            
            createCell(row, cellNum++, collecteur.getId(), dataStyle);
            createCell(row, cellNum++, collecteur.getNom(), dataStyle);
            createCell(row, cellNum++, collecteur.getPrenom(), dataStyle);
            createCell(row, cellNum++, collecteur.getNumeroCni(), dataStyle);
            createCell(row, cellNum++, collecteur.getAdresseMail(), dataStyle);
            createCell(row, cellNum++, collecteur.getTelephone(), dataStyle);
            createCell(row, cellNum++, collecteur.getAgence() != null ? collecteur.getAgence().getNomAgence() : "Non assigné", dataStyle);
            createCell(row, cellNum++, collecteur.isActive() ? "Actif" : "Inactif", dataStyle);
            createCell(row, cellNum++, collecteur.getAncienneteEnMois(), dataStyle);
            createCell(row, cellNum++, collecteur.getMontantMaxRetrait(), currencyStyle);
            createCell(row, cellNum++, collecteur.getDateCreation(), dateStyle);
            createCell(row, cellNum++, countClientsByCollecteur(collecteur.getId()), dataStyle);
            createCell(row, cellNum++, countActiveClientsByCollecteur(collecteur.getId()), dataStyle);
            createCell(row, cellNum++, calculateTotalEpargnesCollecteur(collecteur.getId()), currencyStyle);
        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 👤 Onglet Clients
     */
    private void createClientsSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, 
                                   CellStyle dateStyle, CellStyle currencyStyle, ExportFilters filters) {
        Sheet sheet = workbook.createSheet("Clients");
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "ID", "Nom", "Prénom", "CNI", "Téléphone", "Collecteur", "Agence",
            "Statut", "Date Création", "Solde Total", "Nb Transactions", "Dernière Transaction"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données (limitées pour performance)
        List<Client> clients = getFilteredClients(filters);
        int rowNum = 1;
        
        for (Client client : clients) {
            Row row = sheet.createRow(rowNum++);
            int cellNum = 0;
            
            createCell(row, cellNum++, client.getId(), dataStyle);
            createCell(row, cellNum++, client.getNom(), dataStyle);
            createCell(row, cellNum++, client.getPrenom(), dataStyle);
            createCell(row, cellNum++, client.getNumeroCni(), dataStyle);
            createCell(row, cellNum++, client.getTelephone(), dataStyle);
            createCell(row, cellNum++, client.getCollecteur() != null ? client.getCollecteur().getNom() + " " + client.getCollecteur().getPrenom() : "Non assigné", dataStyle);
            createCell(row, cellNum++, client.getAgence() != null ? client.getAgence().getNomAgence() : "Non assigné", dataStyle);
            createCell(row, cellNum++, client.getValide() ? "Actif" : "Inactif", dataStyle);
            createCell(row, cellNum++, client.getDateCreation(), dateStyle);
            createCell(row, cellNum++, calculateSoldeClient(client.getId()), currencyStyle);
            createCell(row, cellNum++, countTransactionsByClient(client.getId()), dataStyle);
            createCell(row, cellNum++, getLastTransactionDate(client.getId()), dateStyle);
        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 💰 Onglet Transactions (résumé par période)
     */
    private void createTransactionsSheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, 
                                        CellStyle dateStyle, CellStyle currencyStyle, ExportFilters filters) {
        Sheet sheet = workbook.createSheet("Transactions");
        
        // En-têtes
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Date", "Type", "Agence", "Collecteur", "Client", "Montant", "Solde Après"
        };
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données (limitées aux 1000 dernières transactions pour performance)
        List<Mouvement> mouvements = getFilteredMouvements(filters);
        int rowNum = 1;
        
        for (Mouvement mouvement : mouvements) {
            Row row = sheet.createRow(rowNum++);
            int cellNum = 0;
            
            createCell(row, cellNum++, mouvement.getDateOperation(), dateStyle);
            createCell(row, cellNum++, mouvement.getSens(), dataStyle);
            createCell(row, cellNum++, getAgenceFromMouvement(mouvement), dataStyle);
            createCell(row, cellNum++, getCollecteurFromMouvement(mouvement), dataStyle);
            createCell(row, cellNum++, getClientFromMouvement(mouvement), dataStyle);
            createCell(row, cellNum++, mouvement.getMontant(), currencyStyle);
            // Note: Solde après non disponible dans le modèle actuel
            createCell(row, cellNum++, 0.0, currencyStyle);
        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 📊 Onglet Résumé/Statistiques
     */
    private void createSummarySheet(Workbook workbook, CellStyle headerStyle, CellStyle dataStyle, 
                                   CellStyle currencyStyle, ExportFilters filters) {
        Sheet sheet = workbook.createSheet("Résumé");
        
        // Informations générales
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("FOCEP - Export Données Système");
        titleCell.setCellStyle(headerStyle);
        
        Row dateRow = sheet.createRow(1);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("Date d'export: " + LocalDateTime.now().format(DATE_FORMATTER));
        dateCell.setCellStyle(dataStyle);

        // Statistiques globales
        int currentRow = 3;
        createSummarySection(sheet, currentRow, "STATISTIQUES GLOBALES", headerStyle, dataStyle, currencyStyle);
        
        // Auto-size
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ================================
    // MÉTHODES UTILITAIRES ET STYLES
    // ================================

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
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("#,##0 \"XAF\""));
        return style;
    }

    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue((LocalDateTime) value);
        } else {
            cell.setCellValue(value.toString());
        }
        
        cell.setCellStyle(style);
    }

    // ================================
    // MÉTHODES DE FILTRAGE DES DONNÉES
    // ================================

    private List<Agence> getFilteredAgences(ExportFilters filters) {
        if (filters.getAgenceId() != null) {
            return agenceRepository.findById(filters.getAgenceId()).map(List::of).orElse(List.of());
        }
        return agenceRepository.findAll();
    }

    private List<Admin> getFilteredAdmins(ExportFilters filters) {
        if (filters.getAgenceId() != null) {
            return adminRepository.findByAgenceId(filters.getAgenceId());
        }
        return adminRepository.findAll();
    }

    private List<Collecteur> getFilteredCollecteurs(ExportFilters filters) {
        if (filters.getAgenceId() != null) {
            return collecteurRepository.findByAgenceId(filters.getAgenceId());
        }
        return collecteurRepository.findAll();
    }

    private List<Client> getFilteredClients(ExportFilters filters) {
        if (filters.getAgenceId() != null) {
            return clientRepository.findByAgenceId(filters.getAgenceId());
        }
        // Limiter à 5000 clients pour performance
        return clientRepository.findTop5000ByOrderByDateCreationDesc();
    }

    private List<Mouvement> getFilteredMouvements(ExportFilters filters) {
        if (filters.getDateDebut() != null && filters.getDateFin() != null) {
            return mouvementRepository.findByDateMouvementBetweenOrderByDateMouvementDesc(
                filters.getDateDebut(), filters.getDateFin());
        }
        // Limiter aux 1000 derniers mouvements pour performance
        return mouvementRepository.findTop1000ByOrderByDateMouvementDesc();
    }

    // ================================
    // MÉTHODES DE CALCUL DES STATISTIQUES
    // ================================

    private Double calculateTotalEpargnesAgence(Long agenceId) {
        return clientRepository.sumSoldesByAgenceId(agenceId);
    }

    private Double calculateTotalEpargnesCollecteur(Long collecteurId) {
        return clientRepository.sumSoldesByCollecteurId(collecteurId);
    }

    private Double calculateSoldeClient(Long clientId) {
        return mouvementRepository.calculateSoldeByClientId(clientId);
    }

    private Long countCollecteursByAdmin(Long adminId) {
        return collecteurRepository.countByAdminId(adminId);
    }

    private Long countClientsByCollecteur(Long collecteurId) {
        return clientRepository.countByCollecteurId(collecteurId);
    }

    private Long countActiveClientsByCollecteur(Long collecteurId) {
        return clientRepository.countByCollecteurIdAndValideTrue(collecteurId);
    }

    private Long countTransactionsByClient(Long clientId) {
        return mouvementRepository.countByClientId(clientId);
    }

    private LocalDateTime getLastTransactionDate(Long clientId) {
        return mouvementRepository.findLastTransactionDateByClientId(clientId);
    }

    private String getAgenceFromMouvement(Mouvement mouvement) {
        if (mouvement.getClient() != null && mouvement.getClient().getAgence() != null) {
            return mouvement.getClient().getAgence().getNomAgence();
        }
        return "Non défini";
    }

    private String getCollecteurFromMouvement(Mouvement mouvement) {
        if (mouvement.getCollecteur() != null) {
            return mouvement.getCollecteur().getNom() + " " + mouvement.getCollecteur().getPrenom();
        } else if (mouvement.getClient() != null && mouvement.getClient().getCollecteur() != null) {
            Collecteur collecteur = mouvement.getClient().getCollecteur();
            return collecteur.getNom() + " " + collecteur.getPrenom();
        }
        return "Non défini";
    }

    private String getClientFromMouvement(Mouvement mouvement) {
        if (mouvement.getClient() != null) {
            return mouvement.getClient().getNom() + " " + mouvement.getClient().getPrenom();
        }
        return "Non défini";
    }

    private void createSummarySection(Sheet sheet, int startRow, String title, 
                                    CellStyle headerStyle, CellStyle dataStyle, CellStyle currencyStyle) {
        Row titleRow = sheet.createRow(startRow);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);

        // Statistiques
        long totalAgences = agenceRepository.count();
        long totalAdmins = adminRepository.count();
        long totalCollecteurs = collecteurRepository.count();
        long totalClients = clientRepository.count();

        String[] labels = {"Nombre d'agences:", "Nombre d'admins:", "Nombre de collecteurs:", "Nombre de clients:"};
        long[] values = {totalAgences, totalAdmins, totalCollecteurs, totalClients};

        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(startRow + 1 + i);
            createCell(row, 0, labels[i], dataStyle);
            createCell(row, 1, values[i], dataStyle);
        }
    }

    /**
     * 🎯 Classe pour les filtres d'export
     */
    public static class ExportFilters {
        private Long agenceId;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;
        private Boolean includeInactifs = true;
        private Integer maxRecords = 10000;

        // Getters et setters
        public Long getAgenceId() { return agenceId; }
        public void setAgenceId(Long agenceId) { this.agenceId = agenceId; }
        
        public LocalDateTime getDateDebut() { return dateDebut; }
        public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
        
        public LocalDateTime getDateFin() { return dateFin; }
        public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
        
        public Boolean getIncludeInactifs() { return includeInactifs; }
        public void setIncludeInactifs(Boolean includeInactifs) { this.includeInactifs = includeInactifs; }
        
        public Integer getMaxRecords() { return maxRecords; }
        public void setMaxRecords(Integer maxRecords) { this.maxRecords = maxRecords; }

        @Override
        public String toString() {
            return String.format("ExportFilters{agenceId=%d, dateDebut=%s, dateFin=%s}", 
                agenceId, dateDebut, dateFin);
        }
    }
}