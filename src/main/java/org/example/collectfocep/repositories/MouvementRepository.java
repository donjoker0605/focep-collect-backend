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

@Repository
public interface MouvementRepository extends JpaRepository<Mouvement, Long> {

    // =====================================
    // REQUÊTES BASIQUES PAR JOURNAL
    // =====================================

    List<Mouvement> findByJournal(Journal journal);
    Page<Mouvement> findByJournal(Journal journal, Pageable pageable);

    @Query("SELECT m FROM Mouvement m WHERE m.journal.id = :journalId")
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

    @Query("SELECT m FROM Mouvement m WHERE m.collecteur.id = :collecteurId")
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

    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId AND m.typeMouvement = :type")
    Double sumMontantByCollecteurAndType(
            @Param("collecteurId") Long collecteurId,
            @Param("type") String type
    );

    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId AND m.sens = :sens")
    Double sumMontantByCollecteurAndSens(
            @Param("collecteurId") Long collecteurId,
            @Param("sens") String sens
    );

    // =====================================
    // REQUÊTES SPÉCIALISÉES PAR TYPE DE MOUVEMENT
    // =====================================

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND (m.typeMouvement = 'EPARGNE' OR m.sens = 'epargne') " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    Double sumEpargneByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND (m.typeMouvement = 'RETRAIT' OR m.sens = 'retrait') " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    Double sumRetraitByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // =====================================
    // REQUÊTES PAR DATE (LocalDate)
    // =====================================

    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.collecteur.id = :collecteurId " +
            "AND DATE(m.dateMouvement) = :date")
    Long countByCollecteurAndDate(
            @Param("collecteurId") Long collecteurId,
            @Param("date") LocalDate date
    );

    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId AND m.typeMouvement = :type " +
            "AND DATE(m.dateMouvement) = :date")
    Double sumMontantByCollecteurAndTypeAndDate(
            @Param("collecteurId") Long collecteurId,
            @Param("type") String type,
            @Param("date") LocalDate date
    );

    @Query("SELECT COALESCE(SUM(m.montant), 0.0) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId AND m.typeMouvement = :type " +
            "AND DATE(m.dateMouvement) BETWEEN :dateDebut AND :dateFin")
    Double sumMontantByCollecteurAndTypeAndDateRange(
            @Param("collecteurId") Long collecteurId,
            @Param("type") String type,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.collecteur.id = :collecteurId " +
            "AND DATE(m.dateMouvement) BETWEEN :dateDebut AND :dateFin")
    Long countByCollecteurAndDateRange(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    // =====================================
    // REQUÊTES PAR CLIENT
    // =====================================

    @Query("SELECT m FROM Mouvement m WHERE m.client.id = :clientId")
    List<Mouvement> findByClientId(@Param("clientId") Long clientId);

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

    @Query("SELECT m FROM Mouvement m WHERE m.dateOperation BETWEEN :debut AND :fin")
    List<Mouvement> findByDateOperationBetween(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );

    @Query("SELECT m FROM Mouvement m WHERE m.dateOperation BETWEEN :debut AND :fin")
    Page<Mouvement> findByDateOperationBetween(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin,
            Pageable pageable
    );

    // =====================================
    // REQUÊTES POUR COMMISSIONS ET BUSINESS - CORRECTION AJOUTÉE
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

    @Query("SELECT m FROM Mouvement m " +
            "LEFT JOIN FETCH m.compteSource " +
            "LEFT JOIN FETCH m.compteDestination " +
            "LEFT JOIN FETCH m.journal " +
            "WHERE m.journal.id = :journalId")
    List<Mouvement> findByJournalIdWithAccounts(@Param("journalId") Long journalId);

    @Query("SELECT new org.example.collectfocep.dto.MouvementProjection(" +
            "m.id, m.montant, m.libelle, m.sens, m.dateOperation, " +
            "cs.numeroCompte, cd.numeroCompte) " +
            "FROM Mouvement m " +
            "LEFT JOIN m.compteSource cs " +
            "LEFT JOIN m.compteDestination cd " +
            "WHERE m.journal.id = :journalId")
    List<MouvementProjection> findMouvementProjectionsByJournalId(@Param("journalId") Long journalId);

    // =====================================
    // REQUÊTES DE RECHERCHE ET FILTRAGE
    // =====================================

    List<Mouvement> findByLibelleContaining(String keyword);

    @Query("SELECT m FROM Mouvement m WHERE m.journal = :journal AND m.sens = :sens")
    List<Mouvement> findByJournalAndSens(@Param("journal") Journal journal, @Param("sens") String sens);

    @Query("SELECT m FROM Mouvement m WHERE m.collecteur.id = :collecteurId " +
            "ORDER BY m.dateOperation DESC")
    List<Mouvement> findRecentByCollecteur(
            @Param("collecteurId") Long collecteurId,
            Pageable pageable
    );

    // =====================================
// REQUÊTES PAR COLLECTEUR ET DATE - VERSION CORRIGÉE
// =====================================

    @Query("SELECT m FROM Mouvement m WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    List<Mouvement> findByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT m FROM Mouvement m WHERE m.collecteur.id = :collecteurId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    Page<Mouvement> findByCollecteurIdAndDateOperationBetween(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}