package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.CommissionParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommissionParameterRepository extends JpaRepository<CommissionParameter, Long> {
    Optional<CommissionParameter> findByClient(Client client);
    Optional<CommissionParameter> findByCollecteur(Collecteur collecteur);
    Optional<CommissionParameter> findByAgence(Agence agence);
}
