package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.ComptePassageTaxe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComptePassageTaxeRepository extends JpaRepository<ComptePassageTaxe, Long> {

    /**
     * Trouve le compte passage taxe par agence ID
     */
    @Query("SELECT cpt FROM ComptePassageTaxe cpt WHERE cpt.agence.id = :agenceId")
    ComptePassageTaxe findByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve tous les comptes passage taxe d'une agence
     */
    @Query("SELECT cpt FROM ComptePassageTaxe cpt WHERE cpt.agence.id = :agenceId")
    List<ComptePassageTaxe> findAllByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Vérifie si un compte passage taxe existe pour une agence
     */
    @Query("SELECT CASE WHEN COUNT(cpt) > 0 THEN true ELSE false END FROM ComptePassageTaxe cpt WHERE cpt.agence.id = :agenceId")
    boolean existsByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve le compte passage taxe avec les détails de l'agence
     */
    @Query("SELECT cpt FROM ComptePassageTaxe cpt JOIN FETCH cpt.agence WHERE cpt.agence.id = :agenceId")
    Optional<ComptePassageTaxe> findByAgenceIdWithAgence(@Param("agenceId") Long agenceId);
}