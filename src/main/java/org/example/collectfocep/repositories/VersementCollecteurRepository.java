package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.VersementCollecteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VersementCollecteurRepository extends JpaRepository<VersementCollecteur, Long> {

    Optional<VersementCollecteur> findByCollecteurIdAndDateVersement(Long collecteurId, LocalDate date);

    Page<VersementCollecteur> findByCollecteurIdOrderByDateVersementDesc(Long collecteurId, Pageable pageable);

    @Query("SELECT v FROM VersementCollecteur v WHERE v.collecteur.agence.id = :agenceId ORDER BY v.dateVersement DESC")
    Page<VersementCollecteur> findByAgenceIdOrderByDateVersementDesc(@Param("agenceId") Long agenceId, Pageable pageable);

    @Query("SELECT v FROM VersementCollecteur v WHERE v.dateVersement BETWEEN :dateDebut AND :dateFin ORDER BY v.dateVersement DESC")
    List<VersementCollecteur> findByDateVersementBetween(@Param("dateDebut") LocalDate dateDebut, @Param("dateFin") LocalDate dateFin);

    @Query("SELECT SUM(v.manquant) FROM VersementCollecteur v WHERE v.collecteur.id = :collecteurId AND v.manquant > 0")
    Double calculateTotalManquantByCollecteur(@Param("collecteurId") Long collecteurId);

    @Query("SELECT COUNT(v) FROM VersementCollecteur v WHERE v.collecteur.id = :collecteurId AND v.manquant > 0")
    Long countManquantsByCollecteur(@Param("collecteurId") Long collecteurId);

    boolean existsByNumeroAutorisation(String numeroAutorisation);
}