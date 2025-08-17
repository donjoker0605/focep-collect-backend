package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.HistoriqueCalculCommission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 🔥 REPOSITORY: Historique des calculs de commission
 * 
 * Fonctionnalités:
 * - Vérification des doublons de calcul
 * - Récupération de l'historique par collecteur
 * - Recherche des calculs non rémunérés
 */
@Repository
public interface HistoriqueCalculCommissionRepository extends JpaRepository<HistoriqueCalculCommission, Long> {

    // =====================================
    // PRÉVENTION DES DOUBLONS
    // =====================================

    /**
     * Vérifie si un calcul existe déjà pour cette période
     * CRITIQUE: Empêche les calculs en double
     */
    @Query("""
        SELECT COUNT(h) > 0 FROM HistoriqueCalculCommission h 
        WHERE h.collecteur.id = :collecteurId 
        AND h.dateDebut = :dateDebut 
        AND h.dateFin = :dateFin
        AND h.statut != 'ANNULE'
        """)
    boolean existsCalculForPeriod(@Param("collecteurId") Long collecteurId,
                                  @Param("dateDebut") LocalDate dateDebut,
                                  @Param("dateFin") LocalDate dateFin);

    /**
     * Récupère un calcul existant pour une période donnée
     */
    @Query("""
        SELECT h FROM HistoriqueCalculCommission h 
        WHERE h.collecteur.id = :collecteurId 
        AND h.dateDebut = :dateDebut 
        AND h.dateFin = :dateFin
        AND h.statut != 'ANNULE'
        """)
    Optional<HistoriqueCalculCommission> findByCollecteurAndPeriod(@Param("collecteurId") Long collecteurId,
                                                                   @Param("dateDebut") LocalDate dateDebut,
                                                                   @Param("dateFin") LocalDate dateFin);

    // =====================================
    // HISTORIQUE PAR COLLECTEUR
    // =====================================

    /**
     * Récupère tous les calculs d'un collecteur (triés par date décroissante)
     */
    @Query("""
        SELECT h FROM HistoriqueCalculCommission h 
        WHERE h.collecteur.id = :collecteurId 
        ORDER BY h.dateCalcul DESC
        """)
    List<HistoriqueCalculCommission> findByCollecteurIdOrderByDateCalculDesc(@Param("collecteurId") Long collecteurId);

    /**
     * Récupère l'historique paginé d'un collecteur
     */
    @Query("""
        SELECT h FROM HistoriqueCalculCommission h 
        WHERE h.collecteur.id = :collecteurId 
        ORDER BY h.dateCalcul DESC
        """)
    Page<HistoriqueCalculCommission> findByCollecteurId(@Param("collecteurId") Long collecteurId, Pageable pageable);

    // =====================================
    // CALCULS NON RÉMUNÉRÉS
    // =====================================

    /**
     * Récupère tous les calculs non rémunérés d'un collecteur
     * IMPORTANT: Utilisé pour le processus de rémunération
     */
    @Query("""
        SELECT h FROM HistoriqueCalculCommission h 
        WHERE h.collecteur.id = :collecteurId 
        AND h.remunere = false 
        AND h.statut = 'CALCULE'
        ORDER BY h.dateCalcul ASC
        """)
    List<HistoriqueCalculCommission> findNonRemuneresForCollecteur(@Param("collecteurId") Long collecteurId);

    /**
     * Compte les calculs non rémunérés d'un collecteur
     */
    @Query("""
        SELECT COUNT(h) FROM HistoriqueCalculCommission h 
        WHERE h.collecteur.id = :collecteurId 
        AND h.remunere = false 
        AND h.statut = 'CALCULE'
        """)
    long countNonRemuneresForCollecteur(@Param("collecteurId") Long collecteurId);

    // =====================================
    // STATISTIQUES ET RAPPORTS
    // =====================================

    /**
     * Récupère les calculs par agence
     */
    @Query("""
        SELECT h FROM HistoriqueCalculCommission h 
        WHERE h.agenceId = :agenceId 
        AND h.dateCalcul BETWEEN :dateDebut AND :dateFin
        ORDER BY h.dateCalcul DESC
        """)
    List<HistoriqueCalculCommission> findByAgenceAndPeriod(@Param("agenceId") Long agenceId,
                                                           @Param("dateDebut") java.time.LocalDateTime dateDebut,
                                                           @Param("dateFin") java.time.LocalDateTime dateFin);

    /**
     * Somme des commissions par agence sur une période
     */
    @Query("""
        SELECT COALESCE(SUM(h.montantCommissionTotal), 0) FROM HistoriqueCalculCommission h 
        WHERE h.agenceId = :agenceId 
        AND h.dateCalcul BETWEEN :dateDebut AND :dateFin
        AND h.statut = 'CALCULE'
        """)
    java.math.BigDecimal sumCommissionsByAgenceAndPeriod(@Param("agenceId") Long agenceId,
                                                         @Param("dateDebut") java.time.LocalDateTime dateDebut,
                                                         @Param("dateFin") java.time.LocalDateTime dateFin);

    /**
     * Récupère les derniers calculs (dashboard)
     */
    @Query("""
        SELECT h FROM HistoriqueCalculCommission h 
        ORDER BY h.dateCalcul DESC
        """)
    Page<HistoriqueCalculCommission> findLatestCalculs(Pageable pageable);

    // =====================================
    // RÉMUNÉRATION
    // =====================================

    /**
     * Marque plusieurs calculs comme rémunérés
     */
    @Query("""
        UPDATE HistoriqueCalculCommission h 
        SET h.remunere = true, h.remunerationId = :remunerationId, h.dateRemuneration = CURRENT_TIMESTAMP 
        WHERE h.id IN :calculIds
        """)
    void markAsRemunered(@Param("calculIds") List<Long> calculIds, @Param("remunerationId") Long remunerationId);

    /**
     * Récupère les calculs associés à une rémunération
     */
    @Query("""
        SELECT h FROM HistoriqueCalculCommission h 
        WHERE h.remunerationId = :remunerationId
        """)
    List<HistoriqueCalculCommission> findByRemunerationId(@Param("remunerationId") Long remunerationId);
}