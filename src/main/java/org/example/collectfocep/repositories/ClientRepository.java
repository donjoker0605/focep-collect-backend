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
import java.util.Map;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // =====================================
    // MÉTHODES DE BASE EXISTANTES
    // =====================================

    Optional<Client> findByNumeroCni(String numeroCni);
    boolean existsByNumeroCni(String numeroCni);

    // =====================================
    // MÉTHODES PAR COLLECTEUR
    // =====================================

    @Query("SELECT c FROM Client c WHERE c.collecteur.id = :collecteurId")
    List<Client> findByCollecteurId(@Param("collecteurId") Long collecteurId);

    @Query("SELECT c FROM Client c WHERE c.collecteur.id = :collecteurId ORDER BY c.dateCreation DESC")
    Page<Client> findByCollecteurId(@Param("collecteurId") Long collecteurId, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.collecteur = :collecteur ORDER BY c.dateCreation DESC")
    List<Client> findByCollecteur(@Param("collecteur") Collecteur collecteur);

    @Query("SELECT c FROM Client c WHERE c.collecteur = :collecteur ORDER BY c.dateCreation DESC")
    Page<Client> findByCollecteur(@Param("collecteur") Collecteur collecteur, Pageable pageable);


    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur = :collecteur")
    Long countByCollecteur(@Param("collecteur") Collecteur collecteur);

    // =====================================
    // MÉTHODES PAR AGENCE
    // =====================================

    @Query("SELECT c FROM Client c WHERE c.agence.id = :agenceId")
    List<Client> findByAgenceId(@Param("agenceId") Long agenceId);

    @Query("SELECT c FROM Client c WHERE c.agence.id = :agenceId")
    Page<Client> findByAgenceId(@Param("agenceId") Long agenceId, Pageable pageable);

    /**
     * Compte le nombre de clients par agence
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.agence.id = :agenceId")
    Long countByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Trouve les clients d'une agence avec recherche textuelle
     */
    @Query("SELECT c FROM Client c WHERE c.agence.id = :agenceId AND " +
            "(LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.telephone LIKE CONCAT('%', :search, '%') OR " +
            "c.numeroCni LIKE CONCAT('%', :search, '%') OR " +
            "c.numeroCompte LIKE CONCAT('%', :search, '%'))")
    Page<Client> findByAgenceIdAndSearchQuery(@Param("agenceId") Long agenceId,
                                              @Param("search") String search,
                                              Pageable pageable);

    /**
     * Trouve les clients d'une agence par statut
     */
    Page<Client> findByAgenceIdAndValide(Long agenceId, Boolean valide, Pageable pageable);

    /**
     * Trouve les clients d'une agence et d'un collecteur spécifique
     */
    Page<Client> findByAgenceIdAndCollecteurId(Long agenceId, Long collecteurId, Pageable pageable);

    /**
     * Trouve les clients d'une agence, d'un collecteur avec recherche
     */
    @Query("SELECT c FROM Client c WHERE c.agence.id = :agenceId AND c.collecteur.id = :collecteurId AND " +
            "(LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.telephone LIKE CONCAT('%', :search, '%'))")
    Page<Client> findByAgenceIdAndCollecteurIdAndSearchQuery(@Param("agenceId") Long agenceId,
                                                             @Param("collecteurId") Long collecteurId,
                                                             @Param("search") String search,
                                                             Pageable pageable);

    /**
     * Compte les clients validés par agence
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.agence.id = :agenceId AND c.valide = true")
    Long countByAgenceIdAndValideTrue(@Param("agenceId") Long agenceId);


    // =====================================
    // MÉTHODES DE COMPTAGE POUR DASHBOARD
    // =====================================

    /**
     * Compte les clients validés (actifs)
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.valide = true")
    Long countByValideTrue();


    long countByValideFalse();

    @Query("SELECT COUNT(c) FROM Client c WHERE c.valide = :valide")
    Long countByValide(@Param("valide") boolean valide);

    // =====================================
    // MÉTHODES DE RECHERCHE OPTIMISÉES
    // =====================================

    /**
     * Recherche clients par collecteur et terme de recherche
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.collecteur.id = :collecteurId " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.numeroCni) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.telephone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.dateCreation DESC")
    Page<Client> findByCollecteurIdAndSearch(@Param("collecteurId") Long collecteurId,
                                             @Param("search") String search,
                                             Pageable pageable);

    /**
     * Recherche clients par collecteur et terme de recherche (alias pour compatibilité)
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.collecteur.id = :collecteurId " +
            "AND (:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(c.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.numeroCni) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.telephone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY c.dateCreation DESC")
    Page<Client> findByCollecteurIdAndSearchTerm(@Param("collecteurId") Long collecteurId,
                                                 @Param("searchTerm") String searchTerm,
                                                 Pageable pageable);

    /**
     * Recherche avancée avec critères multiples
     */
    @Query("SELECT c FROM Client c WHERE " +
            "(:nom IS NULL OR :nom = '' OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) AND " +
            "(:prenom IS NULL OR :prenom = '' OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) AND " +
            "(:numeroCni IS NULL OR :numeroCni = '' OR c.numeroCni LIKE CONCAT('%', :numeroCni, '%')) AND " +
            "(:collecteurId IS NULL OR c.collecteur.id = :collecteurId) AND " +
            "(:agenceId IS NULL OR c.agence.id = :agenceId) AND " +
            "(:valide IS NULL OR c.valide = :valide)")
    Page<Client> findByMultipleCriteria(@Param("nom") String nom,
                                        @Param("prenom") String prenom,
                                        @Param("numeroCni") String numeroCni,
                                        @Param("collecteurId") Long collecteurId,
                                        @Param("agenceId") Long agenceId,
                                        @Param("valide") Boolean valide,
                                        Pageable pageable);

    // =====================================
    // MÉTHODES AVEC FETCH OPTIMISÉES
    // =====================================

    /**
     * Client par CNI avec collecteur (pour sécurité)
     */
    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.collecteur " +
            "WHERE c.numeroCni = :numeroCni")
    Optional<Client> findByNumeroCniWithCollecteur(@Param("numeroCni") String numeroCni);

    /**
     * Client par ID avec agence
     */
    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.agence " +
            "WHERE c.id = :id")
    Optional<Client> findByIdWithAgence(@Param("id") Long id);

    /**
     * Client avec toutes ses relations
     */
    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.collecteur " +
            "LEFT JOIN FETCH c.agence " +
            "WHERE c.id = :clientId")
    Optional<Client> findByIdWithAllRelations(@Param("clientId") Long clientId);

    // =====================================
    // MÉTHODES DE SÉCURITÉ
    // =====================================

    /**
     * Récupère l'ID de l'agence d'un client (pour sécurité)
     */
    @Query("SELECT c.agence.id FROM Client c WHERE c.id = :clientId")
    Long findAgenceIdByClientId(@Param("clientId") Long clientId);

    /**
     * Vérifie si un client appartient à une agence
     */
    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.id = :clientId AND c.agence.id = :agenceId")
    boolean existsByIdAndAgenceId(@Param("clientId") Long clientId, @Param("agenceId") Long agenceId);

    /**
     * Trouve les clients d'une agence avec leurs collecteurs
     */
    @Query("SELECT c FROM Client c " +
            "LEFT JOIN FETCH c.collecteur " +
            "LEFT JOIN FETCH c.agence " +
            "WHERE c.agence.id = :agenceId")
    List<Client> findByAgenceIdWithCollecteur(@Param("agenceId") Long agenceId);


    /**
     * Code agence d'un client
     */
    @Query("SELECT a.codeAgence FROM Client c JOIN c.agence a WHERE c.id = :clientId")
    String findAgenceCodeByClientId(@Param("clientId") Long clientId);

    // =====================================
    // MÉTHODES PAR STATUT ET ACTIVITÉ
    // =====================================

    /**
     * Trouve les clients d'un collecteur avec recherche
     */
    @Query("SELECT c FROM Client c WHERE c.collecteur.id = :collecteurId AND " +
            "(LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.telephone LIKE CONCAT('%', :search, '%'))")
    Page<Client> findByCollecteurIdAndSearchQuery(@Param("collecteurId") Long collecteurId,
                                                  @Param("search") String search,
                                                  Pageable pageable);

    /**
     * Recherche globale dans tous les clients
     */
    @Query("SELECT c FROM Client c WHERE " +
            "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.telephone LIKE CONCAT('%', :search, '%') OR " +
            "c.numeroCni LIKE CONCAT('%', :search, '%') OR " +
            "c.numeroCompte LIKE CONCAT('%', :search, '%')")
    Page<Client> findBySearchQuery(@Param("search") String search, Pageable pageable);

    /**
     * Trouve les clients par statut (global)
     */
    Page<Client> findByValide(Boolean valide, Pageable pageable);

    /**
     * Clients par collecteur et statut
     */
    List<Client> findByCollecteurAndValide(Collecteur collecteur, boolean valide);

    /**
     * Clients actifs d'un collecteur avec activité récente
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.collecteur = :collecteur " +
            "AND c.valide = true " +
            "ORDER BY c.dateCreation DESC")
    List<Client> findActiveByCollecteur(@Param("collecteur") Collecteur collecteur, Pageable pageable);

    /**
     * Compte le nombre de clients par collecteur (requête groupée optimisée)
     */
    @Query("SELECT c.collecteur.id, COUNT(c) FROM Client c WHERE c.collecteur.id IN :collecteurIds GROUP BY c.collecteur.id")
    List<Object[]> countByCollecteurIds(@Param("collecteurIds") List<Long> collecteurIds);

    // =====================================
    // MÉTHODES STATISTIQUES ET DATES
    // =====================================

    /**
     * Nouveaux clients entre deux dates
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur = :collecteur AND " +
            "c.dateCreation >= :dateDebut AND c.dateCreation < :dateFin")
    Long countByCollecteurAndDateCreationBetween(@Param("collecteur") Collecteur collecteur,
                                                 @Param("dateDebut") LocalDateTime dateDebut,
                                                 @Param("dateFin") LocalDateTime dateFin);

    /**
     * Nouveaux clients d'un collecteur pour une date
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur = :collecteur " +
            "AND DATE(c.dateCreation) = :date")
    Long countByCollecteurAndDateCreation(@Param("collecteur") Collecteur collecteur,
                                          @Param("date") LocalDate date);

    /**
     * Statistiques des clients par collecteur
     */
    @Query("SELECT " +
            "COUNT(c) as totalClients, " +
            "SUM(CASE WHEN c.valide = true THEN 1 ELSE 0 END) as activeClients, " +
            "SUM(CASE WHEN c.valide = false THEN 1 ELSE 0 END) as inactiveClients " +
            "FROM Client c " +
            "WHERE c.collecteur.id = :collecteurId")
    Object[] getClientStatsByCollecteur(@Param("collecteurId") Long collecteurId);

    // =====================================
    // MÉTHODES AVEC SOLDES ET FINANCES
    // =====================================

    /**
     * Clients avec solde positif
     */
    @Query("SELECT c FROM Client c " +
            "JOIN CompteClient cc ON cc.client = c " +
            "WHERE c.collecteur.id = :collecteurId " +
            "AND cc.solde > 0 " +
            "ORDER BY cc.solde DESC")
    List<Client> findClientsWithPositiveBalance(@Param("collecteurId") Long collecteurId, Pageable pageable);

    // =====================================
    // MÉTHODES RÉCENTES ET TENDANCES
    // =====================================

    /**
     * Derniers clients créés par collecteur
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.collecteur.id = :collecteurId " +
            "ORDER BY c.dateCreation DESC")
    List<Client> findRecentClientsByCollecteur(@Param("collecteurId") Long collecteurId, Pageable pageable);

    /**
     * Tous les clients par collecteur, triés par date de création
     */
    @Query("SELECT c FROM Client c WHERE c.collecteur = :collecteur ORDER BY c.dateCreation DESC")
    List<Client> findAllByCollecteurOrderByDateCreationDesc(@Param("collecteur") Collecteur collecteur);

    @Query("SELECT c FROM Client c WHERE c.collecteur = :collecteur ORDER BY c.dateCreation DESC")
    Page<Client> findAllByCollecteurOrderByDateCreationDesc(@Param("collecteur") Collecteur collecteur, Pageable pageable);

    /**
     * Clients récemment modifiés
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.collecteur.id = :collecteurId " +
            "AND c.dateModification IS NOT NULL " +
            "ORDER BY c.dateModification DESC")
    List<Client> findRecentlyModifiedByCollecteur(@Param("collecteurId") Long collecteurId, Pageable pageable);

    // =====================================
    // MÉTHODES DE TRANSFERT
    // =====================================

    /**
     * Clients par transfert
     */
    @Query("SELECT c FROM Client c WHERE c.id IN " +
            "(SELECT tc.clientId FROM TransfertCompteClient tc WHERE tc.transfert.id = :transferId)")
    List<Client> findByTransfertId(@Param("transferId") Long transferId);

    // =====================================
    // MÉTHODES DE VERROUILLAGE ET TRANSACTION
    // =====================================

    /**
     * Client avec verrou pessimiste (pour mise à jour sécurisée)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Client c WHERE c.id = :id")
    Optional<Client> findByIdForUpdate(@Param("id") Long id);

    // =====================================
    // MÉTHODES DE VALIDATION ET MAINTENANCE
    // =====================================

    /**
     * Clients orphelins (sans collecteur)
     */
    @Query("SELECT c FROM Client c WHERE c.collecteur IS NULL")
    List<Client> findOrphanClients();

    /**
     * Clients avec CNI dupliqués
     */
    @Query("SELECT c.numeroCni FROM Client c " +
            "GROUP BY c.numeroCni " +
            "HAVING COUNT(c.numeroCni) > 1")
    List<String> findDuplicateCnis();

    /**
     * Clients inactifs depuis longtemps
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.valide = false " +
            "AND c.dateModification < :cutoffDate")
    List<Client> findLongInactiveClients(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(DISTINCT m.client) FROM Mouvement m " +
            "WHERE m.collecteur.id = :collecteurId " +
            "AND DATE(m.dateOperation) BETWEEN :dateDebut AND :dateFin")
    Long countActiveByCollecteurId(@Param("collecteurId") Long collecteurId,
                                   @Param("dateDebut") LocalDate dateDebut,
                                   @Param("dateFin") LocalDate dateFin);

    /**
     * Clients sans transactions récentes
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.id NOT IN (" +
            "    SELECT DISTINCT m.client.id FROM Mouvement m " +
            "    WHERE m.client.id IS NOT NULL " +
            "    AND m.dateOperation > :sinceDate" +
            ") " +
            "AND c.collecteur.id = :collecteurId")
    List<Client> findClientsWithoutRecentTransactions(@Param("collecteurId") Long collecteurId,
                                                      @Param("sinceDate") LocalDateTime sinceDate);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.valide = :valide")
    Long countByValidé(@Param("valide") boolean valide);


    @Query("SELECT COUNT(c) FROM Client c WHERE c.agence.id = :agenceId AND c.valide = :valide")
    Long countByAgenceIdAndValidé(@Param("agenceId") Long agenceId, @Param("valide") boolean valide);


    /**
     * Clients par collecteur (si cette méthode n'existe pas déjà)
     */
    @Query("SELECT c FROM Client c WHERE c.collecteur.id = :collecteurId")
    List<Client> findByCollecteur(@Param("collecteurId") Long collecteurId);

    /**
     * Compte le nombre de clients actifs par agence
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.agence.id = :agenceId AND c.valide = true")
    Long countActiveByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Compte le nombre de clients par collecteur
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur.id = :collecteurId")
    Long countByCollecteurId(@Param("collecteurId") Long collecteurId);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur.id = :collecteurId AND c.valide = true")
    Long countByCollecteurIdAndValideTrue(@Param("collecteurId") Long collecteurId);

    /**
     * Récupère les statistiques de clients par agence
     */
    @Query("SELECT NEW map(c.agence.id as agenceId, " +
            "c.agence.nomAgence as agenceNom, " +
            "COUNT(c) as totalClients, " +
            "SUM(CASE WHEN c.valide = true THEN 1 ELSE 0 END) as clientsActifs) " +
            "FROM Client c " +
            "GROUP BY c.agence.id, c.agence.nomAgence")
    List<Map<String, Object>> getClientStatsByAgence();

    /**
     * Récupère les statistiques de clients par collecteur pour une agence
     */
    @Query("SELECT NEW map(c.collecteur.id as collecteurId, " +
            "c.collecteur.nom as collecteurNom, " +
            "c.collecteur.prenom as collecteurPrenom, " +
            "COUNT(c) as totalClients, " +
            "SUM(CASE WHEN c.valide = true THEN 1 ELSE 0 END) as clientsActifs) " +
            "FROM Client c " +
            "WHERE c.agence.id = :agenceId " +
            "GROUP BY c.collecteur.id, c.collecteur.nom, c.collecteur.prenom")
    List<Map<String, Object>> getClientStatsByCollecteurInAgence(@Param("agenceId") Long agenceId);

    /**
     * Alternative avec paramètre valide flexible
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.collecteur.id = :collecteurId AND c.valide = :valide")
    Long countByCollecteurIdAndValide(@Param("collecteurId") Long collecteurId, @Param("valide") boolean valide);

    /**
     * Recherche des clients dans un rayon géographique
     * Utilise la formule de distance euclidienne simplifiée (approximation)
     * 111 km = 1 degré de latitude/longitude (approximation)
     */
    @Query("SELECT c FROM Client c WHERE " +
            "c.latitude IS NOT NULL AND c.longitude IS NOT NULL AND " +
            "(SQRT(POWER(c.latitude - :latitude, 2) + POWER(c.longitude - :longitude, 2)) * 111) <= :radiusKm " +
            "ORDER BY SQRT(POWER(c.latitude - :latitude, 2) + POWER(c.longitude - :longitude, 2))")
    List<Client> findClientsInRadius(@Param("latitude") Double latitude,
                                     @Param("longitude") Double longitude,
                                     @Param("radiusKm") Double radiusKm);

    /**
     * Recherche client par collecteur et numéro de compte
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.collecteur.id = :collecteurId " +
            "AND c.numeroCompte = :numeroCompte")
    Optional<Client> findByCollecteurIdAndNumeroCompte(
            @Param("collecteurId") Long collecteurId,
            @Param("numeroCompte") String numeroCompte);

    /**
     * Recherche rapide avec limite pour autocomplete
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.collecteur.id = :collecteurId " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.numeroCompte) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY c.dateModification DESC")
    List<Client> findTopByCollecteurIdAndSearchOrderByDateModificationDesc(
            @Param("collecteurId") Long collecteurId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Vérifier si client a un téléphone
     */
    @Query("SELECT CASE WHEN (c.telephone IS NOT NULL AND c.telephone != '') THEN true ELSE false END " +
            "FROM Client c WHERE c.id = :clientId")
    Boolean hasValidPhone(@Param("clientId") Long clientId);

    /**
     * Recherche client par numéro de compte
     */
    @Query("SELECT c FROM Client c WHERE c.numeroCompte = :numeroCompte")
    Optional<Client> findByNumeroCompte(@Param("numeroCompte") String numeroCompte);

    /**
     * Recherche client par numéro de compte et collecteur (sécurisé)
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.numeroCompte = :numeroCompte " +
            "AND c.collecteur.id = :collecteurId")
    Optional<Client> findByNumeroCompteAndCollecteurId(
            @Param("numeroCompte") String numeroCompte,
            @Param("collecteurId") Long collecteurId);

    /**
     * Recherche partielle par numéro de compte (pour autocomplete)
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.collecteur.id = :collecteurId " +
            "AND c.numeroCompte LIKE CONCAT('%', :numeroCompte, '%') " +
            "ORDER BY c.numeroCompte ASC")
    List<Client> findByPartialNumeroCompteAndCollecteurId(
            @Param("numeroCompte") String numeroCompte,
            @Param("collecteurId") Long collecteurId,
            Pageable pageable);

    /**
     * Vérifier existence numéro de compte pour un collecteur
     */
    @Query("SELECT COUNT(c) > 0 FROM Client c " +
            "WHERE c.numeroCompte = :numeroCompte " +
            "AND c.collecteur.id = :collecteurId")
    Boolean existsByNumeroCompteAndCollecteurId(
            @Param("numeroCompte") String numeroCompte,
            @Param("collecteurId") Long collecteurId);

    /**
     * 🔍 AMÉLIORATION : Recherche optimisée avec nom ET numéro de compte
     */
    @Query("SELECT c FROM Client c " +
            "WHERE c.collecteur.id = :collecteurId " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.numeroCompte LIKE CONCAT('%', :search, '%') OR " +
            "LOWER(c.numeroCni) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY " +
            "CASE WHEN c.numeroCompte LIKE CONCAT(:search, '%') THEN 1 " +
            "     WHEN LOWER(c.nom) LIKE LOWER(CONCAT(:search, '%')) THEN 2 " +
            "     ELSE 3 END, c.dateModification DESC")
    Page<Client> findByCollecteurIdAndSearchOptimized(
            @Param("collecteurId") Long collecteurId,
            @Param("search") String search,
            Pageable pageable);

    // =====================================
