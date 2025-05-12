package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.CompteSysteme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompteSystemeRepository extends JpaRepository<CompteSysteme, Long> {
    Optional<CompteSysteme> findByTypeCompte(String typeCompte);

    Optional<CompteSysteme> findByNumeroCompte(String numeroCompte);
}