// Ajoutez ces méthodes à votre CollecteurRepository existant

package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.dto.CollecteurProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollecteurRepository extends JpaRepository<Collecteur, Long> {

    List<Collecteur> findByAgenceId(Long agenceId);
    Page<Collecteur> findByAgenceId(Long agenceId, Pageable pageable);
    Optional<Collecteur> findByAdresseMail(String adresseMail);


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

    @Query("SELECT c FROM Collecteur c WHERE c.agence.id = :agenceId AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.adresseMail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Collecteur> findByAgenceIdAndSearchTerm(@Param("agenceId") Long agenceId,
                                                 @Param("search") String search,
                                                 Pageable pageable);

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
     * Compte les collecteurs actifs par agence
     */
    @Query("SELECT COUNT(c) FROM Collecteur c WHERE c.agence.id = :agenceId AND c.active = true")
    Long countByAgenceIdAndActiveTrue(@Param("agenceId") Long agenceId);


    /**
     * Compte les collecteurs sans activité récente
     */
    @Query("SELECT COUNT(c) FROM Collecteur c WHERE c.active = false")
    Long countInactiveCollecteurs();

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
     * Compte les collecteurs actifs
     */
    @Query("SELECT COUNT(c) FROM Collecteur c WHERE c.active = true")
    Long countByActiveTrue();

    /**
     * Compte les collecteurs par agence
     */
    @Query("SELECT COUNT(c) FROM Collecteur c WHERE c.agence.id = :agenceId")
    Long countByAgenceId(@Param("agenceId") Long agenceId);


    long countByActiveFalse();
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
     * Statistiques par collecteur
     */
    @Query("SELECT c.id, " +
            "COUNT(cl) as totalClients, " +
            "SUM(CASE WHEN cl.valide = true THEN 1 ELSE 0 END) as activeClients " +
            "FROM Collecteur c " +
            "LEFT JOIN c.clients cl " +
            "WHERE c.id = :collecteurId " +
            "GROUP BY c.id")
    Object[] getCollecteurStats(@Param("collecteurId") Long collecteurId);

    /**
     * Collecteurs par performance (nombre de clients actifs)
     */
    @Query("SELECT c FROM Collecteur c " +
            "LEFT JOIN c.clients cl " +
            "WHERE c.active = true " +
            "GROUP BY c.id " +
            "HAVING COUNT(cl) > :minClients " +
            "ORDER BY COUNT(cl) DESC")
    List<Collecteur> findPerformantCollecteurs(@Param("minClients") Long minClients);

    /**
     * Recherche optimisée pour le dashboard admin
     */
    @Query("SELECT c FROM Collecteur c " +
            "LEFT JOIN FETCH c.agence " +
            "WHERE c.active = true " +
            "ORDER BY c.nom, c.prenom")
    List<Collecteur> findAllActiveWithAgence();

    /**
     * Collecteur par ID avec ses clients seulement
     */
    @EntityGraph(attributePaths = {"agence", "clients"})
    @Query("SELECT c FROM Collecteur c WHERE c.id = :id")
    Optional<Collecteur> findByIdWithClients(@Param("id") Long id);

    /**
     * Collecteur par ID avec son agence
     */
    @Query("SELECT c FROM Collecteur c " +
            "LEFT JOIN FETCH c.agence " +
            "WHERE c.id = :id")
    Optional<Collecteur> findByIdWithAgence(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Collecteur c WHERE c.adresseMail = :email AND c.id IN (SELECT cl.collecteur.id FROM Client cl WHERE cl.id = :clientId)")
    boolean existsByAdresseMailAndClientId(@Param("email") String email, @Param("clientId") Long clientId);

    // =====================================
    // MÉTHODES POUR VALIDATION ET MAINTENANCE
    // =====================================

    /**
     * Collecteurs sans agence (pour maintenance)
     */
    @Query("SELECT c FROM Collecteur c WHERE c.agence IS NULL")
    List<Collecteur> findOrphanCollecteurs();

    /**
     * Collecteurs avec emails dupliqués
     */
    @Query("SELECT c.adresseMail FROM Collecteur c " +
            "GROUP BY c.adresseMail " +
            "HAVING COUNT(c.adresseMail) > 1")
    List<String> findDuplicateEmails();

    /**
     * Collecteurs inactifs depuis longtemps
     */
    @Query("SELECT c FROM Collecteur c " +
            "WHERE c.active = false " +
            "AND c.dateModificationMontantMax < :cutoffDate")
    List<Collecteur> findLongInactiveCollecteurs(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT c.id FROM Collecteur c WHERE c.agence.id = :agenceId")
    List<Long> findIdsByAgenceId(@Param("agenceId") Long agenceId);

    @Query("SELECT c FROM Collecteur c JOIN FETCH c.agence")
    List<Collecteur> findAllWithAgence();

    @Query("SELECT c FROM Collecteur c JOIN FETCH c.agence WHERE c.agence.id = :agenceId")
    List<Collecteur> findByAgenceIdWithAgence(@Param("agenceId") Long agenceId);

    @Query("SELECT c FROM Collecteur c WHERE c.fcmToken IS NOT NULL")
    List<Collecteur> findCollecteursWithFCMToken();

    /**
     * 🔥 MÉTHODES POUR LE SYSTÈME D'ANCIENNETÉ
     */
    @Query("SELECT c FROM Collecteur c ORDER BY c.ancienneteEnMois DESC NULLS LAST")
    List<Collecteur> findAllByOrderByAncienneteEnMoisDesc();

    /**
     * Trouve les collecteurs inactifs depuis une date donnée
     */
    @Query("SELECT c FROM Collecteur c WHERE c.active = false AND c.dateModificationMontantMax < :date")
    List<Collecteur> findInactiveSince(@Param("date") LocalDateTime date);

    // =====================================
    // MÉTHODES POUR EXPORT EXCEL
    // =====================================

    /**
     * 📊 Compte le nombre de collecteurs supervisés par un admin (via agence)
     */
    @Query("SELECT COUNT(c) FROM Collecteur c JOIN Admin a ON c.agenceId = a.agence.id WHERE a.id = :adminId")
    Long countByAdminId(@Param("adminId") Long adminId);

    // =====================================
    // ENTITY GRAPHS POUR DONNÉES ENRICHIES
    // =====================================

    /**
     * Récupère un collecteur avec toutes ses données financières
     */
    @EntityGraph(attributePaths = {
        "agence", 
        "comptes", 
        "clients", 
        "clients.commissionParameters",
        "clients.commissionParameters.tiers"
    })
    @Query("SELECT c FROM Collecteur c WHERE c.id = :collecteurId")
    Optional<Collecteur> findByIdWithFullData(@Param("collecteurId") Long collecteurId);

    /**
     * Récupère tous les collecteurs d'une agence avec leurs données complètes
     * Séparé en deux requêtes pour éviter MultipleBagFetchException
     */
    @EntityGraph(attributePaths = {"agence", "comptes"})
    @Query("SELECT c FROM Collecteur c WHERE c.agence.id = :agenceId ORDER BY c.nom, c.prenom")
    List<Collecteur> findByAgenceIdWithFullData(@Param("agenceId") Long agenceId);
    
    /**
     * Récupère tous les collecteurs d'une agence avec leurs clients
     */
    @EntityGraph(attributePaths = {"agence", "clients"})
    @Query("SELECT c FROM Collecteur c WHERE c.agence.id = :agenceId ORDER BY c.nom, c.prenom")
    List<Collecteur> findByAgenceIdWithClients(@Param("agenceId") Long agenceId);

    /**
     * 🎯 PROJECTION JPA OPTIMISÉE - Avec vraies sous-requêtes pour les soldes
     */
    @Query("""
        SELECT 
            c.id as id,
            c.nom as nom,
            c.prenom as prenom,
            c.adresseMail as adresseMail,
            c.telephone as telephone,
            c.numeroCni as numeroCni,
            c.active as active,
            c.ancienneteEnMois as ancienneteEnMois,
            c.montantMaxRetrait as montantMaxRetrait,
            
            a.id as agenceId,
            a.nomAgence as agenceNom,
            a.codeAgence as agenceCode,
            
            (SELECT COUNT(cl.id) FROM Client cl WHERE cl.collecteur.id = c.id) as nombreClients,
            (SELECT COUNT(cl.id) FROM Client cl WHERE cl.collecteur.id = c.id AND cl.valide = true) as nombreClientsActifs,
            
            (SELECT COALESCE(SUM(CAST(cc.solde AS double)), 0.0) FROM CompteCollecteur cc WHERE cc.collecteur.id = c.id AND cc.typeCompte = 'SERVICE') as soldeCompteService,
            (SELECT COALESCE(SUM(CAST(cc.solde AS double)), 0.0) FROM CompteCollecteur cc WHERE cc.collecteur.id = c.id AND cc.typeCompte = 'REMUNERATION') as soldeCompteSalaire,
            (SELECT COALESCE(SUM(CAST(cc.solde AS double)), 0.0) FROM CompteCollecteur cc WHERE cc.collecteur.id = c.id AND cc.typeCompte = 'MANQUANT') as soldeCompteManquant,
            
            0.0 as totalEpargneCollectee,
            0.0 as totalCommissionsGagnees
            
        FROM Collecteur c
        LEFT JOIN c.agence a
        ORDER BY c.nom, c.prenom
    """)
    List<CollecteurProjection> findAllCollecteursWithData();
    
    /**
     * 🎯 PROJECTION JPA OPTIMISÉE - Un collecteur spécifique avec vraies données
     */
    @Query("""
        SELECT 
            c.id as id,
            c.nom as nom,
            c.prenom as prenom,
            c.adresseMail as adresseMail,
            c.telephone as telephone,
            c.numeroCni as numeroCni,
            c.active as active,
            c.ancienneteEnMois as ancienneteEnMois,
            c.montantMaxRetrait as montantMaxRetrait,
            
            a.id as agenceId,
            a.nomAgence as agenceNom,
            a.codeAgence as agenceCode,
            
            (SELECT COUNT(cl.id) FROM Client cl WHERE cl.collecteur.id = c.id) as nombreClients,
            (SELECT COUNT(cl.id) FROM Client cl WHERE cl.collecteur.id = c.id AND cl.valide = true) as nombreClientsActifs,
            
            (SELECT COALESCE(SUM(CAST(cc.solde AS double)), 0.0) FROM CompteCollecteur cc WHERE cc.collecteur.id = c.id AND cc.typeCompte = 'SERVICE') as soldeCompteService,
            (SELECT COALESCE(SUM(CAST(cc.solde AS double)), 0.0) FROM CompteCollecteur cc WHERE cc.collecteur.id = c.id AND cc.typeCompte = 'REMUNERATION') as soldeCompteSalaire,
            (SELECT COALESCE(SUM(CAST(cc.solde AS double)), 0.0) FROM CompteCollecteur cc WHERE cc.collecteur.id = c.id AND cc.typeCompte = 'MANQUANT') as soldeCompteManquant,
            
            0.0 as totalEpargneCollectee,
            0.0 as totalCommissionsGagnees
            
        FROM Collecteur c
        LEFT JOIN c.agence a
        WHERE c.id = :collecteurId
    """)
    CollecteurProjection findCollecteurWithDataById(@Param("collecteurId") Long collecteurId);
    
    /**
     * Récupère tous les collecteurs avec leurs comptes (étape 1/2) - FALLBACK
     */
    @EntityGraph(attributePaths = {"agence", "comptes"})
    @Query("SELECT c FROM Collecteur c ORDER BY c.nom, c.prenom")
    List<Collecteur> findAllWithComptes();
    
    /**
     * Récupère tous les collecteurs avec leurs clients (étape 2/2) 
     */
    @EntityGraph(attributePaths = {"agence", "clients"})
    @Query("SELECT c FROM Collecteur c ORDER BY c.nom, c.prenom") 
    List<Collecteur> findAllWithClients();

    /**
     * Récupère un collecteur avec ses comptes seulement
     */
    @EntityGraph(attributePaths = {"comptes", "agence"})
    @Query("SELECT c FROM Collecteur c WHERE c.id = :collecteurId")
    Optional<Collecteur> findByIdWithComptes(@Param("collecteurId") Long collecteurId);

}