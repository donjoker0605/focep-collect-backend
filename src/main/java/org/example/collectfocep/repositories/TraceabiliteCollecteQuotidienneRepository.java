package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.TraceabiliteCollecteQuotidienne;
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
public interface TraceabiliteCollecteQuotidienneRepository extends JpaRepository<TraceabiliteCollecteQuotidienne, Long> {

    /**
     * Trouve la trace pour un collecteur et une date donnée
     */
    Optional<TraceabiliteCollecteQuotidienne> findByCollecteurAndDateCollecte(Collecteur collecteur, LocalDate dateCollecte);

    /**
     * Vérifie si une trace existe pour un collecteur et une date
     */
    boolean existsByCollecteurAndDateCollecte(Collecteur collecteur, LocalDate dateCollecte);

    /**
     * Récupère l'historique des collectes d'un collecteur
     */
    @Query("SELECT t FROM TraceabiliteCollecteQuotidienne t WHERE t.collecteur.id = :collecteurId " +
            "ORDER BY t.dateCollecte DESC")
    Page<TraceabiliteCollecteQuotidienne> findByCollecteurIdOrderByDateCollecteDesc(
            @Param("collecteurId") Long collecteurId, Pageable pageable);

    /**
     * Récupère les traces dans une plage de dates
     */
    @Query("SELECT t FROM TraceabiliteCollecteQuotidienne t WHERE t.collecteur.id = :collecteurId " +
            "AND t.dateCollecte BETWEEN :dateDebut AND :dateFin " +
            "ORDER BY t.dateCollecte DESC")
    List<TraceabiliteCollecteQuotidienne> findByCollecteurAndDateRange(
            @Param("collecteurId") Long collecteurId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    /**
     * Statistiques mensuelles pour un collecteur
     */
    @Query("SELECT " +
            "COUNT(t) as nombreJours, " +
            "SUM(t.totalEpargneJour) as totalEpargneMois, " +
            "SUM(t.totalRetraitsJour) as totalRetraitsMois, " +
            "SUM(t.nombreOperationsJour) as totalOperationsMois, " +
            "AVG(t.soldeCompteServiceAvantCloture) as moyenneSoldeQuotidien " +
            "FROM TraceabiliteCollecteQuotidienne t " +
            "WHERE t.collecteur.id = :collecteurId " +
            "AND YEAR(t.dateCollecte) = :annee AND MONTH(t.dateCollecte) = :mois")
    Object[] getStatistiquesMensuelles(@Param("collecteurId") Long collecteurId,
                                       @Param("annee") int annee,
                                       @Param("mois") int mois);

    /**
     * Collecteurs les plus performants sur une période
     */
    @Query("SELECT t.collecteur.id, t.collecteur.nom, t.collecteur.prenom, " +
            "SUM(t.totalEpargneJour) as totalEpargne, " +
            "COUNT(t) as nombreJoursActifs " +
            "FROM TraceabiliteCollecteQuotidienne t " +
            "WHERE t.dateCollecte BETWEEN :dateDebut AND :dateFin " +
            "GROUP BY t.collecteur.id, t.collecteur.nom, t.collecteur.prenom " +
            "ORDER BY SUM(t.totalEpargneJour) DESC")
    List<Object[]> getTopCollecteursByPeriod(@Param("dateDebut") LocalDate dateDebut,
                                             @Param("dateFin") LocalDate dateFin,
                                             Pageable pageable);

    /**
     * Traces incohérentes (pour audit)
     */
    @Query("SELECT t FROM TraceabiliteCollecteQuotidienne t WHERE " +
            "ABS(t.soldeCompteServiceAvantCloture - (t.totalEpargneJour - t.totalRetraitsJour)) > 0.01")
    List<TraceabiliteCollecteQuotidienne> findTracesIncoherentes();

    /**
     * Collectes non clôturées (pour suivi)
     */
    @Query("SELECT t FROM TraceabiliteCollecteQuotidienne t WHERE t.clotureEffectuee = false " +
            "AND t.dateCollecte < :seuilDate " +
            "ORDER BY t.dateCollecte")
    List<TraceabiliteCollecteQuotidienne> findCollectesNonClôturees(@Param("seuilDate") LocalDate seuilDate);

    /**
     * Total collecté par agence sur une période
     */
    @Query("SELECT t.collecteur.agence.id, t.collecteur.agence.nomAgence, " +
            "SUM(t.totalEpargneJour) as totalEpargne, " +
            "SUM(t.totalRetraitsJour) as totalRetraits, " +
            "COUNT(DISTINCT t.collecteur.id) as nombreCollecteurs " +
            "FROM TraceabiliteCollecteQuotidienne t " +
            "WHERE t.dateCollecte BETWEEN :dateDebut AND :dateFin " +
            "GROUP BY t.collecteur.agence.id, t.collecteur.agence.nomAgence " +
            "ORDER BY SUM(t.totalEpargneJour) DESC")
    List<Object[]> getStatistiquesParAgence(@Param("dateDebut") LocalDate dateDebut,
                                            @Param("dateFin") LocalDate dateFin);

    /**
     * Évolution quotidienne d'un collecteur sur une période
     */
    @Query("SELECT t.dateCollecte, t.totalEpargneJour, t.totalRetraitsJour, " +
            "t.nombreOperationsJour, t.soldeCompteServiceAvantCloture " +
            "FROM TraceabiliteCollecteQuotidienne t " +
            "WHERE t.collecteur.id = :collecteurId " +
            "AND t.dateCollecte BETWEEN :dateDebut AND :dateFin " +
            "ORDER BY t.dateCollecte")
    List<Object[]> getEvolutionQuotidienne(@Param("collecteurId") Long collecteurId,
                                           @Param("dateDebut") LocalDate dateDebut,
                                           @Param("dateFin") LocalDate dateFin);

    /**
     * Nettoie les anciennes traces (pour maintenance)
     */
    @Query("DELETE FROM TraceabiliteCollecteQuotidienne t WHERE t.dateCollecte < :seuilDate")
    void deleteOldTraces(@Param("seuilDate") LocalDate seuilDate);

    /**
     * Compte les jours d'activité d'un collecteur
     */
    @Query("SELECT COUNT(t) FROM TraceabiliteCollecteQuotidienne t " +
            "WHERE t.collecteur.id = :collecteurId " +
            "AND t.dateCollecte BETWEEN :dateDebut AND :dateFin " +
            "AND (t.totalEpargneJour > 0 OR t.totalRetraitsJour > 0)")
    Long countJoursActivite(@Param("collecteurId") Long collecteurId,
                            @Param("dateDebut") LocalDate dateDebut,
                            @Param("dateFin") LocalDate dateFin);
}