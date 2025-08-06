package org.example.collectfocep.repositories;


import org.example.collectfocep.entities.CompteAgence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des comptes d'agence (Pour Compte Agence)
 * Extension spécialisée du CompteAgenceRepository avec des méthodes métier spécifiques
 */
@Repository
public interface PourCompteAgenceRepository extends JpaRepository<CompteAgence, Long> {
    
    /**
     * Trouve un compte agence par son ID d'agence
     */
    Optional<CompteAgence> findByAgenceId(Long agenceId);
    
    /**
     * Trouve tous les comptes agence
     */
    @Query("SELECT ca FROM CompteAgence ca")
    List<CompteAgence> findAllActive();
    
    /**
     * Trouve les comptes agence avec un solde supérieur à un montant donné
     */
    @Query("SELECT ca FROM CompteAgence ca WHERE ca.solde >= :montantMinimum")
    List<CompteAgence> findByMinimumBalance(@Param("montantMinimum") Double montantMinimum);
    
    /**
     * Trouve un compte agence par le nom de l'agence
     */
    @Query("SELECT ca FROM CompteAgence ca WHERE ca.agence.nomAgence = :nomAgence")
    Optional<CompteAgence> findByAgenceName(@Param("nomAgence") String nomAgence);
    
    /**
     * Vérifie si un compte agence existe pour une agence donnée
     */
    boolean existsByAgenceId(Long agenceId);
    
    /**
     * Compte le nombre de comptes agence
     */
    @Query("SELECT COUNT(ca) FROM CompteAgence ca")
    long countActiveAccounts();
    
    /**
     * Calcule le solde total de tous les comptes agence
     */
    @Query("SELECT COALESCE(SUM(ca.solde), 0.0) FROM CompteAgence ca")
    Double getTotalBalance();
}