package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteManquant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteManquantRepository extends JpaRepository<CompteManquant, Long> {
    Optional<CompteManquant> findFirstByCollecteur(Collecteur collecteur);
    List<CompteManquant> findAllByCollecteur(Collecteur collecteur);
    boolean existsByCollecteur(Collecteur collecteur);
    Optional<CompteManquant> findByCollecteurId(Long collecteurId);
}