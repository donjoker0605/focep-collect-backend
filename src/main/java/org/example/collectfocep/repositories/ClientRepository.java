package org.example.collectfocep.repositories;

import jakarta.persistence.LockModeType;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    @Query("SELECT c FROM Client c WHERE c.collecteur.id = :collecteurId")
    List<Client> findByCollecteurId(@Param("collecteurId") Long collecteurId);

    @Query("SELECT c FROM Client c WHERE c.collecteur.id = :collecteurId")
    Page<Client> findByCollecteurId(@Param("collecteurId") Long collecteurId, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.agence.id = :agenceId")
    List<Client> findByAgenceId(@Param("agenceId") Long agenceId);

    @Query("SELECT c FROM Client c WHERE c.agence.id = :agenceId")
    Page<Client> findByAgenceId(@Param("agenceId") Long agenceId, Pageable pageable);

    Optional<Client> findByNumeroCni(String numeroCni);

    @Query("SELECT c FROM Client c WHERE c.collecteur = :collecteur")
    List<Client> findByCollecteur(@Param("collecteur") Collecteur collecteur);

    @Query("SELECT c FROM Client c WHERE c.collecteur = :collecteur AND c.valide = true")
    List<Client> findActiveByCollecteur(@Param("collecteur") Collecteur collecteur);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur = :collecteur")
    long countByCollecteur(@Param("collecteur") Collecteur collecteur);

    @Query("SELECT c FROM Client c JOIN FETCH c.collecteur WHERE c.numeroCni = :numeroCni")
    Optional<Client> findByNumeroCniWithCollecteur(@Param("numeroCni") String numeroCni);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Client c WHERE c.id = :id")
    Optional<Client> findByIdForUpdate(@Param("id") Long id);

    boolean existsByNumeroCni(String numeroCni);

    @Query("SELECT c FROM Client c WHERE c.id IN (SELECT tc.clientId FROM TransfertCompteClient tc WHERE tc.transfert.id = :transferId)")
    List<Client> findByTransfertId(@Param("transferId") Long transferId);

    @Query("SELECT c.agence.id FROM Client c WHERE c.id = :clientId")
    Long findAgenceIdByClientId(@Param("clientId") Long clientId);

    // Requêtes spécifiques pour éviter les problèmes de lazy loading
    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.agence WHERE c.id = :id")
    Optional<Client> findByIdWithAgence(@Param("id") Long id);


    @Query("SELECT a.codeAgence FROM Client c JOIN c.agence a WHERE c.id = :clientId")
    String findAgenceCodeByClientId(@Param("clientId") Long clientId);

    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.collecteur col " +
            "LEFT JOIN FETCH col.agence " +
            "WHERE c.id = :id")
    Optional<Client> findByIdWithAllRelations(@Param("id") Long id);
}