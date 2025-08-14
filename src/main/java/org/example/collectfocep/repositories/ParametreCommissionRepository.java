package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.ParametreCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParametreCommissionRepository extends JpaRepository<ParametreCommission, Long> {

    List<ParametreCommission> findByAgenceId(Long agenceId);

    List<ParametreCommission> findByAgenceIdAndActifTrue(Long agenceId);

    Optional<ParametreCommission> findByAgenceIdAndTypeOperationAndActifTrue(
            Long agenceId, 
            ParametreCommission.TypeOperation typeOperation
    );

    List<ParametreCommission> findByTypeOperationAndActifTrue(ParametreCommission.TypeOperation typeOperation);

    @Query("SELECT p FROM ParametreCommission p WHERE p.agence.id = :agenceId AND p.actif = true ORDER BY p.typeOperation")
    List<ParametreCommission> findActiveParametresByAgence(@Param("agenceId") Long agenceId);

    @Query("SELECT COUNT(p) FROM ParametreCommission p WHERE p.agence.id = :agenceId AND p.actif = true")
    long countActiveParametresByAgence(@Param("agenceId") Long agenceId);

    @Query("SELECT DISTINCT p.typeOperation FROM ParametreCommission p WHERE p.agence.id = :agenceId AND p.actif = true")
    List<ParametreCommission.TypeOperation> findActiveTypeOperationsByAgence(@Param("agenceId") Long agenceId);

    boolean existsByAgenceIdAndTypeOperationAndActifTrue(Long agenceId, ParametreCommission.TypeOperation typeOperation);

    void deleteByAgenceIdAndTypeOperation(Long agenceId, ParametreCommission.TypeOperation typeOperation);
}