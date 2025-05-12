package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteServiceRepository extends JpaRepository<CompteServiceEntity, Long> {
    Optional<CompteServiceEntity> findFirstByCollecteur(Collecteur collecteur);
    List<CompteServiceEntity> findAllByCollecteur(Collecteur collecteur);
    boolean existsByCollecteur(Collecteur collecteur);
}