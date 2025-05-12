package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Commission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {

    @Query("SELECT c FROM Commission c WHERE c.collecteur.agence.id = :agenceId")
    List<Commission> findByAgenceId(@Param("agenceId") Long agenceId);

    @Query("SELECT c FROM Commission c WHERE c.collecteur.id = :collecteurId")
    List<Commission> findByCollecteurId(@Param("collecteurId") Long collecteurId);

    @Query("SELECT c FROM Commission c WHERE c.client.id = :clientId")
    List<Commission> findByClientId(@Param("clientId") Long clientId);
}