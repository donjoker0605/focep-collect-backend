package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Commission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {

    // ✅ MÉTHODES EXISTANTES
    @Query("SELECT c FROM Commission c WHERE c.collecteur.agence.id = :agenceId")
    List<Commission> findByAgenceId(@Param("agenceId") Long agenceId);

    @Query("SELECT c FROM Commission c WHERE c.collecteur.id = :collecteurId")
    List<Commission> findByCollecteurId(@Param("collecteurId") Long collecteurId);

    @Query("SELECT c FROM Commission c WHERE c.client.id = :clientId")
    List<Commission> findByClientId(@Param("clientId") Long clientId);

    // ✅ MÉTHODES POUR LE DASHBOARD ADMIN - CORRIGÉES

    /**
     * Compte toutes les commissions
     */
    @Query("SELECT COUNT(c) FROM Commission c")
    Long countAllCommissions();

    /**
     * Compte les commissions par agence
     */
    @Query("SELECT COUNT(c) FROM Commission c WHERE c.collecteur.agence.id = :agenceId")
    Long countCommissionsByAgence(@Param("agenceId") Long agenceId);

    /**
     * ✅ COMMISSIONS EN ATTENTE - CORRIGÉ SANS STATUT
     * Utilise le fait qu'une commission sans rapport est considérée comme "en attente"
     */
    @Query("SELECT COUNT(c) FROM Commission c WHERE c.rapport IS NULL")
    Long countPendingCommissions();

    /**
     * ✅ COMMISSIONS EN ATTENTE PAR AGENCE - CORRIGÉ SANS STATUT
     */
    @Query("SELECT COUNT(c) FROM Commission c WHERE c.collecteur.agence.id = :agenceId AND c.rapport IS NULL")
    Long countPendingCommissionsByAgence(@Param("agenceId") Long agenceId);

    /**
     * Total des commissions calculées
     */
    @Query("SELECT COALESCE(SUM(c.montant), 0) FROM Commission c")
    Double sumAllCommissions();

    /**
     * Total des commissions par agence
     */
    @Query("SELECT COALESCE(SUM(c.montant), 0) FROM Commission c WHERE c.collecteur.agence.id = :agenceId")
    Double sumCommissionsByAgence(@Param("agenceId") Long agenceId);

    /**
     * Total de la TVA
     */
    @Query("SELECT COALESCE(SUM(c.tva), 0) FROM Commission c")
    Double sumAllTVA();

    /**
     * Total de la TVA par agence
     */
    @Query("SELECT COALESCE(SUM(c.tva), 0) FROM Commission c WHERE c.collecteur.agence.id = :agenceId")
    Double sumTVAByAgence(@Param("agenceId") Long agenceId);

    /**
     * Commissions par période
     */
    @Query("SELECT c FROM Commission c WHERE c.dateCalcul BETWEEN :startDate AND :endDate ORDER BY c.dateCalcul DESC")
    List<Commission> findByPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Commissions par période avec pagination
     */
    @Query("SELECT c FROM Commission c WHERE c.dateCalcul BETWEEN :startDate AND :endDate ORDER BY c.dateCalcul DESC")
    Page<Commission> findByPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * Commissions par collecteur et période
     */
    @Query("SELECT c FROM Commission c WHERE c.collecteur.id = :collecteurId AND c.dateCalcul BETWEEN :startDate AND :endDate ORDER BY c.dateCalcul DESC")
    List<Commission> findByCollecteurAndPeriod(
            @Param("collecteurId") Long collecteurId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Commissions par agence et période
     */
    @Query("SELECT c FROM Commission c WHERE c.collecteur.agence.id = :agenceId AND c.dateCalcul BETWEEN :startDate AND :endDate ORDER BY c.dateCalcul DESC")
    List<Commission> findByAgenceAndPeriod(
            @Param("agenceId") Long agenceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Top collecteurs par commissions
     */
    @Query("SELECT c.collecteur.id, COALESCE(SUM(c.montant), 0) as totalCommissions " +
            "FROM Commission c " +
            "GROUP BY c.collecteur.id " +
            "ORDER BY totalCommissions DESC")
    List<Object[]> findTopCollecteursByCommissions(Pageable pageable);

    /**
     * Statistiques par collecteur
     */
    @Query("SELECT " +
            "COUNT(c) as nombreCommissions, " +
            "COALESCE(SUM(c.montant), 0) as totalMontant, " +
            "COALESCE(SUM(c.tva), 0) as totalTVA, " +
            "COALESCE(AVG(c.montant), 0) as moyenneMontant " +
            "FROM Commission c " +
            "WHERE c.collecteur.id = :collecteurId")
    Object[] getStatsByCollecteur(@Param("collecteurId") Long collecteurId);

    /**
     * Statistiques par agence
     */
    @Query("SELECT " +
            "COUNT(c) as nombreCommissions, " +
            "COALESCE(SUM(c.montant), 0) as totalMontant, " +
            "COALESCE(SUM(c.tva), 0) as totalTVA, " +
            "COUNT(DISTINCT c.collecteur.id) as nombreCollecteurs " +
            "FROM Commission c " +
            "WHERE c.collecteur.agence.id = :agenceId")
    Object[] getStatsByAgence(@Param("agenceId") Long agenceId);

    /**
     * Dernières commissions calculées
     */
    @Query("SELECT c FROM Commission c ORDER BY c.dateCalcul DESC")
    List<Commission> findRecentCommissions(Pageable pageable);

    /**
     * Commissions par type
     */
    @Query("SELECT c FROM Commission c WHERE c.type = :type")
    List<Commission> findByType(@Param("type") String type);

    /**
     * ✅ ALIAS POUR COMPATIBILITÉ
     */
    @Query("SELECT COUNT(c) FROM Commission c WHERE c.collecteur.agence.id = :agenceId")
    Long countByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Volume de commissions par mois
     */
    @Query(value = "SELECT YEAR(date_calcul) as annee, " +
            "MONTH(date_calcul) as mois, " +
            "SUM(montant) as volume " +
            "FROM commission " +
            "WHERE date_calcul BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(date_calcul), MONTH(date_calcul) " +
            "ORDER BY annee, mois",
            nativeQuery = true)
    List<Object[]> getVolumeByMonth(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Commissions avec détails complets (avec joins)
     */
    @Query("SELECT c FROM Commission c " +
            "LEFT JOIN FETCH c.collecteur col " +
            "LEFT JOIN FETCH c.client cl " +
            "LEFT JOIN FETCH c.commissionParameter cp " +
            "WHERE c.id = :id")
    Commission findByIdWithDetails(@Param("id") Long id);

    /**
     * Vérifier l'existence de commissions pour un paramètre
     */
    @Query("SELECT COUNT(c) FROM Commission c WHERE c.commissionParameter.id = :parameterId")
    Long countByCommissionParameter(@Param("parameterId") Long parameterId);

    /**
     * Commissions sans paramètre associé (pour maintenance)
     */
    @Query("SELECT c FROM Commission c WHERE c.commissionParameter IS NULL")
    List<Commission> findOrphanCommissions();

    /**
     * Supprimer les commissions d'un collecteur
     */
    @Query("DELETE FROM Commission c WHERE c.collecteur.id = :collecteurId")
    void deleteByCollecteurId(@Param("collecteurId") Long collecteurId);

    /**
     * Commissions par client avec pagination
     */
    @Query("SELECT c FROM Commission c WHERE c.client.id = :clientId ORDER BY c.dateCalcul DESC")
    Page<Commission> findByClientId(@Param("clientId") Long clientId, Pageable pageable);

    /**
     * ✅ ALTERNATIVE: Commissions récentes sans rapport (plus précis)
     */
    @Query("SELECT COUNT(c) FROM Commission c WHERE c.rapport IS NULL AND c.dateCalcul >= :cutoffDate")
    Long countPendingCommissionsAfterDate(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Méthodes pour le collecteur spécifique
     */
    @Query("SELECT COUNT(c) FROM Commission c WHERE c.collecteur.id = :collecteurId AND c.rapport IS NULL")
    Long countPendingCommissionsByCollecteur(@Param("collecteurId") Long collecteurId);

    @Query("SELECT COALESCE(SUM(c.montant), 0) FROM Commission c WHERE c.collecteur.id = :collecteurId")
    Double sumCommissionsByCollecteur(@Param("collecteurId") Long collecteurId);
}