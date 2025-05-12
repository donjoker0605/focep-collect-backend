package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.CompteClient;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CompteClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientAccountInitializationService {

    private final CompteClientRepository compteClientRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public CompteClient ensureClientAccountExists(Client client) {
        log.info("Vérification/création du compte pour le client ID: {}", client.getId());

        return compteClientRepository.findByClient(client)
                .orElseGet(() -> createClientAccount(client));
    }

    @Transactional
    public void createAccountsForAllClientsWithoutAccounts() {
        log.info("Création des comptes pour tous les clients sans compte");

        // Récupérer tous les clients
        List<Client> allClients = clientRepository.findAll();

        int comptesCreated = 0;
        for (Client client : allClients) {
            if (!compteClientRepository.findByClient(client).isPresent()) {
                createClientAccount(client);
                comptesCreated++;
            }
        }

        log.info("{} comptes clients créés", comptesCreated);
    }

    private CompteClient createClientAccount(Client client) {
        log.info("Création d'un nouveau compte épargne pour le client: {} {}",
                client.getNom(), client.getPrenom());

        CompteClient compteClient = CompteClient.builder()
                .client(client)
                .nomCompte("Compte Épargne - " + client.getNom() + " " + client.getPrenom())
                .numeroCompte(generateAccountNumber(client))
                .solde(0.0)
                .typeCompte("EPARGNE")
                .build();

        return compteClientRepository.save(compteClient);
    }

    private String generateAccountNumber(Client client) {
        String codeAgence = "DEF"; // Valeur par défaut

        if (client != null && client.getAgence() != null && client.getAgence().getCodeAgence() != null) {
            codeAgence = client.getAgence().getCodeAgence();
        }

        String clientIdFormatted = String.format("%08d", client.getId());
        return "372" + codeAgence + clientIdFormatted;
    }
}