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
    // MÉTHODES PAR JOURNAL
    // =====================================

    List<Mouvement> findByJournal(Journal journal);
    Page<Mouvement> findByJournal(Journal journal, Pageable pageable);

    @Query("SELECT m FROM Mouvement m WHERE m.journal.id = :journalId ORDER BY m.dateOperation DESC")
    List<Mouvement> findByJournalId(@Param("journalId") Long journalId);

    @Query("SELECT m FROM Mouvement m WHERE m.journal.id = :journalId ORDER BY m.dateOperation DESC")
    List<Mouvement> findByJournalIdOrderByDateOperationDesc(@Param("journalId") Long journalId);

    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.journal = :journal")
    long countByJournal(@Param("journal") Journal journal);

    void deleteByJournal(Journal journal);

    // =====================================
    // MÉTHODES PAR JOURNAL AVEC OPTIMISATIONS
    // =====================================

    /**
     * Mouvements par journal avec comptes (optimisé avec FETCH)
     */
    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.compteSource " +
            "LEFT JOIN FETCH m.compteDestination " +
            "WHERE m.journal.id = :journalId " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findByJournalIdWithAccounts(@Param("journalId") Long journalId);

    @Query("SELECT " +
            "COUNT(m) as totalOperations, " +
            "COALESCE(SUM(m.montant), 0) as montantTotal, " +
            "COALESCE(SUM(CASE WHEN UPPER(m.sens) = 'EPARGNE' THEN m.montant ELSE 0 END), 0) as totalEpargne, " +
            "COALESCE(SUM(CASE WHEN UPPER(m.sens) = 'RETRAIT' THEN m.montant ELSE 0 END), 0) as totalRetrait " +
            "FROM Mouvement m")
    Object[] getGlobalStats();


    /**
     * Statistiques par agence
     */
    @Query("SELECT " +
            "COUNT(m) as totalOperations, " +
            "COALESCE(SUM(m.montant), 0) as montantTotal, " +
            "COALESCE(SUM(CASE WHEN UPPER(m.sens) = 'EPARGNE' THEN m.montant ELSE 0 END), 0) as totalEpargne, " +
            "COALESCE(SUM(CASE WHEN UPPER(m.sens) = 'RETRAIT' THEN m.montant ELSE 0 END), 0) as totalRetrait " +
            "FROM Mouvement m " +
            "WHERE m.client.agence.id = :agenceId")
    Object[] getStatsByAgence(@Param("agenceId") Long agenceId);


    /**
     * Mouvements récents pour dashboard
     */
    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.client " +
            "LEFT JOIN FETCH m.collecteur " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findRecentMovements(Pageable pageable);


    /**
     * Top collecteurs par volume
     */
    @Query("SELECT m.collecteur.id, " +
            "COUNT(m) as nombreOperations, " +
            "COALESCE(SUM(m.montant), 0) as montantTotal " +
            "FROM Mouvement m " +
            "GROUP BY m.collecteur.id " +
            "ORDER BY montantTotal DESC")
    List<Object[]> getTopCollecteursByVolume(Pageable pageable);


    /**
     * Projections optimisées pour l'affichage
     */
    @Query("SELECT m.id as id, " +
            "m.montant as montant, " +
            "m.libelle as libelle, " +
            "m.sens as sens, " +
            "m.dateOperation as dateOperation, " +
            "COALESCE(cs.numeroCompte, '') as compteSourceNumero, " +
            "COALESCE(cd.numeroCompte, '') as compteDestinationNumero " +
            "FROM Mouvement m " +
            "LEFT JOIN m.compteSource cs " +
            "LEFT JOIN m.compteDestination cd " +
            "WHERE m.journal.id = :journalId " +
            "ORDER BY m.dateOperation DESC")
    List<MouvementProjection> findMouvementProjectionsByJournalId(@Param("journalId") Long journalId);

    /**
     * Mouvements avec tous les détails
     */
    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.compteSource cs " +
            "LEFT JOIN FETCH m.compteDestination cd " +
            "LEFT JOIN FETCH m.client c " +
            "LEFT JOIN FETCH m.collecteur col " +
            "WHERE m.journal.id = :journalId " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findMouvementsWithDetailsByJournalId(@Param("journalId") Long journalId);

    // =====================================
    // MÉTHODES PAR COMPTE
    // =====================================

    @Query("SELECT m FROM Mouvement m WHERE m.compteSource = :compte OR m.compteDestination = :compte")
    List<Mouvement> findByCompte(@Param("compte") Compte compte);

    @Query("SELECT m FROM Mouvement m WHERE m.compteSource = :compte OR m.compteDestination = :compte")
    Page<Mouvement> findByCompte(@Param("compte") Compte compte, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m WHERE m.compteSource = :compte AND m.sens = 'DEBIT'")
    Double sumDebits(@Param("compte") Compte compte);

    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m WHERE m.compteDestination = :compte AND m.sens = 'CREDIT'")
    Double sumCredits(@Param("compte") Compte compte);

    // =====================================
    // MÉTHODES PAR COLLECTEUR - OPTIMISÉES
    // =====================================

    /**
     * Mouvements par collecteur (méthode principale)
     */
    @Query("SELECT m FROM Mouvement m WHERE m.collecteur.id = :collecteurId ORDER BY m.dateOperation DESC")
    List<Mouvement> findByCollecteurId(@Param("collecteurId") Long collecteurId);

    @Query("SELECT m FROM Mouvement m WHERE m.collecteur.id = :collecteurId ORDER BY m.dateOperation DESC")
    Page<Mouvement> findByCollecteurId(@Param("collecteurId") Long collecteurId, Pageable pageable);

    /**
     * Mouvements récents par collecteur
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findRecentByCollecteur(@Param("collecteurId") Long collecteurId, Pageable pageable);

    // =====================================
    // MÉTHODES PAR CLIENT
    // =====================================

    /**
     * Mouvements par client (méthode principale)
     */
    Page<Mouvement> findByClientIdOrderByDateOperationDesc(Long clientId, Pageable pageable);

    @Query("SELECT m FROM Mouvement m WHERE m.client.id = :clientId ORDER BY m.dateOperation DESC")
    List<Mouvement> findByClientId(@Param("clientId") Long clientId);

    /**
     *  Recherche paginée par client
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "ORDER BY m.dateOperation DESC")
    Page<Mouvement> findByClientId(@Param("clientId") Long clientId, Pageable pageable);

    /**
     *  Recherche avec filtres multiples
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND (:type IS NULL OR UPPER(m.sens) = UPPER(:type) OR UPPER(m.typeMouvement) = UPPER(:type)) " +
            "AND (:dateDebut IS NULL OR DATE(m.dateOperation) >= :dateDebut) " +
            "AND (:dateFin IS NULL OR DATE(m.dateOperation) <= :dateFin)")
    Page<Mouvement> findByClientIdWithFilters(
            @Param("clientId") Long clientId,
            @Param("type") String type,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            Pageable pageable
    );

    /**
     * Statistiques par client et période
     */
    @Query("SELECT " +
            "SUM(CASE WHEN UPPER(m.sens) = 'EPARGNE' OR UPPER(m.typeMouvement) = 'EPARGNE' THEN m.montant ELSE 0 END) as totalEpargne, " +
            "SUM(CASE WHEN UPPER(m.sens) = 'RETRAIT' OR UPPER(m.typeMouvement) = 'RETRAIT' THEN m.montant ELSE 0 END) as totalRetraits, " +
            "COUNT(m) as nombreTransactions " +
            "FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND (:dateDebut IS NULL OR DATE(m.dateOperation) >= :dateDebut) " +
            "AND (:dateFin IS NULL OR DATE(m.dateOperation) <= :dateFin)")
    Object[] getClientStatistics(
            @Param("clientId") Long clientId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     *  Calcul du solde d'un client à une date précise
     */
    @Query("SELECT " +
            "SUM(CASE WHEN UPPER(m.sens) = 'EPARGNE' OR UPPER(m.typeMouvement) = 'EPARGNE' THEN m.montant " +
            "          WHEN UPPER(m.sens) = 'RETRAIT' OR UPPER(m.typeMouvement) = 'RETRAIT' THEN -m.montant " +
            "          ELSE 0 END) " +
            "FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND DATE(m.dateOperation) <= :date")
    Double calculateClientBalanceAtDate(
            @Param("clientId") Long clientId,
            @Param("date") LocalDate date
    );

    /**
     *  Transactions d'un client par mois (pour graphiques)
     */
    @Query("SELECT " +
            "YEAR(m.dateOperation) as annee, " +
            "MONTH(m.dateOperation) as mois, " +
            "SUM(CASE WHEN UPPER(m.sens) = 'EPARGNE' OR UPPER(m.typeMouvement) = 'EPARGNE' THEN m.montant ELSE 0 END) as epargne, " +
            "SUM(CASE WHEN UPPER(m.sens) = 'RETRAIT' OR UPPER(m.typeMouvement) = 'RETRAIT' THEN m.montant ELSE 0 END) as retraits " +
            "FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND m.dateOperation >= :dateDebut " +
            "GROUP BY YEAR(m.dateOperation), MONTH(m.dateOperation) " +
            "ORDER BY YEAR(m.dateOperation) DESC, MONTH(m.dateOperation) DESC")  // ✅ CORRECTION
    List<Object[]> getClientMonthlyStats(
            @Param("clientId") Long clientId,
            @Param("dateDebut") LocalDateTime dateDebut
    );

    /**
     *  Dernière transaction d'un client
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "ORDER BY m.dateOperation DESC " +
            "LIMIT 1")
    Optional<Mouvement> findLastTransactionByClientId(@Param("clientId") Long clientId);

    /**
     * Mouvements par client avec toutes les relations
     */
    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.client c " +
            "LEFT JOIN FETCH m.collecteur col " +
            "LEFT JOIN FETCH col.agence a" +
            "LEFT JOIN FETCH m.compteSource cs " +
            "LEFT JOIN FETCH m.compteDestination cd " +
            "LEFT JOIN FETCH m.journal j " +
            "WHERE m.client.id = :clientId " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findByClientIdWithAllRelations(@Param("clientId") Long clientId);

    // =====================================
    // MÉTHODES PAR DATES ET PÉRIODES
    // =====================================

    /**
     * Mouvements par période
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND m.dateOperation BETWEEN :dateDebut AND :dateFin " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findByClientIdAndDateOperationBetween(
            @Param("clientId") Long clientId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin
    );

    /**
     * Mouvements entre deux dates
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.dateOperation BETWEEN :startDate AND :endDate " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findByDateOperationBetween(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.dateOperation BETWEEN :debut AND :fin " +
            "ORDER BY m.dateOperation DESC")
    Page<Mouvement> findByDateOperationBetween(@Param("debut") LocalDateTime debut,
                                               @Param("fin") LocalDateTime fin,
                                               Pageable pageable);

    /**
     * Comptage par dates
     */
    long countByDateOperationBetween(LocalDateTime dateDebut, LocalDateTime dateFin);

    // =====================================
    // MÉTHODES PAR COLLECTEUR ET DATES - CORRIGÉES
    // =====================================

    /**
     * Mouvements par collecteur et période
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Mouvements d'un collecteur par période
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :dateDebut AND :dateFin " +
            "ORDER BY m.dateOperation DESC")
    Page<Mouvement> findByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            Pageable pageable
    );

    /**
     * Mouvements par collecteur pour un jour spécifique
     */
    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.client c " +
            "LEFT JOIN FETCH m.journal j " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation >= :dayStart " +
            "AND m.dateOperation < :dayEnd " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findByCollecteurAndDay(@Param("collecteurId") Long collecteurId,
                                           @Param("dayStart") LocalDateTime dayStart,
                                           @Param("dayEnd") LocalDateTime dayEnd);

    /**
     * Méthode alternative pour collecteur et date (avec pagination)
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startOfDay AND :endOfDay " +
            "ORDER BY m.dateOperation DESC")
    Page<Mouvement> findByCollecteurAndDate(@Param("collecteurId") Long collecteurId,
                                            @Param("startOfDay") LocalDateTime startOfDay,
                                            @Param("endOfDay") LocalDateTime endOfDay,
                                            Pageable pageable);

    // =====================================
    // MÉTHODES DE COMPTAGE ET SOMMES
    // =====================================

    /**
     * Comptage par collecteur et période
     */
    @Query("SELECT COUNT(m) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    Long countByCollecteurIdAndDateOperationBetween(@Param("collecteurId") Long collecteurId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Somme par collecteur et sens
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId AND m.sens = :sens")
    Double sumMontantByCollecteurAndSens(@Param("collecteurId") Long collecteurId,
                                         @Param("sens") String sens);

    /**
     * Somme par sens et période (pour dashboard admin)
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.sens = :sens " +
            "AND m.dateOperation BETWEEN :dateDebut AND :dateFin")
    Double sumMontantBySensAndDateBetween(@Param("sens") String sens,
                                          @Param("dateDebut") LocalDateTime dateDebut,
                                          @Param("dateFin") LocalDateTime dateFin);

    /**
     * Somme par collecteur et type
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId AND m.sens = :type")
    Double sumMontantByCollecteurAndType(@Param("collecteurId") Long collecteurId,
                                         @Param("type") String type);

    // =====================================
    // STATISTIQUES SPÉCIALISÉES PAR TYPE
    // =====================================

    /**
     * Somme épargne par collecteur et période
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.client.collecteur.id = :collecteurId " +
            "AND UPPER(m.sens) = 'EPARGNE' " +
            "AND m.dateOperation BETWEEN :dateDebut AND :dateFin")
    Double sumEpargneByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin
    );

    @Query("SELECT SUM(m.montant) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND (UPPER(m.sens) = 'EPARGNE' OR UPPER(m.typeMouvement) = 'EPARGNE') " +
            "AND DATE(m.dateOperation) = :date")
    Double sumEpargneByCollecteurAndDate(@Param("collecteurId") Long collecteurId,
                                         @Param("date") LocalDate date);

    @Query("SELECT SUM(m.montant) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND (UPPER(m.sens) = 'RETRAIT' OR UPPER(m.typeMouvement) = 'RETRAIT') " +
            "AND DATE(m.dateOperation) = :date")
    Double sumRetraitsByCollecteurAndDate(@Param("collecteurId") Long collecteurId,
                                          @Param("date") LocalDate date);

    @Query("SELECT m.collecteur.id, SUM(m.montant) as total " +
            "FROM Mouvement m " +
            "WHERE m.collecteur.agence.id = :agenceId " +
            "AND (UPPER(m.sens) = 'EPARGNE' OR UPPER(m.typeMouvement) = 'EPARGNE') " +
            "AND DATE(m.dateOperation) BETWEEN :dateDebut AND :dateFin " +
            "GROUP BY m.collecteur.id " +
            "ORDER BY total DESC")
    List<Object[]> getCollecteurRankingInAgence(@Param("agenceId") Long agenceId,
                                                @Param("dateDebut") LocalDate dateDebut,
                                                @Param("dateFin") LocalDate dateFin);

    /**
     * Total épargne par collecteur
     */
    @Query("SELECT SUM(m.montant) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND (UPPER(m.sens) = 'EPARGNE' OR UPPER(m.typeMouvement) = 'EPARGNE') " +
            "AND DATE(m.dateOperation) BETWEEN :dateDebut AND :dateFin")
    Double sumEpargneByCollecteur(@Param("collecteurId") Long collecteurId,
                                  @Param("dateDebut") LocalDate dateDebut,
                                  @Param("dateFin") LocalDate dateFin);

    @Query("SELECT SUM(m.montant) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND (UPPER(m.sens) = 'RETRAIT' OR UPPER(m.typeMouvement) = 'RETRAIT') " +
            "AND DATE(m.dateOperation) BETWEEN :dateDebut AND :dateFin")
    Double sumRetraitsByCollecteur(@Param("collecteurId") Long collecteurId,
                                   @Param("dateDebut") LocalDate dateDebut,
                                   @Param("dateFin") LocalDate dateFin);

    @Query("SELECT COUNT(m) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND DATE(m.dateOperation) BETWEEN :dateDebut AND :dateFin")
    Long countByCollecteurAndPeriod(@Param("collecteurId") Long collecteurId,
                                    @Param("dateDebut") LocalDate dateDebut,
                                    @Param("dateFin") LocalDate dateFin);

    /**
     * Somme retraits par collecteur et période
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.client.collecteur.id = :collecteurId " +
            "AND UPPER(m.sens) = 'RETRAIT' " +
            "AND m.dateOperation BETWEEN :dateDebut AND :dateFin")
    Double sumRetraitByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin
    );

    /**
     * Mouvements d'une agence
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.agence.id = :agenceId " +
            "ORDER BY m.dateOperation DESC")
    Page<Mouvement> findByAgenceId(@Param("agenceId") Long agenceId, Pageable pageable);

    /**
     * Dernière opération d'un client
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findLastOperationByClientId(@Param("clientId") Long clientId, Pageable pageable);

    /**
     * Total retraits par collecteur
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.client.collecteur.id = :collecteurId " +
            "AND UPPER(m.sens) = 'RETRAIT'")
    Double sumRetraitByCollecteurId(@Param("collecteurId") Long collecteurId);

    /**
     * Solde d'un client (épargne - retrait)
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN UPPER(m.sens) = 'EPARGNE' THEN m.montant " +
            "WHEN UPPER(m.sens) = 'RETRAIT' THEN -m.montant ELSE 0 END), 0) " +
            "FROM Mouvement m WHERE m.client.id = :clientId")
    Double calculateSoldeClient(@Param("clientId") Long clientId);

    /**
     * Somme par collecteur et sens
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.client.collecteur.id = :collecteurId " +
            "AND UPPER(m.sens) = UPPER(:sens)")
    Double sumByCollecteurIdAndSens(@Param("collecteurId") Long collecteurId, @Param("sens") String sens);

    /**
     * Statistiques par jour pour un collecteur
     */
    @Query("SELECT DATE(m.dateOperation) as date, " +
            "SUM(CASE WHEN UPPER(m.sens) = 'EPARGNE' THEN m.montant ELSE 0 END) as epargne, " +
            "SUM(CASE WHEN UPPER(m.sens) = 'RETRAIT' THEN m.montant ELSE 0 END) as retrait, " +
            "COUNT(m) as operations " +
            "FROM Mouvement m " +
            "WHERE m.client.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :dateDebut AND :dateFin " +
            "GROUP BY DATE(m.dateOperation) " +
            "ORDER BY DATE(m.dateOperation)")
    List<Object[]> getStatistiquesByJour(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin
    );

    /**
     * Nombre d'opérations par agence
     */
    @Query("SELECT COUNT(m) FROM Mouvement m " +
            "WHERE m.client.agence.id = :agenceId")
    Long countByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Clients avec activité récente
     */
    @Query("SELECT DISTINCT m.client FROM Mouvement m " +
            "WHERE m.client.collecteur.id = :collecteurId " +
            "AND m.dateOperation >= :dateLimit " +
            "ORDER BY m.client.nom, m.client.prenom")
    List<Object> findClientsWithRecentActivity(
            @Param("collecteurId") Long collecteurId,
            @Param("dateLimit") LocalDateTime dateLimit
    );
    // =====================================
    // STATISTIQUES PAR COLLECTEUR ET PÉRIODE
    // =====================================

    /**
     * Statistiques complètes par collecteur et période
     */
    @Query("SELECT " +
            "COUNT(m) as totalTransactions, " +
            "COALESCE(SUM(CASE WHEN m.sens = 'epargne' THEN m.montant ELSE 0 END), 0) as totalEpargne, " +
            "COALESCE(SUM(CASE WHEN m.sens = 'retrait' THEN m.montant ELSE 0 END), 0) as totalRetraits, " +
            "COUNT(CASE WHEN m.sens = 'epargne' THEN 1 END) as nombreEpargnes, " +
            "COUNT(CASE WHEN m.sens = 'retrait' THEN 1 END) as nombreRetraits " +
            "FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startDateTime AND :endDateTime")
    Object[] getStatsByCollecteurAndPeriod(@Param("collecteurId") Long collecteurId,
                                           @Param("startDateTime") LocalDateTime startDateTime,
                                           @Param("endDateTime") LocalDateTime endDateTime);

    // =====================================
    // MÉTHODES PAR CLIENT ET DATES
    // =====================================

    /**
     * Mouvements par client et période
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findByClientIdAndDateRange(@Param("clientId") Long clientId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Mouvements par client et période (pour solde)
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND m.dateOperation BETWEEN :startDateTime AND :endDateTime " +
            "ORDER BY m.dateOperation ASC")
    List<Mouvement> findByClientIdAndPeriod(@Param("clientId") Long clientId,
                                            @Param("startDateTime") LocalDateTime startDateTime,
                                            @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Mouvements par multiples clients
     */
    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.client.id IN :clientIds " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "ORDER BY m.client.id, m.dateOperation")
    List<Mouvement> findByClientIdsAndDateRange(@Param("clientIds") List<Long> clientIds,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // =====================================
    // MÉTHODES DE RECHERCHE ET FILTRAGE
    // =====================================

    /**
     * Recherche par libellé
     */
    List<Mouvement> findByLibelleContaining(String keyword);

    /**
     * Mouvements par journal et sens
     */
    @Query("SELECT m FROM Mouvement m WHERE m.journal = :journal AND m.sens = :sens")
    List<Mouvement> findByJournalAndSens(@Param("journal") Journal journal, @Param("sens") String sens);

    /**
     * Mouvements par sens (tous)
     */
    List<Mouvement> findBySensOrderByDateOperationDesc(String sens);

    // =====================================
    // MÉTHODES POUR COMMISSIONS ET BUSINESS
    // =====================================

    /**
     * Calcul commissions en attente
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "JOIN CompteCollecteur cc ON m.compteSource.id = cc.id " +
            "WHERE m.compteSource.id = :compteId " +
            "AND cc.collecteur.id = :collecteurId " +
            "AND m.libelle LIKE '%commission%' " +
            "AND m.dateOperation > :dateLimit")
    double calculatePendingCommissions(@Param("compteId") Long compteId,
                                       @Param("collecteurId") Long collecteurId,
                                       @Param("dateLimit") LocalDateTime dateLimit);

    /**
     * Somme épargne par client et période (CORRIGÉ - était 'CREDIT' au lieu de 'epargne')
     * ⚠️ CRITIQUE: Cette requête était défectueuse et cherchait 'CREDIT' qui n'existe pas
     * ✅ CORRIGÉE: Maintenant cherche 'epargne' qui correspond aux données réelles
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "AND LOWER(m.sens) = 'epargne'")
    double sumAmountByClientAndPeriod(@Param("clientId") Long clientId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * 🔥 OPTIMISATION N+1: Récupération groupée des montants d'épargne
     */
    @Query("SELECT m.client.id, COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.client.id IN :clientIds " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "AND LOWER(m.sens) = 'epargne' " +
            "GROUP BY m.client.id")
    List<Object[]> sumAmountByClientsAndPeriod(@Param("clientIds") List<Long> clientIds,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // =====================================
    // MÉTHODES POUR TRANSFERTS
    // =====================================

    /**
     * Mouvements par transfert
     */
    @Query("SELECT m FROM Mouvement m WHERE m.transfert.id = :transferId")
    List<Mouvement> findByTransfertId(@Param("transferId") Long transferId);

    // =====================================
    // MÉTHODE AVEC TOUTES LES RELATIONS
    // =====================================

    /**
     * Mouvement par ID avec toutes les relations
     */
    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.client c " +
            "LEFT JOIN FETCH m.collecteur col " +
            "LEFT JOIN FETCH m.compteSource cs " +
            "LEFT JOIN FETCH m.compteDestination cd " +
            "LEFT JOIN FETCH m.journal j " +
            "WHERE m.id = :mouvementId")
    Optional<Mouvement> findByIdWithAllRelations(@Param("mouvementId") Long mouvementId);

    // =====================================
    // MÉTHODES ALTERNATIVES POUR COMPATIBILITÉ
    // =====================================

    /**
     * Comptage par collecteur et date (LocalDate)
     */
    @Query("SELECT COUNT(m) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND DATE(m.dateOperation) = :date")
    Long countByCollecteurAndDate(@Param("collecteurId") Long collecteurId,
                                  @Param("date") LocalDate date);

    /**
     * Somme par collecteur, type et date (LocalDate)
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.sens = :type " +
            "AND DATE(m.dateOperation) = :date")
    Double sumMontantByCollecteurAndTypeAndDate(@Param("collecteurId") Long collecteurId,
                                                @Param("type") String type,
                                                @Param("date") LocalDate date);

    /**
     * Somme par collecteur, type et plage de dates (LocalDate)
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.sens = :type " +
            "AND DATE(m.dateOperation) BETWEEN :startDate AND :endDate")
    Double sumMontantByCollecteurAndTypeAndDateRange(@Param("collecteurId") Long collecteurId,
                                                     @Param("type") String type,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    /**
     * Comptage par collecteur et plage de dates (LocalDate)
     */
    @Query("SELECT COUNT(m) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND DATE(m.dateOperation) BETWEEN :startDate AND :endDate")
    Long countByCollecteurAndDateRange(@Param("collecteurId") Long collecteurId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    // =====================================
    // MÉTHODES UTILITAIRES ET MAINTENANCE
    // =====================================

    /**
     * Volume par jour (pour graphiques)
     */
    @Query("SELECT DATE(m.dateOperation) as jour, COALESCE(SUM(m.montant), 0) as volume " +
            "FROM Mouvement m " +
            "WHERE m.dateOperation BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(m.dateOperation) " +
            "ORDER BY DATE(m.dateOperation)")
    List<Object[]> getVolumeByDay(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Compter les mouvements par période
     */
    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.dateOperation BETWEEN :startDate AND :endDate")
    Long countByPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


    /**
     * Mouvements orphelins (sans journal)
     */
    @Query("SELECT m FROM Mouvement m WHERE m.journal IS NULL")
    List<Mouvement> findOrphanMovements();

    /**
     * Mouvements avec problèmes de comptes
     */
    @Query("SELECT m FROM Mouvement m WHERE m.compteSource IS NULL AND m.compteDestination IS NULL")
    List<Mouvement> findMovementsWithoutAccounts();

    /**
     * Derniers mouvements (pour monitoring)
     */
    @Query("SELECT m FROM Mouvement m ORDER BY m.dateOperation DESC")
    List<Mouvement> findLatestMovements(Pageable pageable);

    // =====================================
    // MÉTHODES OPTIMISÉES AVEC DateTimeService SUPPORT
    // =====================================

    /**
     * Méthode optimisée pour rechercher par collecteur et plage de dates (LocalDateTime)
     */
    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.client c " +
            "LEFT JOIN FETCH m.collecteur col " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startDateTime AND :endDateTime " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findByCollecteurIdAndDateTimeBetween(@Param("collecteurId") Long collecteurId,
                                                         @Param("startDateTime") LocalDateTime startDateTime,
                                                         @Param("endDateTime") LocalDateTime endDateTime);

    // =====================================
    // 🔥 NOUVELLES MÉTHODES POUR SYSTÈME DOUBLE SOLDE
    // =====================================

    /**
     * Calcule le total des montants par client et sens (EPARGNE ou RETRAIT)
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m WHERE m.client.id = :clientId AND UPPER(m.sens) = UPPER(:sens)")
    Double sumMontantByClientIdAndSens(@Param("clientId") Long clientId, @Param("sens") String sens);

    /**
     * 🔥 REQUÊTE OPTIMISÉE : Calcule tous les totaux client en une seule requête
     */
    @Query("SELECT " +
           "COALESCE(SUM(CASE WHEN UPPER(m.sens) = 'EPARGNE' THEN m.montant ELSE 0 END), 0) as totalEpargne, " +
           "COALESCE(SUM(CASE WHEN UPPER(m.sens) = 'RETRAIT' THEN m.montant ELSE 0 END), 0) as totalRetraits " +
           "FROM Mouvement m WHERE m.client.id = :clientId")
    Object[] calculateTotalsForClient(@Param("clientId") Long clientId);

    /**
     * Compte le nombre total de transactions d'un client
     */
    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.client.id = :clientId")
    Long countTransactionsByClientId(@Param("clientId") Long clientId);

    /**
     * Récupère la date de la dernière transaction d'un client
     */
    @Query("SELECT MAX(m.dateOperation) FROM Mouvement m WHERE m.client.id = :clientId")
    LocalDateTime getLastTransactionDate(@Param("clientId") Long clientId);

    /**
     * Version paginée pour rechercher par collecteur et plage de dates
     */
    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.client c " +
            "LEFT JOIN FETCH m.collecteur col " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startDateTime AND :endDateTime " +
            "ORDER BY m.dateOperation DESC")
    Page<Mouvement> findByCollecteurIdAndDateTimeBetween(@Param("collecteurId") Long collecteurId,
                                                         @Param("startDateTime") LocalDateTime startDateTime,
                                                         @Param("endDateTime") LocalDateTime endDateTime,
                                                         Pageable pageable);

    /**
     * Somme par sens (EPARGNE/RETRAIT) - MÉTHODE MANQUANTE CRITIQUE
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m WHERE UPPER(m.sens) = UPPER(:sens)")
    Double sumBySens(@Param("sens") String sens);

    /**
     * Somme par agence et sens - MÉTHODE MANQUANTE CRITIQUE
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.client.agence.id = :agenceId " +
            "AND UPPER(m.sens) = UPPER(:sens)")
    Double sumByAgenceIdAndSens(@Param("agenceId") Long agenceId, @Param("sens") String sens);

    /**
     * Somme des mouvements par collecteur, sens et période
     * Utilisé pour calculer le solde journalier du collecteur
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND UPPER(m.sens) = UPPER(:sens) " +
            "AND m.dateOperation BETWEEN :startDateTime AND :endDateTime")
    Double sumByCollecteurAndSensAndPeriod(@Param("collecteurId") Long collecteurId,
                                           @Param("sens") String sens,
                                           @Param("startDateTime") LocalDateTime startDateTime,
                                           @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Compte les mouvements par collecteur, sens et date
     * Utilisé pour vérifier si le collecteur a fait au moins une épargne dans la journée
     */
    @Query("SELECT COUNT(m) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND UPPER(m.sens) = UPPER(:sens) " +
            "AND DATE(m.dateOperation) = :date")
    Long countByCollecteurAndSensAndDate(@Param("collecteurId") Long collecteurId,
                                         @Param("sens") String sens,
                                         @Param("date") LocalDate date);

    /**
     * Trouve les clients avec des soldes négatifs
     */
    @Query("SELECT m.client.id, m.client.nom, SUM(CASE WHEN m.sens = 'CREDIT' THEN m.montant ELSE -m.montant END) as solde " +
           "FROM Mouvement m " +
           "GROUP BY m.client.id, m.client.nom " +
           "HAVING SUM(CASE WHEN m.sens = 'CREDIT' THEN m.montant ELSE -m.montant END) < 0")
    List<Object[]> findClientsWithNegativeBalances();

    /**
     * Trouve les transactions importantes depuis une date donnée
     */
    @Query("SELECT m FROM Mouvement m " +
           "WHERE m.dateOperation >= :since " +
           "AND m.montant >= :minAmount " +
           "ORDER BY m.montant DESC")
    List<Mouvement> findLargeTransactionsSince(@Param("since") LocalDateTime since, 
                                              @Param("minAmount") double minAmount);

}