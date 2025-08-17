package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Agence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgenceRepository extends JpaRepository<Agence, Long> {

    // =====================================
    // MÉTHODES DE BASE - CORRIGÉES
    // =====================================

    Optional<Agence> findByCodeAgence(String codeAgence);
    Optional<Agence> findByNomAgence(String nomAgence); // ✅ CORRECTION: nomAgence au lieu de nom
    boolean existsByCodeAgence(String codeAgence);
    boolean existsByNomAgence(String nomAgence); // ✅ CORRECTION: nomAgence au lieu de nom

    // =====================================
    // MÉTHODES DE COMPTAGE POUR DASHBOARD
    // =====================================

    long countByActiveTrue();
    long countByActiveFalse();

    @Query("SELECT COUNT(a) FROM Agence a")
    Long countAllAgences();

    @Query("SELECT COUNT(DISTINCT a) FROM Agence a " +
            "JOIN a.collecteurs c " +
            "WHERE c.active = true")
    Long countAgencesWithActiveCollecteurs();

    @Query("SELECT COUNT(a) FROM Agence a WHERE a.active = true")
    Long countActiveAgences();

    // =====================================
    // MÉTHODES DE RECHERCHE - CORRIGÉES
    // =====================================

    /**
     * Recherche par nom ou code agence
     */
    @Query("SELECT a FROM Agence a WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(a.nomAgence) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.codeAgence) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Agence> findBySearchTerm(@Param("search") String search, Pageable pageable);

    @Query("SELECT a FROM Agence a ORDER BY a.nomAgence")
    List<Agence> findAllOrderByNomAgence(); // ✅ CORRECTION: Renommé pour clarté

    @Query("SELECT DISTINCT a FROM Agence a " +
            "JOIN a.collecteurs c " +
            "WHERE c.active = true " +
            "ORDER BY a.nomAgence")
    List<Agence> findWithActiveCollecteurs();

    @Query("SELECT a FROM Agence a LEFT JOIN FETCH a.collecteurs ORDER BY a.nomAgence")
    List<Agence> findAllWithCollecteurs();

    @Query("SELECT DISTINCT a FROM Agence a " +
            "LEFT JOIN FETCH a.collecteurs c " +
            "WHERE c.active = true OR c.active IS NULL " +
            "ORDER BY a.nomAgence")
    List<Agence> findAllWithActiveCollecteurs();

    /**
     * Agences actives seulement - CORRIGÉ
     */
    List<Agence> findByActiveTrueOrderByNomAgence(); // ✅ CORRECTION PRINCIPALE: nomAgence au lieu de nom

    /**
     * Agences avec leurs collecteurs (pour statistiques)
     */
    @Query("SELECT a FROM Agence a LEFT JOIN FETCH a.collecteurs WHERE a.active = true")
    List<Agence> findActiveWithCollecteurs();

    // =====================================
    // MÉTHODES STATISTIQUES
    // =====================================

    /**
     * Statistiques par agence
     */
    @Query("SELECT a.id, a.nomAgence, " +
            "COUNT(DISTINCT c) as totalCollecteurs, " +
            "COUNT(DISTINCT cl) as totalClients " +
            "FROM Agence a " +
            "LEFT JOIN a.collecteurs c " +
            "LEFT JOIN c.clients cl " +
            "WHERE a.id = :agenceId " +
            "GROUP BY a.id, a.nomAgence")
    Object[] getAgenceStats(@Param("agenceId") Long agenceId);

    /**
     * Agences par performance
     */
    @Query("SELECT a FROM Agence a " +
            "LEFT JOIN a.collecteurs c " +
            "WHERE a.active = true " +
            "GROUP BY a.id " +
            "ORDER BY COUNT(c) DESC")
    List<Agence> findByPerformance();

    @Query("SELECT a FROM Agence a " +
            "LEFT JOIN a.collecteurs c " +
            "WHERE c.active = true OR c.active IS NULL " +
            "GROUP BY a.id " +
            "ORDER BY COUNT(c) DESC")
    List<Agence> findByPerformanceWithActiveCollecteurs();

    // =====================================
    // MÉTHODES UTILES POUR L'ADMINISTRATION
    // =====================================

    /**
     * Agences sans collecteurs
     */
    @Query("SELECT a FROM Agence a WHERE SIZE(a.collecteurs) = 0")
    List<Agence> findAgencesWithoutCollecteurs();

    /**
     * Agences avec nombre de collecteurs actifs
     */
    @Query("SELECT a, COUNT(c) as activeCollecteurs FROM Agence a " +
            "LEFT JOIN a.collecteurs c " +
            "WHERE c.active = true " +
            "GROUP BY a.id " +
            "HAVING COUNT(c) >= :minCollecteurs " +
            "ORDER BY COUNT(c) DESC")
    List<Object[]> findAgencesWithMinActiveCollecteurs(@Param("minCollecteurs") Long minCollecteurs);

    /**
     * Agences avec leurs statistiques complètes
     */
    @Query("SELECT a.id, a.nomAgence, a.codeAgence, " +
            "COUNT(DISTINCT c) as totalCollecteurs, " +
            "COUNT(DISTINCT CASE WHEN c.active = true THEN c.id END) as collecteursActifs, " +
            "COUNT(DISTINCT cl) as totalClients, " +
            "COUNT(DISTINCT CASE WHEN cl.valide = true THEN cl.id END) as clientsActifs " +
            "FROM Agence a " +
            "LEFT JOIN a.collecteurs c " +
            "LEFT JOIN c.clients cl " +
            "GROUP BY a.id, a.nomAgence, a.codeAgence " +
            "ORDER BY a.nomAgence")
    List<Object[]> getAgencesWithCompleteStats();

    /**
     * Agence par ID avec collecteurs seulement (pour éviter MultipleBagFetchException)
     */
    @Query("SELECT a FROM Agence a " +
            "LEFT JOIN FETCH a.collecteurs " +
            "WHERE a.id = :agenceId")
    Optional<Agence> findByIdWithDetails(@Param("agenceId") Long agenceId);
    
    /**
     * Agence par ID de base (sans collections)
     */
    @Query("SELECT a FROM Agence a WHERE a.id = :agenceId")
    Optional<Agence> findByIdBasic(@Param("agenceId") Long agenceId);

    // =====================================
    // MÉTHODES DE VALIDATION
    // =====================================

    /**
     * Vérifier si une agence a des collecteurs actifs
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Agence a " +
            "JOIN a.collecteurs c " +
            "WHERE a.id = :agenceId AND c.active = true")
    boolean hasActiveCollecteurs(@Param("agenceId") Long agenceId);

    /**
     * Compter les collecteurs actifs d'une agence
     */
    @Query("SELECT COUNT(c) FROM Agence a " +
            "JOIN a.collecteurs c " +
            "WHERE a.id = :agenceId AND c.active = true")
    Long countActiveCollecteursByAgence(@Param("agenceId") Long agenceId);

    /**
     * Compter les clients d'une agence
     */
    @Query("SELECT COUNT(cl) FROM Agence a " +
            "JOIN a.collecteurs c " +
            "JOIN c.clients cl " +
            "WHERE a.id = :agenceId")
    Long countClientsByAgence(@Param("agenceId") Long agenceId);

    // =====================================
    // MÉTHODES AVEC @Query POUR ÉVITER LES ERREURS
    // =====================================

    /**
     * Alternative sécurisée pour les méthodes de recherche
     */
    @Query("SELECT a FROM Agence a WHERE a.active = true ORDER BY a.nomAgence ASC")
    List<Agence> findActiveAgencesOrderedByName();

    @Query("SELECT a FROM Agence a WHERE " +
            "LOWER(a.nomAgence) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(a.codeAgence) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Agence> searchByNomOrCode(@Param("search") String search);

    @Query("SELECT COUNT(a) FROM Agence a WHERE a.active = :active")
    Long countByActive(@Param("active") boolean active);
}