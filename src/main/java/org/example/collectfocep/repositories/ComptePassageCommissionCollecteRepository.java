package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.ComptePassageCommissionCollecte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComptePassageCommissionCollecteRepository extends JpaRepository<ComptePassageCommissionCollecte, Long> {

    /**
     * Trouve le compte passage commission collecte par agence ID
     */
    @Query("SELECT cpcc FROM ComptePassageCommissionCollecte cpcc WHERE cpcc.agence.id = :agenceId")
    ComptePassageCommissionCollecte findByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve tous les comptes passage commission collecte d'une agence
     */
    @Query("SELECT cpcc FROM ComptePassageCommissionCollecte cpcc WHERE cpcc.agence.id = :agenceId")
    List<ComptePassageCommissionCollecte> findAllByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Vérifie si un compte passage commission collecte existe pour une agence
     */
    @Query("SELECT CASE WHEN COUNT(cpcc) > 0 THEN true ELSE false END FROM ComptePassageCommissionCollecte cpcc WHERE cpcc.agence.id = :agenceId")
    boolean existsByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve le compte passage commission collecte avec les détails de l'agence
     */
    @Query("SELECT cpcc FROM ComptePassageCommissionCollecte cpcc JOIN FETCH cpcc.agence WHERE cpcc.agence.id = :agenceId")
    Optional<ComptePassageCommissionCollecte> findByAgenceIdWithAgence(@Param("agenceId") Long agenceId);
}