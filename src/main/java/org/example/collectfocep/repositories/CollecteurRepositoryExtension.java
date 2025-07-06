package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollecteurRepositoryExtension {

    /**
     * IDs des collecteurs par agence (pour notifications)
     */
    @Query("SELECT c.id FROM Collecteur c WHERE c.agence.id = :agenceId")
    List<Long> findIdsByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Collecteur avec agence (pour notifications)
     */
    @Query("SELECT c FROM Collecteur c LEFT JOIN FETCH c.agence WHERE c.id = :collecteurId")
    Optional<Collecteur> findByIdWithAgence(@Param("collecteurId") Long collecteurId);

    /**
     * Nom du collecteur (optimisé)
     */
    @Query("SELECT CONCAT(c.nom, ' ', c.prenom) FROM Collecteur c WHERE c.id = :collecteurId")
    String findNomById(@Param("collecteurId") Long collecteurId);

}
