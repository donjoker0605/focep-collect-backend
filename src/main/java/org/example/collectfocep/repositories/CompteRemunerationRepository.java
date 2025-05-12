package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteRemuneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteRemunerationRepository extends JpaRepository<CompteRemuneration, Long> {
    Optional<CompteRemuneration> findFirstByCollecteur(Collecteur collecteur);
    List<CompteRemuneration> findAllByCollecteur(Collecteur collecteur);
    boolean existsByCollecteur(Collecteur collecteur);
}