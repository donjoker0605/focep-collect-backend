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

    // ✅ MÉTHODE PRINCIPALE: Trouver LE journal du jour pour un collecteur
    @Query("SELECT j FROM Journal j WHERE j.collecteur = :collecteur AND j.dateDebut = :date AND j.dateFin = :date")
    Optional<Journal> findByCollecteurAndDate(@Param("collecteur") Collecteur collecteur, @Param("date") LocalDate date);

    // ✅ VERSION AVEC ID DU COLLECTEUR
    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId AND j.dateDebut = :date AND j.dateFin = :date")
    Optional<Journal> findByCollecteurIdAndDate(@Param("collecteurId") Long collecteurId, @Param("date") LocalDate date);

    // ✅ RÉCUPÉRER LE JOURNAL ACTIF (NON CLÔTURÉ) DU JOUR
    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId AND j.dateDebut = :date AND j.dateFin = :date AND j.estCloture = false")
    Optional<Journal> findActiveJournalByCollecteurAndDate(@Param("collecteurId") Long collecteurId, @Param("date") LocalDate date);

    // ✅ RÉCUPÉRER LE JOURNAL ACTIF LE PLUS RÉCENT (FALLBACK)
    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId AND j.estCloture = false ORDER BY j.dateDebut DESC")
    Optional<Journal> findLatestActiveJournalByCollecteur(@Param("collecteurId") Long collecteurId);

    // ✅ CONTRAINTE DE VÉRIFICATION: S'assurer qu'il n'y a qu'un journal par collecteur/jour
    @Query("SELECT COUNT(j) FROM Journal j WHERE j.collecteur = :collecteur AND j.dateDebut = :date AND j.dateFin = :date")
    Long countByCollecteurAndDate(@Param("collecteur") Collecteur collecteur, @Param("date") LocalDate date);

    // MÉTHODES EXISTANTES CONSERVÉES POUR COMPATIBILITÉ
    /**
     * Trouver les journaux par collecteur entre deux dates de début
     */
    @Query("SELECT j FROM Journal j WHERE j.collecteur = :collecteur " +
            "AND j.dateDebut BETWEEN :dateDebut AND :dateFin " +
            "ORDER BY j.dateDebut DESC")
    List<Journal> findByCollecteurAndDateDebutBetween(
            @Param("collecteur") Collecteur collecteur,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("SELECT j FROM Journal j WHERE j.collecteur = :collecteur AND j.dateDebut >= :dateDebut AND j.dateFin <= :dateFin")
    Page<Journal> findByCollecteurAndDateDebutBetween(
            @Param("collecteur") Collecteur collecteur,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            Pageable pageable);

    /**
     * Compter les journaux ouverts pour un collecteur
     */
    @Query("SELECT COUNT(j) FROM Journal j WHERE j.collecteur.id = :collecteurId AND j.estCloture = false")
    long countByCollecteurIdAndEstClotureIsFalse(@Param("collecteurId") Long collecteurId);

    /**
     * Trouver les journaux non clôturés antérieurs à une date donnée
     */
    @Query("SELECT j FROM Journal j WHERE j.estCloture = false AND j.dateDebut < :seuilDate")
    List<Journal> findByEstClotureIsFalseAndDateDebutBefore(@Param("seuilDate") LocalDate seuilDate);



    /**
     * Trouver les journaux par collecteur et plage de dates
     */
    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId " +
            "AND j.dateDebut >= :dateDebut AND j.dateFin <= :dateFin " +
            "ORDER BY j.dateDebut DESC")
    List<Journal> findByCollecteurAndDateRange(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId AND j.dateDebut >= :dateDebut AND j.dateFin <= :dateFin")
    Page<Journal> findByCollecteurAndDateRange(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            Pageable pageable);

    Optional<Journal> findByCollecteurAndDateDebut(Collecteur collecteur, LocalDate dateDebut);

    @Query("SELECT j FROM Journal j WHERE j.collecteur = :collecteur AND j.statut = 'OUVERT' ORDER BY j.dateDebut DESC")
    Optional<Journal> findActiveJournalByCollecteur(@Param("collecteur") Collecteur collecteur);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur.id = :collecteurId")
    Integer countByCollecteurId(@Param("collecteurId") Long collecteurId);

    List<Journal> findByCollecteurIdAndEstClotureFalseOrderByDateDebutDesc(Long collecteurId);

    // ✅ NOUVELLES MÉTHODES POUR AMÉLIORER LA GESTION QUOTIDIENNE

    /**
     * Récupère tous les journaux ouverts (pour nettoyage automatique)
     */
    @Query("SELECT j FROM Journal j WHERE j.estCloture = false AND j.dateDebut < :date")
    List<Journal> findOpenJournalsBeforeDate(@Param("date") LocalDate date);

    /**
     * Récupère les journaux d'un collecteur pour un mois donné
     */
    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId AND YEAR(j.dateDebut) = :year AND MONTH(j.dateDebut) = :month ORDER BY j.dateDebut")
    List<Journal> findByCollecteurAndMonth(@Param("collecteurId") Long collecteurId, @Param("year") int year, @Param("month") int month);

    /**
     * Vérifie si un collecteur a un journal ouvert
     */
    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM Journal j WHERE j.collecteur.id = :collecteurId AND j.estCloture = false")
    boolean hasOpenJournal(@Param("collecteurId") Long collecteurId);

    /**
     * Trouver les journaux par collecteur et période
     */
    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId " +
            "AND j.dateDebut >= :startDate AND j.dateFin <= :endDate " +
            "ORDER BY j.dateDebut DESC")
    List<Journal> findByCollecteurIdAndPeriod(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Trouver les entrées mensuelles pour AsyncReportService
     */
    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId " +
            "AND YEAR(j.dateDebut) = :year AND MONTH(j.dateDebut) = :month " +
            "ORDER BY j.dateDebut DESC")
    List<Journal> findMonthlyEntries(
            @Param("collecteurId") Long collecteurId,
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * Trouver le journal le plus récent d'un collecteur
     */
    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId " +
            "ORDER BY j.dateDebut DESC, j.id DESC")
    Optional<Journal> findTopByCollecteurIdOrderByDateDebutDescIdDesc(@Param("collecteurId") Long collecteurId);

    /**
     * Vérifier si un journal existe pour un collecteur à une date donnée
     */
    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM Journal j " +
            "WHERE j.collecteur.id = :collecteurId AND j.dateDebut = :date")
    boolean existsByCollecteurIdAndDate(@Param("collecteurId") Long collecteurId, @Param("date") LocalDate date);

    @Query("SELECT j FROM Journal j WHERE j.collecteur.id = :collecteurId ORDER BY j.dateDebut DESC")
    Page<Journal> findByCollecteurId(@Param("collecteurId") Long collecteurId, Pageable pageable);

    /**
     * Récupérer la dernière date de clôture d'un collecteur
     */
    @Query("SELECT j.dateDebut FROM Journal j WHERE j.collecteur.id = :collecteurId " +
            "AND j.estCloture = true AND j.dateCloture IS NOT NULL " +
            "ORDER BY j.dateCloture DESC LIMIT 1")
    Optional<LocalDate> findLastClosureDateByCollecteurId(@Param("collecteurId") Long collecteurId);

}