package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.AdminCollecteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminCollecteurRepository extends JpaRepository<AdminCollecteur, Long> {

    /**
     * Vérifier si un admin peut gérer un collecteur spécifique
     */
    @Query("SELECT COUNT(ac) > 0 FROM AdminCollecteur ac " +
           "WHERE ac.admin.id = :adminId AND ac.collecteur.id = :collecteurId AND ac.active = true")
    boolean canAdminManageCollecteur(@Param("adminId") Long adminId, @Param("collecteurId") Long collecteurId);

    /**
     * Vérifier si un admin (par email) peut gérer un collecteur spécifique
     */
    @Query("SELECT COUNT(ac) > 0 FROM AdminCollecteur ac " +
           "JOIN ac.admin a " +
           "WHERE a.adresseMail = :adminEmail AND ac.collecteur.id = :collecteurId AND ac.active = true")
    boolean canAdminManageCollecteurByEmail(@Param("adminEmail") String adminEmail, @Param("collecteurId") Long collecteurId);

    /**
     * Récupérer tous les collecteurs gérés par un admin
     */
    @Query("SELECT ac.collecteur.id FROM AdminCollecteur ac " +
           "WHERE ac.admin.id = :adminId AND ac.active = true")
    List<Long> getCollecteurIdsByAdminId(@Param("adminId") Long adminId);

    /**
     * Récupérer tous les collecteurs gérés par un admin (par email)
     */
    @Query("SELECT ac.collecteur.id FROM AdminCollecteur ac " +
           "JOIN ac.admin a " +
           "WHERE a.adresseMail = :adminEmail AND ac.active = true")
    List<Long> getCollecteurIdsByAdminEmail(@Param("adminEmail") String adminEmail);

    /**
     * Vérifier si un admin peut accéder à un client via ses collecteurs
     */
    @Query("SELECT COUNT(ac) > 0 FROM AdminCollecteur ac " +
           "JOIN ac.admin a " +
           "JOIN ac.collecteur col " +
           "JOIN col.clients c " +
           "WHERE a.adresseMail = :adminEmail AND c.id = :clientId AND ac.active = true")
    boolean canAdminAccessClientByEmail(@Param("adminEmail") String adminEmail, @Param("clientId") Long clientId);

    /**
     * Récupérer tous les clients accessibles par un admin
     */
    @Query("SELECT DISTINCT c.id FROM AdminCollecteur ac " +
           "JOIN ac.admin a " +
           "JOIN ac.collecteur col " +
           "JOIN col.clients c " +
           "WHERE a.adresseMail = :adminEmail AND ac.active = true")
    List<Long> getAccessibleClientIdsByAdminEmail(@Param("adminEmail") String adminEmail);

    /**
     * Trouver la relation active entre admin et collecteur
     */
    Optional<AdminCollecteur> findByAdminIdAndCollecteurIdAndActiveTrue(Long adminId, Long collecteurId);

    /**
     * Trouver toutes les relations actives d'un admin
     */
    List<AdminCollecteur> findByAdminIdAndActiveTrue(Long adminId);

    /**
     * Trouver toutes les relations actives d'un collecteur
     */
    List<AdminCollecteur> findByCollecteurIdAndActiveTrue(Long collecteurId);
}