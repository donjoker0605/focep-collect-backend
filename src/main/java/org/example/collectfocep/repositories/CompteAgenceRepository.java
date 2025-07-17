package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.CompteAgence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteAgenceRepository extends JpaRepository<CompteAgence, Long> {

    /**
     * Trouve le compte agence pour une agence donnée
     */
    Optional<CompteAgence> findByAgence(Agence agence);

    /**
     * Trouve le compte agence par ID d'agence
     */
    @Query("SELECT ca FROM CompteAgence ca WHERE ca.agence.id = :agenceId")
    Optional<CompteAgence> findByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Vérifie si un compte agence existe pour une agence
     */
    boolean existsByAgence(Agence agence);

    /**
     * Trouve tous les comptes agence avec solde négatif (état normal selon logique métier)
     */
    @Query("SELECT ca FROM CompteAgence ca WHERE ca.solde < 0")
    List<CompteAgence> findComptesAgenceAvecSoldeNegatif();

    /**
     * Trouve tous les comptes agence avec solde positif (état anormal selon logique métier)
     */
    @Query("SELECT ca FROM CompteAgence ca WHERE ca.solde > 0")
    List<CompteAgence> findComptesAgenceAvecSoldePositif();

    /**
     * Calcule le total des fonds versés à toutes les agences
     */
    @Query("SELECT COALESCE(SUM(ABS(ca.solde)), 0) FROM CompteAgence ca WHERE ca.solde < 0")
    Double calculateTotalFondsVersesAgences();

    /**
     * Statistiques par agence
     */
    @Query("SELECT ca.agence.id, ca.agence.nomAgence, ca.solde, ABS(ca.solde) as montantVerse " +
            "FROM CompteAgence ca " +
            "ORDER BY ABS(ca.solde) DESC")
    List<Object[]> getStatistiquesVersementsParAgence();
}