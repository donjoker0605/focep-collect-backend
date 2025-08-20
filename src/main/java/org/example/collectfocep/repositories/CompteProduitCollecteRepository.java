package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.CompteProduitCollecte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteProduitCollecteRepository extends JpaRepository<CompteProduitCollecte, Long> {

    /**
     * Trouve le compte produit collecte par agence ID
     */
    @Query("SELECT cpc FROM CompteProduitCollecte cpc WHERE cpc.agence.id = :agenceId")
    CompteProduitCollecte findByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve tous les comptes produit collecte d'une agence
     */
    @Query("SELECT cpc FROM CompteProduitCollecte cpc WHERE cpc.agence.id = :agenceId")
    List<CompteProduitCollecte> findAllByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Vérifie si un compte produit collecte existe pour une agence
     */
    @Query("SELECT CASE WHEN COUNT(cpc) > 0 THEN true ELSE false END FROM CompteProduitCollecte cpc WHERE cpc.agence.id = :agenceId")
    boolean existsByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve le compte produit collecte avec les détails de l'agence
     */
    @Query("SELECT cpc FROM CompteProduitCollecte cpc JOIN FETCH cpc.agence WHERE cpc.agence.id = :agenceId")
    Optional<CompteProduitCollecte> findByAgenceIdWithAgence(@Param("agenceId") Long agenceId);
}