// MÉTHODES POUR LES LISTES D'IDS
// =====================================

    /**
     * Récupère tous les IDs de clients (pour super admin)
     */
    @Query("SELECT c.id FROM Client c")
    List<Long> findAllClientIds();

    /**
     * Récupère les IDs des clients d'une agence
     */
    @Query("SELECT c.id FROM Client c WHERE c.agence.id = :agenceId")
    List<Long> findClientIdsByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Récupère les IDs des clients d'un collecteur
     */
    @Query("SELECT c.id FROM Client c WHERE c.collecteur.id = :collecteurId")
    List<Long> findClientIdsByCollecteurId(@Param("collecteurId") Long collecteurId);

    // =====================================
// MÉTHODES POUR LA GÉOLOCALISATION ADMIN
// =====================================

    /**
     * Trouve les clients d'une agence avec localisation
     */
    @Query("SELECT c FROM Client c WHERE c.agence.id = :agenceId AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL")
    List<Client> findByAgenceIdWithLocation(@Param("agenceId") Long agenceId);

    /**
     * Trouve les clients d'une agence sans localisation
     */
    @Query("SELECT c FROM Client c WHERE c.agence.id = :agenceId AND (c.latitude IS NULL OR c.longitude IS NULL)")
    List<Client> findByAgenceIdWithoutLocation(@Param("agenceId") Long agenceId);

    /**
     * Statistiques de géolocalisation par agence
     */
    @Query("SELECT NEW map(" +
            "COUNT(c) as totalClients, " +
            "SUM(CASE WHEN c.latitude IS NOT NULL AND c.longitude IS NOT NULL THEN 1 ELSE 0 END) as withLocation, " +
            "SUM(CASE WHEN c.coordonneesSaisieManuelle = true THEN 1 ELSE 0 END) as manualLocation) " +
            "FROM Client c WHERE c.agence.id = :agenceId")
    Map<String, Object> getLocationStatsByAgence(@Param("agenceId") Long agenceId);
}