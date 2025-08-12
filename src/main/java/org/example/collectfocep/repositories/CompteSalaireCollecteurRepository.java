package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteSalaireCollecteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteSalaireCollecteurRepository extends JpaRepository<CompteSalaireCollecteur, Long> {
    Optional<CompteSalaireCollecteur> findFirstByCollecteur(Collecteur collecteur);
    List<CompteSalaireCollecteur> findAllByCollecteur(Collecteur collecteur);
    boolean existsByCollecteur(Collecteur collecteur);
    Optional<CompteSalaireCollecteur> findByCollecteurId(Long collecteurId);
}