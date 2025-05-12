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
    Optional<CompteCollecteur> findByCollecteurAndTypeCompte(Collecteur collecteur, String typeCompte);


    Optional<CompteCollecteur> findByCollecteurAndSoldeGreaterThan(Collecteur collecteur, double solde);
    List<CompteCollecteur> findByCollecteur(Collecteur collecteur);
}
