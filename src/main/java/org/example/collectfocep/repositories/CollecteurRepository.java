// Ajoutez ces méthodes à votre CollecteurRepository existant

package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollecteurRepository extends JpaRepository<Collecteur, Long> {

    // ✅ MÉTHODES EXISTANTES (à conserver)
    List<Collecteur> findByAgenceId(Long agenceId);
    Page<Collecteur> findByAgenceId(Long agenceId, Pageable pageable);
    Optional<Collecteur> findByAdresseMail(String adresseMail);

    // ✅ NOUVELLES MÉTHODES REQUISES POUR LE CONTRÔLEUR

    /**
     * Recherche des collecteurs par nom, prénom ou email (insensible à la casse)
     */
    @Query("SELECT c FROM Collecteur c WHERE " +
            "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.adresseMail) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Collecteur> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrAdresseMailContainingIgnoreCase(
            @Param("search") String nom,
            @Param("search") String prenom,
            @Param("search") String adresseMail,
            Pageable pageable);

    /**
     * Version simplifiée avec un seul paramètre de recherche
     */
    @Query("SELECT c FROM Collecteur c WHERE " +
            "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.adresseMail) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Collecteur> findBySearchTerm(@Param("search") String searchTerm, Pageable pageable);

    /**
     * Recherche avec jointure pour optimiser les performances
     */
    @Query("SELECT c FROM Collecteur c " +
            "LEFT JOIN FETCH c.agence a " +
            "WHERE c.adresseMail = :email")
    Optional<Collecteur> findByAdresseMailWithAgence(@Param("email") String email);

    /**
     * Compte le nombre de collecteurs actifs par agence
     */
    @Query("SELECT COUNT(c) FROM Collecteur c WHERE c.agence.id = :agenceId AND c.active = true")
    Long countActiveByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve tous les collecteurs actifs d'une agence
     */
    @Query("SELECT c FROM Collecteur c WHERE c.agence.id = :agenceId AND c.active = true")
    List<Collecteur> findActiveByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Recherche avancée avec critères multiples
     */
    @Query("SELECT c FROM Collecteur c WHERE " +
            "(:nom IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) AND " +
            "(:prenom IS NULL OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) AND " +
            "(:email IS NULL OR LOWER(c.adresseMail) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:agenceId IS NULL OR c.agence.id = :agenceId) AND " +
            "(:active IS NULL OR c.active = :active)")
    Page<Collecteur> findByMultipleCriteria(
            @Param("nom") String nom,
            @Param("prenom") String prenom,
            @Param("email") String email,
            @Param("agenceId") Long agenceId,
            @Param("active") Boolean active,
            Pageable pageable);

    /**
     * Vérifie l'existence par email
     */
    boolean existsByAdresseMail(String adresseMail);

    /**
     * Récupère l'ID de l'agence d'un collecteur
     */
    @Query("SELECT c.agence.id FROM Collecteur c WHERE c.id = :collecteurId")
    Long findAgenceIdByCollecteurId(@Param("collecteurId") Long collecteurId);

    /**
     * Compte le nombre total de collecteurs
     */
    @Query("SELECT COUNT(c) FROM Collecteur c")
    Long countAllCollecteurs();

    /**
     * Compte le nombre de collecteurs actifs
     */
    @Query("SELECT COUNT(c) FROM Collecteur c WHERE c.active = true")
    Long countActiveCollecteurs();

    /**
     * Trouve les collecteurs avec le plus de clients
     */
    @Query("SELECT c FROM Collecteur c " +
            "LEFT JOIN c.clients cl " +
            "GROUP BY c.id " +
            "ORDER BY COUNT(cl) DESC")
    Page<Collecteur> findTopCollecteursByClientCount(Pageable pageable);

    /**
     * Recherche optimisée pour le dashboard admin
     */
    @Query("SELECT c FROM Collecteur c " +
            "LEFT JOIN FETCH c.agence " +
            "WHERE c.active = true " +
            "ORDER BY c.nom, c.prenom")
    List<Collecteur> findAllActiveWithAgence();

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Collecteur c WHERE c.adresseMail = :email AND c.id IN (SELECT cl.collecteur.id FROM Client cl WHERE cl.id = :clientId)")
    boolean existsByAdresseMailAndClientId(@Param("email") String email, @Param("clientId") Long clientId);
}