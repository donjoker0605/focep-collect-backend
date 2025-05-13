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
}