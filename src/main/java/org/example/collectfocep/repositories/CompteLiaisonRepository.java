package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.CompteLiaison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompteLiaisonRepository extends JpaRepository<CompteLiaison, Long> {
    @Query("SELECT c FROM CompteLiaison c WHERE c.typeCompte = :typeCompte AND c.agence = :agence")
    Optional<CompteLiaison> findByAgenceAndTypeCompte(@Param("agence") Agence agence, @Param("typeCompte") String typeCompte);

    // Ajout de la méthode manquante
    @Query("SELECT c FROM CompteLiaison c WHERE c.agence.id = :agenceId")
    Optional<CompteLiaison> findByAgenceId(@Param("agenceId") Long agenceId);
}