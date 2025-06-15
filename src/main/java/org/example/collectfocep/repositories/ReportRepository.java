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
     * Trouver les rapports par agence, triés par date de création
     */
    @Query("SELECT r FROM Report r WHERE r.agence.id = :agenceId " +
            "ORDER BY r.dateCreation DESC")
    Page<Report> findByAgenceIdOrderByDateCreationDesc(
            @Param("agenceId") Long agenceId,
            Pageable pageable
    );
    /**
     * Trouver un rapport par ID et agence
     */
    @Query("SELECT r FROM Report r WHERE r.id = :reportId AND r.agence.id = :agenceId")
    Optional<Report> findByIdAndAgenceId(
            @Param("reportId") Long reportId,
            @Param("agenceId") Long agenceId
    );
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
