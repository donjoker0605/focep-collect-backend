package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * ✅ CORRIGER L'ERREUR 404 - RÉCUPÉRER LES RAPPORTS PAR AGENCE
     */
    Page<Report> findByAgenceIdOrderByDateCreationDesc(Long agenceId, Pageable pageable);

    /**
     * ✅ RÉCUPÉRER UN RAPPORT SPÉCIFIQUE AVEC SÉCURITÉ
     */
    Optional<Report> findByIdAndAgenceId(Long reportId, Long agenceId);

    /**
     * ✅ RÉCUPÉRER LES RAPPORTS RÉCENTS
     */
    @Query("SELECT r FROM Report r WHERE r.agence.id = :agenceId AND r.dateCreation >= :dateDebut ORDER BY r.dateCreation DESC")
    List<Report> findRecentReportsByAgence(@Param("agenceId") Long agenceId, @Param("dateDebut") LocalDateTime dateDebut);

    /**
     * ✅ RÉCUPÉRER LES RAPPORTS PAR TYPE
     */
    List<Report> findByAgenceIdAndTypeOrderByDateCreationDesc(Long agenceId, String type);

    /**
     * ✅ RÉCUPÉRER LES RAPPORTS PAR COLLECTEUR
     */
    List<Report> findByCollecteurIdOrderByDateCreationDesc(Long collecteurId);

    /**
     * ✅ COMPTER LES RAPPORTS PAR AGENCE
     */
    long countByAgenceId(Long agenceId);

    /**
     * ✅ COMPTER LES RAPPORTS EN COURS
     */
    long countByAgenceIdAndStatus(Long agenceId, Report.ReportStatus status);
}
