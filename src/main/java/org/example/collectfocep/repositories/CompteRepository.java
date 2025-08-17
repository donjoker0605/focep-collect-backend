package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Compte;
import org.example.collectfocep.entities.CompteSysteme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteRepository extends JpaRepository<Compte, Long> {
    Optional<Compte> findByTypeCompte(String typeCompte);

    @Query("SELECT c FROM Compte c WHERE c.typeCompte = :typeCompte AND c.id IN " +
            "(SELECT cc.id FROM CompteCollecteur cc WHERE cc.collecteur.id = :collecteurId)")
    Optional<Compte> findByCollecteurAndTypeCompte(@Param("collecteurId") Long collecteurId, @Param("typeCompte") String typeCompte);

    @Query("SELECT c FROM Compte c WHERE c.nomCompte LIKE CONCAT('%', :agenceId, '%')")
    List<Compte> findByAgenceId(@Param("agenceId") Long agenceId);

    @Query("SELECT c FROM Compte c WHERE c.nomCompte LIKE CONCAT('%', :collecteurId, '%')")
    List<Compte> findByCollecteurId(@Param("collecteurId") Long collecteurId);

    // Ajout de la méthode manquante
    @Query("SELECT c FROM Compte c WHERE c.typeCompte = :typeCompte AND c.id IN " +
            "(SELECT cc.id FROM CompteCollecteur cc WHERE cc.collecteur.id = :collecteurId)")
    Optional<Compte> findByTypeCompteAndCollecteurId(@Param("typeCompte") String typeCompte, @Param("collecteurId") Long collecteurId);

    @Query("SELECT c FROM CompteSysteme c WHERE c.typeCompte = :typeCompte")
    Optional<CompteSysteme> findSystemCompteByTypeCompte(@Param("typeCompte") String typeCompte);

    /**
     * Vérifie si un compte avec le numéro donné existe
     */
    boolean existsByNumeroCompte(String numeroCompte);

    /**
     * Trouve un compte par son numéro
     */
    Optional<Compte> findByNumeroCompte(String numeroCompte);

    /**
     * Trouve tous les comptes d'un type donné
     */
    List<Compte> findAllByTypeCompte(String typeCompte);

    /**
     * Recherche avancée pour les comptes système
     */
    @Query("SELECT c FROM Compte c WHERE c.typeCompte = :typeCompte AND c.numeroCompte LIKE :numeroPattern")
    List<Compte> findByTypeAndNumeroPattern(@Param("typeCompte") String typeCompte, @Param("numeroPattern") String numeroPattern);

    /**
     * Trouve un compte par agence et type (pour les nouveaux comptes spécialisés)
     */
    @Query("SELECT c FROM Compte c WHERE c.typeCompte = :typeCompte AND " +
           "(c.id IN (SELECT pccc.id FROM ComptePassageCommissionCollecte pccc WHERE pccc.agence.id = :agenceId) OR " +
           "c.id IN (SELECT cpt.id FROM ComptePassageTaxe cpt WHERE cpt.agence.id = :agenceId) OR " +
           "c.id IN (SELECT cpc.id FROM CompteProduitCollecte cpc WHERE cpc.agence.id = :agenceId) OR " +
           "c.id IN (SELECT ccc.id FROM CompteChargeCollecte ccc WHERE ccc.agence.id = :agenceId) OR " +
           "c.id IN (SELECT ct.id FROM CompteTaxe ct WHERE ct.agence.id = :agenceId))")
    Optional<Compte> findByAgenceIdAndTypeCompte(@Param("agenceId") Long agenceId, @Param("typeCompte") String typeCompte);

    /**
     * Trouve un compte par collecteur et type (pour C.S.C)
     */
    @Query("SELECT c FROM Compte c WHERE c.typeCompte = :typeCompte AND " +
           "c.id IN (SELECT csc.id FROM CompteSalaireCollecteur csc WHERE csc.collecteur.id = :collecteurId)")
    Optional<Compte> findByCollecteurIdAndTypeCompte(@Param("collecteurId") Long collecteurId, @Param("typeCompte") String typeCompte);

    /**
     * Vérifie si un compte existe pour une agence et un type donné
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Compte c WHERE c.typeCompte = :typeCompte AND " +
           "(c.id IN (SELECT pccc.id FROM ComptePassageCommissionCollecte pccc WHERE pccc.agence.id = :agenceId) OR " +
           "c.id IN (SELECT cpt.id FROM ComptePassageTaxe cpt WHERE cpt.agence.id = :agenceId) OR " +
           "c.id IN (SELECT cpc.id FROM CompteProduitCollecte cpc WHERE cpc.agence.id = :agenceId) OR " +
           "c.id IN (SELECT ccc.id FROM CompteChargeCollecte ccc WHERE ccc.agence.id = :agenceId) OR " +
           "c.id IN (SELECT ct.id FROM CompteTaxe ct WHERE ct.agence.id = :agenceId))")
    Boolean existsByTypeCompteAndAgenceId(@Param("typeCompte") String typeCompte, @Param("agenceId") Long agenceId);
}