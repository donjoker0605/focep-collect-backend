package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.TransfertCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransfertCompteRepository extends JpaRepository<TransfertCompte, Long> {
    List<TransfertCompte> findBySourceCollecteurId(Long collecteurId);

    List<TransfertCompte> findByTargetCollecteurId(Long collecteurId);

    List<TransfertCompte> findByDateTransfertBetween(LocalDateTime debut, LocalDateTime fin);

    List<TransfertCompte> findByIsInterAgence(boolean isInterAgence);
}