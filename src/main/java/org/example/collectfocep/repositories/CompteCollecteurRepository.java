package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteCollecteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteCollecteurRepository extends JpaRepository<CompteCollecteur, Long> {

    Optional<CompteCollecteur> findByCollecteurAndTypeCompteCollecteur(
            Collecteur collecteur,
            CompteCollecteur.TypeCompteCollecteur type
    );

    List<CompteCollecteur> findByCollecteur(Collecteur collecteur);

    @Query("SELECT c FROM CompteCollecteur c WHERE c.collecteur.id = :collecteurId " +
            "AND c.typeCompteCollecteur = :type")
    Optional<CompteCollecteur> findByCollecteurIdAndType(
            @Param("collecteurId") Long collecteurId,
            @Param("type") CompteCollecteur.TypeCompteCollecteur type
    );

    @Query("SELECT SUM(c.solde) FROM CompteCollecteur c " +
            "WHERE c.collecteur.agence.id = :agenceId " +
            "AND c.typeCompteCollecteur = :type")
    Double sumSoldeByAgenceAndType(
            @Param("agenceId") Long agenceId,
            @Param("type") CompteCollecteur.TypeCompteCollecteur type
    );

    @Query("SELECT c.collecteur FROM CompteCollecteur c " +
            "WHERE c.collecteur.agence.id = :agenceId " +
            "AND c.typeCompteCollecteur = :type " +
            "AND c.solde != 0")
    List<Collecteur> findCollecteursWithNonZeroBalance(
            @Param("agenceId") Long agenceId,
            @Param("type") CompteCollecteur.TypeCompteCollecteur type
    );
}
