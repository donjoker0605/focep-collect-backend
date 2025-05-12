package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.entities.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ClientService {
    List<Client> getAllClients();

    Page<Client> getAllClients(Pageable pageable);

    Optional<Client> getClientById(Long id);

    Client saveClient(Client client);

    void deleteClient(Long id);

    List<Client> findByAgenceId(Long agenceId);

    Page<Client> findByAgenceId(Long agenceId, Pageable pageable);

    List<Client> findByCollecteurId(Long collecteurId);

    Page<Client> findByCollecteurId(Long collecteurId, Pageable pageable);

    Client updateClient(Client client);
}