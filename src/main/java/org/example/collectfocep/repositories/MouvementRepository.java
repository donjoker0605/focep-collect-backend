package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Compte;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementRepository extends JpaRepository<Mouvement, Long> {

    List<Mouvement> findByJournal(Journal journal);

    Page<Mouvement> findByJournal(Journal journal, Pageable pageable);

    @Query("SELECT m FROM Mouvement m WHERE m.compteSource = :compte OR m.compteDestination = :compte")
    List<Mouvement> findByCompte(@Param("compte") Compte compte);

    @Query("SELECT m FROM Mouvement m WHERE m.compteSource = :compte OR m.compteDestination = :compte")
    Page<Mouvement> findByCompte(@Param("compte") Compte compte, Pageable pageable);

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

    @Query("SELECT SUM(m.montant) FROM Mouvement m WHERE m.compteSource = :compte AND m.sens = 'DEBIT'")
    Double sumDebits(@Param("compte") Compte compte);

    @Query("SELECT SUM(m.montant) FROM Mouvement m WHERE m.compteDestination = :compte AND m.sens = 'CREDIT'")
    Double sumCredits(@Param("compte") Compte compte);

    @Query("SELECT COUNT(m) FROM Mouvement m WHERE m.journal = :journal")
    long countByJournal(@Param("journal") Journal journal);

    @Query("SELECT m FROM Mouvement m WHERE m.journal = :journal AND m.sens = :sens")
    List<Mouvement> findByJournalAndSens(@Param("journal") Journal journal, @Param("sens") String sens);

    @Query("SELECT m FROM Mouvement m JOIN m.compteDestination cd JOIN cd.client c " +
            "WHERE c.id IN :clientIds AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "ORDER BY c.id, m.dateOperation")
    List<Mouvement> findByClientIdsAndDateRange(
            @Param("clientIds") List<Long> clientIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "JOIN CompteCollecteur cc ON m.compteSource.id = cc.id " +
            "WHERE m.compteSource.id = :compteId " +
            "AND cc.collecteur.id = :collecteurId " +
            "AND m.libelle LIKE '%commission%' " +
            "AND m.dateOperation > :dateLimit")
    double calculatePendingCommissions(
            @Param("compteId") Long compteId,
            @Param("collecteurId") Long collecteurId,
            @Param("dateLimit") LocalDateTime dateLimit);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "JOIN CompteClient cc ON cc.id = m.compteDestination.id " +
            "WHERE cc.client.id = :clientId " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate " +
            "AND m.sens = 'CREDIT'")
    double sumAmountByClientAndPeriod(
            @Param("clientId") Long clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT m FROM Mouvement m WHERE m.journal.id = :journalId")
    List<Mouvement> findByJournalId(@Param("journalId") Long journalId);

    void deleteByJournal(Journal journal);

    // Méthode manquante pour trouver les mouvements liés à un transfert
    @Query("SELECT m FROM Mouvement m WHERE m.transfert.id = :transferId")
    List<Mouvement> findByTransfertId(@Param("transferId") Long transferId);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.compteSource.id IN " +
            "(SELECT cc.id FROM CompteClient cc WHERE cc.client.collecteur.id = :collecteurId) " +
            "AND m.compteDestination.typeCompte = 'ATTENTE' " +
            "AND m.libelle LIKE '%Commission%' " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    double sumCommissionsByCollecteurAndPeriod(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mouvement m " +
            "WHERE m.compteSource.id IN " +
            "(SELECT cc.id FROM CompteClient cc WHERE cc.client.collecteur.id = :collecteurId) " +
            "AND m.compteDestination.typeCompte = 'TAXE' " +
            "AND m.libelle LIKE '%TVA%' " +
            "AND m.dateOperation BETWEEN :startDate AND :endDate")
    double sumTVAByCollecteurAndPeriod(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    List<Mouvement> findByLibelleContaining(String keyword);
}