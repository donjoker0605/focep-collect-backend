package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.CommissionParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionParameterRepository extends JpaRepository<CommissionParameter, Long> {

    // Existants
    Optional<CommissionParameter> findByClient(Client client);
    Optional<CommissionParameter> findByCollecteur(Collecteur collecteur);
    Optional<CommissionParameter> findByAgence(Agence agence);

    // Nouveaux pour CRUD
    @Query("SELECT cp FROM CommissionParameter cp WHERE cp.client.id = :clientId AND cp.active = true")
    List<CommissionParameter> findActiveByClientId(@Param("clientId") Long clientId);

    @Query("SELECT cp FROM CommissionParameter cp WHERE cp.collecteur.id = :collecteurId AND cp.active = true")
    List<CommissionParameter> findActiveByCollecteurId(@Param("collecteurId") Long collecteurId);

    @Query("SELECT cp FROM CommissionParameter cp WHERE cp.agence.id = :agenceId AND cp.active = true")
    List<CommissionParameter> findActiveByAgenceId(@Param("agenceId") Long agenceId);

    // Recherche avec pagination
    @Query("SELECT cp FROM CommissionParameter cp WHERE cp.active = true")
    Page<CommissionParameter> findActiveParameters(Pageable pageable);

    // Vérification d'existence
    @Query("SELECT COUNT(c) FROM Commission c WHERE c.commissionParameter.id = :parameterId")
    long countCommissionsUsingParameter(@Param("parameterId") Long parameterId);

    // Recherche par type
    @Query("SELECT cp FROM CommissionParameter cp WHERE cp.type = :type AND cp.active = true")
    List<CommissionParameter> findByTypeAndActive(@Param("type") String type);
}