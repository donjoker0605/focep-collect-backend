package org.example.collectfocep.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.Validation.ClientValidator;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedAccessException;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.services.interfaces.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final CollecteurRepository collecteurRepository;
    private final AgenceRepository agenceRepository;

    @Autowired
    private ClientValidator clientValidator;

    @Autowired
    public ClientServiceImpl(
            ClientRepository clientRepository,
            CollecteurRepository collecteurRepository,
            AgenceRepository agenceRepository) {
        this.clientRepository = clientRepository;
        this.collecteurRepository = collecteurRepository;
        this.agenceRepository = agenceRepository;
    }

    @Override
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    @Override
    public Page<Client> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    @Override
    @Cacheable(value = "clients", key = "#id")
    public Optional<Client> getClientById(Long id) {
        return clientRepository.findById(id);
    }

    @Override
    @Transactional
    public Client saveClient(Client client) {
        log.info("Tentative de sauvegarde d'un client: {}", client.getNumeroCni());

        // Validation des champs obligatoires
        validateClientBeforeSave(client);

        // Validation et récupération du collecteur
        if (client.getCollecteur() == null || client.getCollecteur().getId() == null) {
            throw new BusinessException("L'ID du collecteur est requis", "MISSING_COLLECTEUR_ID",
                    "Veuillez spécifier un collecteur valide pour ce client");
        }

        Long collecteurId = client.getCollecteur().getId();
        log.debug("Chargement du collecteur avec ID: {}", collecteurId);

        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur", "id", collecteurId));
        client.setCollecteur(collecteur);

        // Validation et récupération de l'agence
        if (client.getAgence() == null || client.getAgence().getId() == null) {
            throw new BusinessException("L'ID de l'agence est requis", "MISSING_AGENCE_ID",
                    "Veuillez spécifier une agence valide pour ce client");
        }

        Long agenceId = client.getAgence().getId();
        log.debug("Chargement de l'agence avec ID: {}", agenceId);

        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence", "id", agenceId));
        client.setAgence(agence);

        // Vérification de cohérence : le collecteur doit appartenir à la même agence
        if (!collecteur.getAgence().getId().equals(agence.getId())) {
            throw new BusinessException(
                    "Le collecteur ne peut pas être assigné à un client d'une agence différente",
                    "AGENCE_MISMATCH",
                    String.format("Collecteur agence: %d, Client agence: %d",
                            collecteur.getAgence().getId(), agence.getId())
            );
        }

        try {
            Client savedClient = clientRepository.save(client);
            log.info("Client sauvegardé avec succès, ID: {}", savedClient.getId());
            return savedClient;
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du client: {}", e.getMessage(), e);
            throw new BusinessException("Impossible de sauvegarder le client", "CLIENT_SAVE_ERROR",
                    "Une erreur est survenue lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Méthode privée pour valider un client avant sauvegarde
     * Cette méthode effectue toutes les validations métier nécessaires
     */
    private void validateClientBeforeSave(Client client) {
        // Vérifier les champs obligatoires
        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            throw new BusinessException("Le nom est obligatoire", "MISSING_FIELD", "nom");
        }

        if (client.getPrenom() == null || client.getPrenom().trim().isEmpty()) {
            throw new BusinessException("Le prénom est obligatoire", "MISSING_FIELD", "prenom");
        }

        if (client.getNumeroCni() == null || client.getNumeroCni().trim().isEmpty()) {
            throw new BusinessException("Le numéro CNI est obligatoire", "MISSING_FIELD", "numeroCni");
        }

        // Vérifier que le numéro CNI est unique pour un nouveau client
        if (client.getId() == null) {
            Optional<Client> existingClient = clientRepository.findByNumeroCni(client.getNumeroCni());
            if (existingClient.isPresent()) {
                throw new DuplicateResourceException("Un client avec le numéro CNI " + client.getNumeroCni() + " existe déjà");
            }
        } else {
            // Pour une mise à jour, vérifier que le CNI n'est pas déjà utilisé par un autre client
            clientRepository.findByNumeroCni(client.getNumeroCni())
                    .ifPresent(existingClient -> {
                        if (!existingClient.getId().equals(client.getId())) {
                            throw new DuplicateResourceException("Un client avec le numéro CNI " + client.getNumeroCni() + " existe déjà");
                        }
                    });
        }
    }

    @Override
    @CacheEvict(value = "clients", key = "#id")
    @Transactional
    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client", "id", id);
        }

        try {
            clientRepository.deleteById(id);
            log.info("Client supprimé avec succès, ID: {}", id);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du client: {}", e.getMessage(), e);
            throw new BusinessException("Impossible de supprimer le client", "CLIENT_DELETE_ERROR",
                    "Une erreur est survenue lors de la suppression: " + e.getMessage());
        }
    }

    @Override
    public List<Client> findByAgenceId(Long agenceId) {
        if (!agenceRepository.existsById(agenceId)) {
            throw new ResourceNotFoundException("Agence", "id", agenceId);
        }
        return clientRepository.findByAgenceId(agenceId);
    }

    @Override
    public Page<Client> findByAgenceId(Long agenceId, Pageable pageable) {
        if (!agenceRepository.existsById(agenceId)) {
            throw new ResourceNotFoundException("Agence", "id", agenceId);
        }
        return clientRepository.findByAgenceId(agenceId, pageable);
    }

    @Override
    public List<Client> findByCollecteurId(Long collecteurId) {
        if (!collecteurRepository.existsById(collecteurId)) {
            throw new ResourceNotFoundException("Collecteur", "id", collecteurId);
        }
        return clientRepository.findByCollecteurId(collecteurId);
    }

    @Override
    public Page<Client> findByCollecteurId(Long collecteurId, Pageable pageable) {
        if (!collecteurRepository.existsById(collecteurId)) {
            throw new ResourceNotFoundException("Collecteur", "id", collecteurId);
        }
        return clientRepository.findByCollecteurId(collecteurId, pageable);
    }

    @Override
    @Transactional
    public Client updateClient(Client client) {
        if (client.getId() == null) {
            throw new BusinessException("L'ID du client ne peut pas être null pour une mise à jour",
                    "MISSING_CLIENT_ID", "Veuillez spécifier l'ID du client à mettre à jour");
        }

        // Vérifier si le client existe
        if (!clientRepository.existsById(client.getId())) {
            throw new ResourceNotFoundException("Client", "id", client.getId());
        }

        return saveClient(client);
    }
}