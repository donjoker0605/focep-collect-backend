package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.HistoriqueRemuneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoriqueRemunerationRepository extends JpaRepository<HistoriqueRemuneration, Long> {
    
    /**
     * Vérifie si une rémunération existe déjà pour un collecteur sur une période donnée
     */
    @Query("SELECT hr FROM HistoriqueRemuneration hr WHERE " +
           "hr.collecteur.id = :collecteurId AND " +
           "hr.dateDebutPeriode = :dateDebut AND " +
           "hr.dateFinPeriode = :dateFin")
    Optional<HistoriqueRemuneration> findByCollecteurAndPeriode(
        @Param("collecteurId") Long collecteurId,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin
    );
    
    /**
     * Récupère l'historique des rémunérations d'un collecteur, trié par date décroissante
     */
    @Query("SELECT hr FROM HistoriqueRemuneration hr WHERE " +
           "hr.collecteur.id = :collecteurId " +
           "ORDER BY hr.dateRemuneration DESC")
    List<HistoriqueRemuneration> findByCollecteurOrderByDateDesc(@Param("collecteurId") Long collecteurId);
    
    /**
     * Vérifie si un chevauchement de période existe pour un collecteur
     */
    @Query("SELECT COUNT(hr) > 0 FROM HistoriqueRemuneration hr WHERE " +
           "hr.collecteur.id = :collecteurId AND (" +
           "(hr.dateDebutPeriode <= :dateFin AND hr.dateFinPeriode >= :dateDebut))")
    boolean existsOverlappingPeriod(
        @Param("collecteurId") Long collecteurId,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin
    );
}