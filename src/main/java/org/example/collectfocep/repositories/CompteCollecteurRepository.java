package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteCollecteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteCollecteurRepository extends JpaRepository<CompteCollecteur, Long> {

    // MÉTHODE PRINCIPALE POUR OPTION A
    Optional<CompteCollecteur> findByCollecteurAndTypeCompte(
            Collecteur collecteur,
            String typeCompte
    );

    // MÉTHODES STANDARDS OPTION A
    List<CompteCollecteur> findByCollecteur(Collecteur collecteur);

    Optional<CompteCollecteur> findByCollecteurAndSoldeGreaterThan(
            Collecteur collecteur,
            double solde
    );

    @Query("SELECT c FROM CompteCollecteur c WHERE c.collecteur.id = :collecteurId " +
            "AND c.typeCompte = :typeCompte")
    Optional<CompteCollecteur> findByCollecteurIdAndTypeCompte(
            @Param("collecteurId") Long collecteurId,
            @Param("typeCompte") String typeCompte
    );

    @Query("SELECT c FROM CompteCollecteur c WHERE c.collecteur.agence.id = :agenceId " +
            "AND c.typeCompte = :typeCompte")
    List<CompteCollecteur> findByAgenceIdAndTypeCompte(
            @Param("agenceId") Long agenceId,
            @Param("typeCompte") String typeCompte
    );

    @Query("SELECT SUM(c.solde) FROM CompteCollecteur c " +
            "WHERE c.collecteur.agence.id = :agenceId " +
            "AND c.typeCompte = :typeCompte")
    Double sumSoldeByAgenceAndTypeCompte(
            @Param("agenceId") Long agenceId,
            @Param("typeCompte") String typeCompte
    );

    @Query("SELECT c.collecteur FROM CompteCollecteur c " +
            "WHERE c.collecteur.agence.id = :agenceId " +
            "AND c.typeCompte = :typeCompte " +
            "AND c.solde != 0")
    List<Collecteur> findCollecteursWithNonZeroBalanceByType(
            @Param("agenceId") Long agenceId,
            @Param("typeCompte") String typeCompte
    );

    // VÉRIFICATIONS EXISTENCE
    boolean existsByCollecteurAndTypeCompte(Collecteur collecteur, String typeCompte);

    /**
     * Compte le nombre de comptes par collecteur (requête groupée optimisée)
     */
    @Query("SELECT cc.collecteur.id, COUNT(cc) FROM CompteCollecteur cc WHERE cc.collecteur.id IN :collecteurIds GROUP BY cc.collecteur.id")
    List<Object[]> countByCollecteurIds(@Param("collecteurIds") List<Long> collecteurIds);

    /**
     * Compte les comptes liés à un collecteur (puisque la notion "actif" n'existe pas dans la table)
     */
    @Query("SELECT COUNT(cc) FROM CompteCollecteur cc WHERE cc.collecteur.id = :collecteurId")
    Long countByCollecteurId(@Param("collecteurId") Long collecteurId);
}