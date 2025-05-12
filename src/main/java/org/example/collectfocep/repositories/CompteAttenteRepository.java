package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteAttente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteAttenteRepository extends JpaRepository<CompteAttente, Long> {
    Optional<CompteAttente> findFirstByCollecteur(Collecteur collecteur);
    List<CompteAttente> findAllByCollecteur(Collecteur collecteur);
    boolean existsByCollecteur(Collecteur collecteur);
}