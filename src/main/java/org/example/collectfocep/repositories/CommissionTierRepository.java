package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.CommissionTier;
import org.example.collectfocep.entities.CommissionParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionTierRepository extends JpaRepository<CommissionTier, Long> {

    /**
     * Rechercher tous les tiers d'un paramètre de commission
     */
    List<CommissionTier> findByCommissionParameterOrderByMontantMinAsc(CommissionParameter commissionParameter);

    /**
     * Rechercher les tiers par ID de paramètre
     */
    @Query("SELECT ct FROM CommissionTier ct WHERE ct.commissionParameter.id = :parameterId ORDER BY ct.montantMin ASC")
    List<CommissionTier> findByCommissionParameterIdOrderByMontantMinAsc(@Param("parameterId") Long parameterId);

    /**
     * Supprimer tous les tiers d'un paramètre
     */
    void deleteByCommissionParameter(CommissionParameter commissionParameter);

    /**
     * Supprimer tous les tiers par ID de paramètre
     */
    @Query("DELETE FROM CommissionTier ct WHERE ct.commissionParameter.id = :parameterId")
    void deleteByCommissionParameterId(@Param("parameterId") Long parameterId);

    /**
     * Compter les tiers d'un paramètre
     */
    long countByCommissionParameter(CommissionParameter commissionParameter);

    /**
     * Vérifier l'existence de chevauchements dans les montants
     */
    @Query("SELECT COUNT(ct) FROM CommissionTier ct WHERE ct.commissionParameter.id = :parameterId " +
            "AND ((ct.montantMin < :montantMax AND ct.montantMax > :montantMin) " +
            "OR (ct.montantMin <= :montantMin AND ct.montantMax >= :montantMax))")
    long countOverlappingTiers(@Param("parameterId") Long parameterId,
                               @Param("montantMin") Double montantMin,
                               @Param("montantMax") Double montantMax);

    /**
     * Rechercher le tier applicable pour un montant donné
     */
    @Query("SELECT ct FROM CommissionTier ct WHERE ct.commissionParameter.id = :parameterId " +
            "AND ct.montantMin <= :montant " +
            "AND (ct.montantMax IS NULL OR ct.montantMax >= :montant) " +
            "ORDER BY ct.montantMin DESC")
    List<CommissionTier> findApplicableTier(@Param("parameterId") Long parameterId, @Param("montant") Double montant);

    /**
     * Obtenir la liste des tiers avec détails du paramètre
     */
    @Query("SELECT ct FROM CommissionTier ct " +
            "LEFT JOIN FETCH ct.commissionParameter cp " +
            "WHERE ct.commissionParameter.id = :parameterId " +
            "ORDER BY ct.montantMin ASC")
    List<CommissionTier> findWithParameterDetails(@Param("parameterId") Long parameterId);
}