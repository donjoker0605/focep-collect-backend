package org.example.collectfocep.repositories;

import org.example.collectfocep.dto.MouvementProjection;
import org.example.collectfocep.entities.Compte;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MouvementRepository extends JpaRepository<Mouvement, Long> {

    // =====================================
    // REQUÊTES BASIQUES PAR JOURNAL
    // =====================================

    List<Mouvement> findByJournal(Journal journal);
    Page<Mouvement> findByJournal(Journal journal, Pageable pageable);

    /**
     * Trouve les mouvements par journal
     */
    @Query("SELECT m FROM Mouvement m WHERE m.journal.id = :journalId ORDER BY m.dateOperation DESC")
    List<Mouvement> findByJournalId(@Param("journalId") Long journalId);

    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.journal = :journal")
    long countByJournal(@Param("journal") Journal journal);

    void deleteByJournal(Journal journal);

    // =====================================
    // REQUÊTES PAR COMPTE
    // =====================================

    @Query("SELECT m FROM Mouvement m WHERE m.compteSource = :compte OR m.compteDestination = :compte")
    List<Mouvement> findByCompte(@Param("compte") Compte compte);

    @Query("SELECT m FROM Mouvement m WHERE m.compteSource = :compte OR m.compteDestination = :compte")
    Page<Mouvement> findByCompte(@Param("compte") Compte compte, Pageable pageable);

    @Query("SELECT SUM(m.montant) FROM Mouvement m WHERE m.compteSource = :compte AND m.sens = 'DEBIT'")
    Double sumDebits(@Param("compte") Compte compte);

    @Query("SELECT SUM(m.montant) FROM Mouvement m WHERE m.compteDestination = :compte AND m.sens = 'CREDIT'")
    Double sumCredits(@Param("compte") Compte compte);

    // =====================================
    // REQUÊTES PAR COLLECTEUR (DIRECTES)
    // =====================================

    @Query("SELECT m FROM Mouvement m WHERE m.collecteur.id = :collecteurId")
    List<Mouvement> findByCollecteurId(@Param("collecteurId") Long collecteurId);

    /**
     * Trouve les mouvements récents d'un collecteur
     */
    @Query("""
        SELECT m FROM Mouvement m
        LEFT JOIN m.compteDestination cd
        LEFT JOIN m.compteSource cs
        LEFT JOIN CompteClient ccd ON cd.id = ccd.id
        LEFT JOIN CompteClient ccs ON cs.id = ccs.id
        WHERE (ccd.client.collecteur.id = :collecteurId OR ccs.client.collecteur.id = :collecteurId)
        ORDER BY m.dateOperation DESC
        """)
    Page<Mouvement> findByCollecteurId(@Param("collecteurId") Long collecteurId, Pageable pageable);

    // =====================================
    // REQUÊTES DE COMPTAGE ET SOMMES PAR COLLECTEUR
    // =====================================

    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    Long countByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Calcule la somme des montants par collecteur et type de mouvement
     */
    @Query("""
        SELECT COALESCE(SUM(m.montant), 0.0)
        FROM Mouvement m
        LEFT JOIN m.compteDestination cd
        LEFT JOIN m.compteSource cs
        LEFT JOIN CompteClient ccd ON cd.id = ccd.id
        LEFT JOIN CompteClient ccs ON cs.id = ccs.id
        WHERE (ccd.client.collecteur.id = :collecteurId OR ccs.client.collecteur.id = :collecteurId)
        AND m.sens = :type
        """)
    Double sumMontantByCollecteurAndType(
            @Param("collecteurId") Long collecteurId,
            @Param("type") String type);

    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId AND m.sens = :sens")
    Double sumMontantByCollecteurAndSens(
            @Param("collecteurId") Long collecteurId,
            @Param("sens") String sens
    );

    // =====================================
    // REQUÊTES SPÉCIALISÉES PAR TYPE DE MOUVEMENT
    // =====================================

    /**
     * Calcule la somme des épargnes pour un collecteur sur une période
     */
    @Query("""
        SELECT COALESCE(SUM(m.montant), 0.0)
        FROM Mouvement m
        JOIN m.compteDestination cd
        JOIN CompteClient cc ON cd.id = cc.id
        JOIN cc.client c
        WHERE c.collecteur.id = :collecteurId
        AND m.sens = 'epargne'
        AND m.dateOperation BETWEEN :startDate AND :endDate
        """)
    Double sumEpargneByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Calcule la somme des retraits pour un collecteur sur une période
     */
    @Query("""
        SELECT COALESCE(SUM(m.montant), 0.0)
        FROM Mouvement m
        JOIN m.compteSource cs
        JOIN CompteClient cc ON cs.id = cc.id
        JOIN cc.client c
        WHERE c.collecteur.id = :collecteurId
        AND m.sens = 'retrait'
        AND m.dateOperation BETWEEN :startDate AND :endDate
        """)
    Double sumRetraitByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // =====================================
    // REQUÊTES PAR DATE (LocalDate)
    // =====================================

    /**
     * Compte les mouvements par collecteur et date
     */
    @Query("""
        SELECT COUNT(m)
        FROM Mouvement m
        LEFT JOIN m.compteDestination cd
        LEFT JOIN m.compteSource cs
        LEFT JOIN CompteClient ccd ON cd.id = ccd.id
        LEFT JOIN CompteClient ccs ON cs.id = ccs.id
        WHERE (ccd.client.collecteur.id = :collecteurId OR ccs.client.collecteur.id = :collecteurId)
        AND DATE(m.dateOperation) = :date
        """)
    Long countByCollecteurAndDate(
            @Param("collecteurId") Long collecteurId,
            @Param("date") LocalDate date);

    /**
     * Calcule la somme des montants par collecteur, type et date
     */
    @Query("""
        SELECT COALESCE(SUM(m.montant), 0.0)
        FROM Mouvement m
        LEFT JOIN m.compteDestination cd
        LEFT JOIN m.compteSource cs
        LEFT JOIN CompteClient ccd ON cd.id = ccd.id
        LEFT JOIN CompteClient ccs ON cs.id = ccs.id
        WHERE (ccd.client.collecteur.id = :collecteurId OR ccs.client.collecteur.id = :collecteurId)
        AND m.sens = :type
        AND DATE(m.dateOperation) = :date
        """)
    Double sumMontantByCollecteurAndTypeAndDate(
            @Param("collecteurId") Long collecteurId,
            @Param("type") String type,
            @Param("date") LocalDate date);

    /**
     * Calcule la somme des montants par collecteur, type et plage de dates
     */
    @Query("""
        SELECT COALESCE(SUM(m.montant), 0.0)
        FROM Mouvement m
        LEFT JOIN m.compteDestination cd
        LEFT JOIN m.compteSource cs
        LEFT JOIN CompteClient ccd ON cd.id = ccd.id
        LEFT JOIN CompteClient ccs ON cs.id = ccs.id
        WHERE (ccd.client.collecteur.id = :collecteurId OR ccs.client.collecteur.id = :collecteurId)
        AND m.sens = :type
        AND DATE(m.dateOperation) BETWEEN :startDate AND :endDate
        """)
    Double sumMontantByCollecteurAndTypeAndDateRange(
            @Param("collecteurId") Long collecteurId,
            @Param("type") String type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Compte les mouvements par collecteur et plage de dates
     */
    @Query("""
        SELECT COUNT(m)
        FROM Mouvement m
        LEFT JOIN m.compteDestination cd
        LEFT JOIN m.compteSource cs
        LEFT JOIN CompteClient ccd ON cd.id = ccd.id
        LEFT JOIN CompteClient ccs ON cs.id = ccs.id
        WHERE (ccd.client.collecteur.id = :collecteurId OR ccs.client.collecteur.id = :collecteurId)
        AND DATE(m.dateOperation) BETWEEN :startDate AND :endDate
        """)
    Long countByCollecteurAndDateRange(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // =====================================
    // REQUÊTES PAR CLIENT
    // =====================================

    /**
     * Trouve les mouvements par client
     */
    @Query("""
        SELECT m FROM Mouvement m
        LEFT JOIN m.compteDestination cd
        LEFT JOIN m.compteSource cs
        LEFT JOIN CompteClient ccd ON cd.id = ccd.id
        LEFT JOIN CompteClient ccs ON cs.id = ccs.id
        WHERE ccd.client.id = :clientId OR ccs.client.id = :clientId
        ORDER BY m.dateOperation DESC
        """)
    List<Mouvement> findByClientId(@Param("clientId") Long clientId);

    /**
     * Trouve les mouvements par type de mouvement
     */
    List<Mouvement> findBySensOrderByDateOperationDesc(String sens);

    @Query("SELECT m FROM Mouvement m WHERE m.client.id = :clientId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    List<Mouvement> findByClientIdAndDateRange(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT m FROM Mouvement m WHERE m.client.id IN :clientIds " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "ORDER BY m.client.id, m.dateOperation")
    List<Mouvement> findByClientIdsAndDateRange(
            @Param("clientIds") List<Long> clientIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // =====================================
    // REQUÊTES DE DATES ET PÉRIODES
    // =====================================

    /**
     * Trouve les mouvements entre deux dates
     */
    @Query("""
        SELECT m FROM Mouvement m
        WHERE m.dateOperation BETWEEN :startDate AND :endDate
        ORDER BY m.dateOperation DESC
        """)
    List<Mouvement> findByDateOperationBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT m FROM Mouvement m WHERE m.dateOperation BETWEEN :debut AND :fin")
    Page<Mouvement> findByDateOperationBetween(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin,
            Pageable pageable
    );

    // =====================================
    // REQUÊTES POUR COMMISSIONS ET BUSINESS
    // =====================================

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "JOIN CompteCollecteur cc ON m.compteSource.id = cc.id " +
            "WHERE m.compteSource.id = :compteId " +
            "AND cc.collecteur.id = :collecteurId " +
            "AND m.libelle LIKE '%commission%' " +
            "AND m.dateOperation > :dateLimit")
    double calculatePendingCommissions(
            @Param("compteId") Long compteId,
            @Param("collecteurId") Long collecteurId,
            @Param("dateLimit") LocalDateTime dateLimit
    );

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "AND m.sens = 'CREDIT'")
    double sumAmountByClientAndPeriod(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // =====================================
    // REQUÊTES POUR TRANSFERTS
    // =====================================

    @Query("SELECT m FROM Mouvement m WHERE m.transfert.id = :transferId")
    List<Mouvement> findByTransfertId(@Param("transferId") Long transferId);

    // =====================================
    // REQUÊTES OPTIMISÉES AVEC FETCH
    // =====================================

    /**
     * Trouve les mouvements par journal avec les comptes (JOIN FETCH pour éviter N+1)
     */
    @Query("""
        SELECT m FROM Mouvement m
        LEFT JOIN FETCH m.compteSource
        LEFT JOIN FETCH m.compteDestination
        WHERE m.journal.id = :journalId
        ORDER BY m.dateOperation DESC
        """)
    List<Mouvement> findByJournalIdWithAccounts(@Param("journalId") Long journalId);

    /**
     * Récupère les projections des mouvements par journal (optimisé)
     */
    @Query("""
    SELECT m.id as id,
           m.montant as montant,
           m.libelle as libelle,
           m.sens as sens,
           m.dateOperation as dateOperation,
           COALESCE(cs.numeroCompte, '') as compteSourceNumero,
           COALESCE(cd.numeroCompte, '') as compteDestinationNumero
    FROM Mouvement m
    LEFT JOIN m.compteSource cs
    LEFT JOIN m.compteDestination cd
    WHERE m.journal.id = :journalId
    ORDER BY m.dateOperation DESC
    """)
    List<MouvementProjection> findMouvementProjectionsByJournalId(@Param("journalId") Long journalId);

    @Query("""
    SELECT m FROM Mouvement m
    LEFT JOIN FETCH m.compteSource cs
    LEFT JOIN FETCH m.compteDestination cd
    LEFT JOIN FETCH m.client c
    LEFT JOIN FETCH m.collecteur col
    WHERE m.journal.id = :journalId
    ORDER BY m.dateOperation DESC
    """)
    List<Mouvement> findMouvementsWithDetailsByJournalId(@Param("journalId") Long journalId);

    // =====================================
    // REQUÊTES DE RECHERCHE ET FILTRAGE
    // =====================================

    List<Mouvement> findByLibelleContaining(String keyword);

    @Query("SELECT m FROM Mouvement m WHERE m.journal = :journal AND m.sens = :sens")
    List<Mouvement> findByJournalAndSens(@Param("journal") Journal journal, @Param("sens") String sens);

    /**
     * Trouve les mouvements récents d'un collecteur avec limite
     */
    @Query("""
        SELECT m FROM Mouvement m
        LEFT JOIN m.compteDestination cd
        LEFT JOIN m.compteSource cs
        LEFT JOIN CompteClient ccd ON cd.id = ccd.id
        LEFT JOIN CompteClient ccs ON cs.id = ccs.id
        WHERE (ccd.client.collecteur.id = :collecteurId OR ccs.client.collecteur.id = :collecteurId)
        ORDER BY m.dateOperation DESC
        """)
    List<Mouvement> findRecentByCollecteur(@Param("collecteurId") Long collecteurId, Pageable pageable);

    // ✅ MÉTHODES DE COMPATIBILITÉ POUR L'EXISTANT

    /**
     * Trouve tous les mouvements par journal (méthode simple)
     */
    List<Mouvement> findByJournalIdOrderByDateOperationDesc(Long journalId);

    // =====================================
    // REQUÊTES PAR COLLECTEUR ET DATE - VERSION CORRIGÉE
    // =====================================

    @Query("""
    SELECT m FROM Mouvement m
    LEFT JOIN FETCH m.compteSource cs
    LEFT JOIN FETCH m.compteDestination cd
    LEFT JOIN FETCH m.client c
    LEFT JOIN FETCH m.collecteur col
    LEFT JOIN FETCH m.journal j
    WHERE m.collecteur.id = :collecteurId
    AND m.dateOperation BETWEEN :startDate AND :endDate
    ORDER BY m.dateOperation DESC
    """)
    List<Mouvement> findByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Trouve les mouvements par collecteur et date avec pagination
     */
    @Query("""
        SELECT m FROM Mouvement m
        LEFT JOIN m.compteDestination cd
        LEFT JOIN m.compteSource cs
        LEFT JOIN CompteClient ccd ON cd.id = ccd.id
        LEFT JOIN CompteClient ccs ON cs.id = ccs.id
        WHERE (ccd.client.collecteur.id = :collecteurId OR ccs.client.collecteur.id = :collecteurId)
        AND m.dateOperation BETWEEN :startDate AND :endDate
        ORDER BY m.dateOperation DESC
        """)
    Page<Mouvement> findByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // =====================================
    // REQUÊTES PAR COLLECTEUR ET DATE AVEC LocalDateTime - ✅ NOUVELLE MÉTHODE CORRIGÉE
    // =====================================

    /**
     * ✅ CORRECTION CRITIQUE: Utilise LocalDateTime au lieu de String pour les dates
     * Cette méthode remplace l'ancienne qui utilisait String date et parsing manuel
     */
    @Query("""
    SELECT m FROM Mouvement m 
    WHERE m.collecteur.id = :collecteurId 
    AND m.dateOperation BETWEEN :startOfDay AND :endOfDay
    ORDER BY m.dateOperation DESC
    """)
    Page<Mouvement> findByCollecteurAndDate(
            @Param("collecteurId") Long collecteurId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable pageable
    );

    @Query("""
    SELECT m FROM Mouvement m
    LEFT JOIN FETCH m.client c
    LEFT JOIN FETCH m.collecteur col
    LEFT JOIN FETCH m.compteSource cs
    LEFT JOIN FETCH m.compteDestination cd
    LEFT JOIN FETCH m.journal j
    WHERE m.id = :mouvementId
    """)
    Optional<Mouvement> findByIdWithAllRelations(@Param("mouvementId") Long mouvementId);

    /**
     * CORRECTION CRITIQUE: Récupère les mouvements d'un client avec toutes les relations
     */
    @Query("""
    SELECT m FROM Mouvement m
    LEFT JOIN FETCH m.client c
    LEFT JOIN FETCH m.collecteur col
    LEFT JOIN FETCH m.compteSource cs
    LEFT JOIN FETCH m.compteDestination cd
    LEFT JOIN FETCH m.journal j
    WHERE m.client.id = :clientId
    ORDER BY m.dateOperation DESC
    """)
    List<Mouvement> findByClientIdWithAllRelations(@Param("clientId") Long clientId);

    // =====================================
    // ✅ NOUVELLES MÉTHODES AVEC DateTimeService SUPPORT
    // =====================================

    /**
     * Méthode optimisée pour rechercher par collecteur et plage de dates (LocalDateTime)
     * Compatible avec DateTimeService
     */
    @Query("""
    SELECT m FROM Mouvement m
    LEFT JOIN FETCH m.client c
    LEFT JOIN FETCH m.collecteur col
    WHERE m.collecteur.id = :collecteurId
    AND m.dateOperation BETWEEN :startDateTime AND :endDateTime
    ORDER BY m.dateOperation DESC
    """)
    List<Mouvement> findByCollecteurIdAndDateTimeBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Version paginée pour rechercher par collecteur et plage de dates
     */
    @Query("""
    SELECT m FROM Mouvement m
    LEFT JOIN FETCH m.client c
    LEFT JOIN FETCH m.collecteur col
    WHERE m.collecteur.id = :collecteurId
    AND m.dateOperation BETWEEN :startDateTime AND :endDateTime
    ORDER BY m.dateOperation DESC
    """)
    Page<Mouvement> findByCollecteurIdAndDateTimeBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable);

    /**
     * Statistiques de mouvements par collecteur et période
     */
    @Query("""
    SELECT 
        COUNT(m) as totalTransactions,
        COALESCE(SUM(CASE WHEN m.sens = 'epargne' THEN m.montant ELSE 0 END), 0) as totalEpargne,
        COALESCE(SUM(CASE WHEN m.sens = 'retrait' THEN m.montant ELSE 0 END), 0) as totalRetraits,
        COUNT(CASE WHEN m.sens = 'epargne' THEN 1 END) as nombreEpargnes,
        COUNT(CASE WHEN m.sens = 'retrait' THEN 1 END) as nombreRetraits
    FROM Mouvement m
    WHERE m.collecteur.id = :collecteurId
    AND m.dateOperation BETWEEN :startDateTime AND :endDateTime
    """)
    Object[] getStatsByCollecteurAndPeriod(
            @Param("collecteurId") Long collecteurId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Récupère les mouvements par client et période (pour le solde)
     */
    @Query("""
    SELECT m FROM Mouvement m
    WHERE m.client.id = :clientId
    AND m.dateOperation BETWEEN :startDateTime AND :endDateTime
    ORDER BY m.dateOperation ASC
    """)
    List<Mouvement> findByClientIdAndPeriod(
            @Param("clientId") Long clientId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Recherche de transactions par collecteur pour un jour spécifique
     * Utilise LocalDateTime pour une meilleure précision
     */
    @Query("""
    SELECT m FROM Mouvement m
    LEFT JOIN FETCH m.client c
    LEFT JOIN FETCH m.journal j
    WHERE m.collecteur.id = :collecteurId
    AND m.dateOperation >= :dayStart
    AND m.dateOperation < :dayEnd
    ORDER BY m.dateOperation DESC
    """)
    List<Mouvement> findByCollecteurAndDay(
            @Param("collecteurId") Long collecteurId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}