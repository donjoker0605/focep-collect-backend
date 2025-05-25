package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
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
public interface JournalRepository extends JpaRepository<Journal, Long> {
    // Méthode avec objet collecteur
    @Query("SELECT j FROM Journal j WHERE j.collecteur = :collecteur AND j.dateDebut >= :dateDebut AND j.dateFin <= :dateFin")
    List<Journal> findByCollecteurAndDateDebutBetween(
            @Param("collecteur") Collecteur collecteur,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    // Méthode avec objet collecteur et pagination
    @Query("SELECT j FROM Journal j WHERE j.collecteur = :collecteur AND j.dateDebut >= :dateDebut AND j.dateFin <= :dateFin")
    Page<Journal> findByCollecteurAndDateDebutBetween(
            @Param("collecteur") Collecteur collecteur,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            Pageable pageable);

    // Méthodes ajoutées pour accepter l'ID du collecteur directement
    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId AND j.dateDebut >= :dateDebut AND j.dateFin <= :dateFin")
    List<Journal> findByCollecteurAndDateRange(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId AND j.dateDebut >= :dateDebut AND j.dateFin <= :dateFin")
    Page<Journal> findByCollecteurAndDateRange(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            Pageable pageable);

    Optional<Journal> findByCollecteurAndDateDebut(Collecteur collecteur, LocalDate dateDebut);

    @Query("SELECT j FROM Journal j WHERE j.collecteur = :collecteur AND j.statut = 'OUVERT' ORDER BY j.dateDebut DESC")
    Optional<Journal> findActiveJournalByCollecteur(@Param("collecteur") Collecteur collecteur);

    /**
     * Compter les clients d'un collecteur
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur.id = :collecteurId")
    Integer countByCollecteurId(@Param("collecteurId") Long collecteurId);
}