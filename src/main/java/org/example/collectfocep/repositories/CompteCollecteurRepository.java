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

    @Query("SELECT COUNT(c) FROM CompteCollecteur c " +
            "WHERE c.collecteur.id = :collecteurId")
    long countByCollecteurId(@Param("collecteurId") Long collecteurId);
}