package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ReportDTO;
import org.example.collectfocep.dto.ReportRequestDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Report;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.ReportRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final AgenceRepository agenceRepository;
    private final CollecteurRepository collecteurRepository;
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

            // TODO: Supprimer aussi le fichier physique si nécessaire
            // deletePhysicalFile(reportOpt.get().getFilePath());

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
        // TODO: Implémenter la génération asynchrone réelle
        // Pour l'instant, marquer comme terminé après 2 secondes

        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simuler le traitement

                // ✅ MARQUER COMME TERMINÉ
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
        // TODO: Utiliser Jackson pour sérialiser les paramètres
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