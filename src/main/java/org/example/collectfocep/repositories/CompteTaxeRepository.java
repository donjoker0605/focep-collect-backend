package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.CompteTaxe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteTaxeRepository extends JpaRepository<CompteTaxe, Long> {

    /**
     * Trouve le compte taxe par agence ID
     */
    @Query("SELECT ct FROM CompteTaxe ct WHERE ct.agence.id = :agenceId")
    CompteTaxe findByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve tous les comptes taxe d'une agence
     */
    @Query("SELECT ct FROM CompteTaxe ct WHERE ct.agence.id = :agenceId")
    List<CompteTaxe> findAllByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Vérifie si un compte taxe existe pour une agence
     */
    @Query("SELECT CASE WHEN COUNT(ct) > 0 THEN true ELSE false END FROM CompteTaxe ct WHERE ct.agence.id = :agenceId")
    boolean existsByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve le compte taxe avec les détails de l'agence
     */
    @Query("SELECT ct FROM CompteTaxe ct JOIN FETCH ct.agence WHERE ct.agence.id = :agenceId")
    Optional<CompteTaxe> findByAgenceIdWithAgence(@Param("agenceId") Long agenceId);
}