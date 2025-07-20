package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByAdresseMail(String email);

    @Query("SELECT a FROM Admin a WHERE a.agence.id = :agenceId")
    Page<Admin> findByAgenceId(@Param("agenceId") Long agenceId, Pageable pageable);

    boolean existsByAdresseMail(String email);

    boolean existsByNumeroCni(String numeroCni);


    /**
     * Vérifie si un admin existe avec un email et un ID d'agence spécifique
     * ATTENTION: Cette méthode doit être utilisée avec prudence dans les vérifications de sécurité
     */
    @Query("SELECT COUNT(a) > 0 FROM Admin a WHERE a.adresseMail = :email AND a.agence.id = :agenceId")
    boolean existsByAdresseMailAndAgenceId(@Param("email") String email, @Param("agenceId") Long agenceId);

    // =====================================
// NOUVELLES MÉTHODES POUR CORRIGER LA SÉCURITÉ
// =====================================

    /**
     * Vérifie si un admin peut accéder à un client
     * Cette méthode combine la vérification de l'admin et la vérification que le client est dans son agence
     */
    @Query("SELECT COUNT(a) > 0 FROM Admin a, Client c WHERE " +
            "a.adresseMail = :adminEmail AND " +
            "c.id = :clientId AND " +
            "a.agence.id = c.agence.id")
    boolean canAdminAccessClient(@Param("adminEmail") String adminEmail, @Param("clientId") Long clientId);

    /**
     * Vérifie si un admin peut accéder à un collecteur
     */
    @Query("SELECT COUNT(a) > 0 FROM Admin a, Collecteur col WHERE " +
            "a.adresseMail = :adminEmail AND " +
            "col.id = :collecteurId AND " +
            "a.agence.id = col.agence.id")
    boolean canAdminAccessCollecteur(@Param("adminEmail") String adminEmail, @Param("collecteurId") Long collecteurId);

    /**
     * Récupère l'ID de l'agence d'un admin par son email
     */
    @Query("SELECT a.agence.id FROM Admin a WHERE a.adresseMail = :email")
    Long findAgenceIdByEmail(@Param("email") String email);

    /**
     * Vérifie si un admin appartient à une agence spécifique
     */
    @Query("SELECT COUNT(a) > 0 FROM Admin a WHERE a.adresseMail = :email AND a.agence.id = :agenceId")
    boolean isAdminInAgence(@Param("email") String email, @Param("agenceId") Long agenceId);

    /**
     * Récupère les informations complètes d'un admin avec son agence
     */
    @Query("SELECT a FROM Admin a LEFT JOIN FETCH a.agence WHERE a.adresseMail = :email")
    Optional<Admin> findByAdresseMailWithAgence(@Param("email") String email);

    /**
     * Récupère tous les admins d'une agence
     */
    @Query("SELECT a FROM Admin a WHERE a.agence.id = :agenceId")
    List<Admin> findByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Vérifie si un admin est le seul admin de son agence
     */
    @Query("SELECT COUNT(a) FROM Admin a WHERE a.agence.id = (SELECT a2.agence.id FROM Admin a2 WHERE a2.adresseMail = :email)")
    Long countAdminsInSameAgence(@Param("email") String email);

    // =====================================
// MÉTHODES POUR LES STATISTIQUES ADMIN
// =====================================

    /**
     * Compte le nombre total d'admins
     */
    @Query("SELECT COUNT(a) FROM Admin a")
    Long countAllAdmins();

    /**
     * Compte le nombre d'admins par agence
     */
    @Query("SELECT COUNT(a) FROM Admin a WHERE a.agence.id = :agenceId")
    Long countByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Récupère les statistiques des admins par agence
     */
    @Query("SELECT NEW map(a.agence.id as agenceId, " +
            "a.agence.nomAgence as agenceNom, " +
            "COUNT(a) as nombreAdmins) " +
            "FROM Admin a " +
            "GROUP BY a.agence.id, a.agence.nomAgence")
    List<Map<String, Object>> getAdminStatsByAgence();

    // =====================================
// MÉTHODES POUR LA VALIDATION DES PERMISSIONS
// =====================================

    /**
     * MÉTHODE DE VALIDATION: Vérifie que l'admin et l'entité sont dans la même agence
     * Méthode générique qui peut être utilisée pour différents types d'entités
     */
    @Query("SELECT a.agence.id FROM Admin a WHERE a.adresseMail = :adminEmail")
    Optional<Long> getAdminAgenceId(@Param("adminEmail") String adminEmail);

    /**
     * MÉTHODE D'AUDIT: Trouve tous les admins qui ont accès à une agence spécifique
     */
    @Query("SELECT a FROM Admin a WHERE a.agence.id = :agenceId ORDER BY a.nom, a.prenom")
    List<Admin> findAdminsWithAccessToAgence(@Param("agenceId") Long agenceId);

    /**
     * MÉTHODE DE DEBUG: Récupère les informations détaillées d'un admin pour le debug
     */
    @Query("SELECT NEW map(a.id as adminId, " +
            "a.nom as nom, " +
            "a.prenom as prenom, " +
            "a.adresseMail as email, " +
            "a.agence.id as agenceId, " +
            "a.agence.nomAgence as agenceNom, " +
            "a.role as role) " +
            "FROM Admin a WHERE a.adresseMail = :email")
    Optional<Map<String, Object>> getAdminDebugInfo(@Param("email") String email);
}