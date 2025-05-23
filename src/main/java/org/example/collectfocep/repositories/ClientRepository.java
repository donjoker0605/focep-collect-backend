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

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // ✅ REQUÊTE CORRIGÉE - Maintenant que dateCreation existe dans l'entité
    @Query("SELECT c FROM Client c WHERE c.collecteur = :collecteur AND c.valide = true ORDER BY c.dateCreation DESC")
    List<Client> findActiveByCollecteur(@Param("collecteur") Collecteur collecteur, Pageable pageable);

    // ✅ AJOUT DE MÉTHODES DE RECHERCHE UTILES
    @Query("SELECT c FROM Client c WHERE c.collecteur = :collecteur ORDER BY c.dateCreation DESC")
    List<Client> findAllByCollecteurOrderByDateCreationDesc(@Param("collecteur") Collecteur collecteur);

    @Query("SELECT c FROM Client c WHERE c.collecteur = :collecteur ORDER BY c.dateCreation DESC")
    Page<Client> findAllByCollecteurOrderByDateCreationDesc(@Param("collecteur") Collecteur collecteur, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.collecteur.id = :collecteurId AND " +
            "(LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.numeroCni LIKE CONCAT('%', :search, '%'))")
    Page<Client> findByCollecteurIdAndSearch(@Param("collecteurId") Long collecteurId,
                                             @Param("search") String search,
                                             Pageable pageable);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur = :collecteur")
    Long countByCollecteur(@Param("collecteur") Collecteur collecteur);

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

    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.agence WHERE c.id = :id")
    Optional<Client> findByIdWithAgence(@Param("id") Long id);

    @Query("SELECT a.codeAgence FROM Client c JOIN c.agence a WHERE c.id = :clientId")
    String findAgenceCodeByClientId(@Param("clientId") Long clientId);

    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.collecteur col " +
            "LEFT JOIN FETCH col.agence " +
            "WHERE c.id = :id")
    Optional<Client> findByIdWithAllRelations(@Param("id") Long id);

    // ✅ CORRIGER LA REQUÊTE DE COMPTAGE AVEC PLAGE DE DATES
    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur = :collecteur AND " +
            "c.dateCreation >= :dateDebut AND c.dateCreation < :dateFin")
    Long countByCollecteurAndDateCreationBetween(@Param("collecteur") Collecteur collecteur,
                                                 @Param("dateDebut") LocalDateTime dateDebut,
                                                 @Param("dateFin") LocalDateTime dateFin);

    // ✅ MÉTHODE POUR COMPTER PAR DATE SPÉCIFIQUE
    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur = :collecteur AND " +
            "DATE(c.dateCreation) = :date")
    Long countByCollecteurAndDateCreation(@Param("collecteur") Collecteur collecteur, @Param("date") LocalDate date);

    // ✅ RECHERCHE AVANCÉE POUR ADMIN
    @Query("SELECT c FROM Client c WHERE " +
            "(:nom IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) AND " +
            "(:prenom IS NULL OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) AND " +
            "(:numeroCni IS NULL OR c.numeroCni LIKE CONCAT('%', :numeroCni, '%')) AND " +
            "(:collecteurId IS NULL OR c.collecteur.id = :collecteurId) AND " +
            "(:agenceId IS NULL OR c.agence.id = :agenceId)")
    Page<Client> findByMultipleCriteria(@Param("nom") String nom,
                                        @Param("prenom") String prenom,
                                        @Param("numeroCni") String numeroCni,
                                        @Param("collecteurId") Long collecteurId,
                                        @Param("agenceId") Long agenceId,
                                        Pageable pageable);

    // ✅ MÉTHODES UTILES POUR LES STATISTIQUES
    @Query("SELECT COUNT(c) FROM Client c WHERE c.agence.id = :agenceId")
    Long countByAgenceId(@Param("agenceId") Long agenceId);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur.id = :collecteurId")
    Long countByCollecteurId(@Param("collecteurId") Long collecteurId);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.valide = :valide")
    Long countByValide(@Param("valide") boolean valide);
}