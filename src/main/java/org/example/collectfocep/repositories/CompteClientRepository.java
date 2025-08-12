package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.CompteClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompteClientRepository extends JpaRepository<CompteClient, Long> {
    Optional<CompteClient> findByClient(Client client);
    List<CompteClient> findAllByClient(Client client);
    
    @Query("SELECT cc FROM CompteClient cc LEFT JOIN FETCH cc.client WHERE cc.client.id IN :clientIds")
    List<CompteClient> findByClientIdIn(@Param("clientIds") List<Long> clientIds);
}
