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
    @Query("SELECT m FROM Mouvement m WHERE m.client.id = :clientId ORDER BY m.dateOperation DESC")
    List<Mouvement> findByClientId(@Param("clientId") Long clientId);

    /**
     * Mouvements par client avec toutes les relations
     */
    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.client c " +
            "LEFT JOIN FETCH m.collecteur col " +
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

    @Query("SELECT m FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "ORDER BY m.dateOperation DESC")
    Page<Mouvement> findByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

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
     * Somme des épargnes par collecteur et période
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.sens = 'epargne' " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    Double sumEpargneByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Somme des retraits par collecteur et période
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND m.sens = 'retrait' " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    Double sumRetraitByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

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
     * Somme par client et période
     */
    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.client.id = :clientId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "AND m.sens = 'CREDIT'")
    double sumAmountByClientAndPeriod(@Param("clientId") Long clientId,
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
            "ORDER BY jour")
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
            "WHERE m.client.agence.id = :agenceId AND UPPER(m.sens) = UPPER(:sens)")
    Double sumByAgenceIdAndSens(@Param("agenceId") Long agenceId, @Param("sens") String sens);
}