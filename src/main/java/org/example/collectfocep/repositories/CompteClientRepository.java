package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.CompteClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteClientRepository extends JpaRepository<CompteClient, Long> {
    Optional<CompteClient> findByClient(Client client);
    List<CompteClient> findAllByClient(Client client);
}
