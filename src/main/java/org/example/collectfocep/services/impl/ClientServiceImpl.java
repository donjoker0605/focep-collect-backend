package org.example.collectfocep.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.Validation.ClientValidator;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteClient;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.DuplicateResourceException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedAccessException;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.CompteClientRepository;
import org.example.collectfocep.services.interfaces.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final CollecteurRepository collecteurRepository;
    private final AgenceRepository agenceRepository;
    private final CompteClientRepository compteClientRepository;

    @Autowired
    private ClientValidator clientValidator;

    @Autowired
    public ClientServiceImpl(
            ClientRepository clientRepository,
            CollecteurRepository collecteurRepository,
            AgenceRepository agenceRepository,
            CompteClientRepository compteClientRepository) { // 🔥 INJECTION COMPTE REPOSITORY
        this.clientRepository = clientRepository;
        this.collecteurRepository = collecteurRepository;
        this.agenceRepository = agenceRepository;
        this.compteClientRepository = compteClientRepository;
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

        // Validation et traitement de la géolocalisation
        validateAndProcessGeolocation(client);

        // Génération du numéro de compte si nouveau client
        if (client.getId() == null && (client.getNumeroCompte() == null || client.getNumeroCompte().trim().isEmpty())) {
            client.setNumeroCompte(generateAccountNumber(agence.getId()));
        }

        try {
            // SAUVEGARDE DU CLIENT
            Client savedClient = clientRepository.save(client);
            log.info("Client sauvegardé avec succès, ID: {}", savedClient.getId());

            // CRÉATION AUTOMATIQUE DU COMPTE CLIENT
            if (savedClient.getId() != null) {
                createClientAccountIfNotExists(savedClient, agence);
            }

            // Log des informations de géolocalisation
            if (savedClient.hasLocation()) {
                log.info("📍 Client sauvegardé avec localisation: {}", savedClient.getLocationSummary());
            }

            return savedClient;
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du client: {}", e.getMessage(), e);
            throw new BusinessException("Impossible de sauvegarder le client", "CLIENT_SAVE_ERROR",
                    "Une erreur est survenue lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Création automatique du compte client
     */
    private void createClientAccountIfNotExists(Client client, Agence agence) {
        log.info("🏦 Vérification/création du compte pour le client: {} {}", client.getPrenom(), client.getNom());

        // Vérifier si le client a déjà un compte
        Optional<CompteClient> existingAccount = compteClientRepository.findByClient(client);

        if (existingAccount.isPresent()) {
            log.info("✅ Compte existant trouvé pour le client ID: {}", client.getId());
            return;
        }

        // Créer le compte client
        try {
            CompteClient compteClient = CompteClient.builder()
                    .client(client)
                    .nomCompte("Compte Épargne - " + client.getPrenom() + " " + client.getNom())
                    .numeroCompte(generateClientAccountNumber(client, agence))
                    .solde(0.0)
                    .typeCompte("EPARGNE")
                    .version(0L)
                    .build();

            CompteClient savedCompte = compteClientRepository.save(compteClient);
            log.info("✅ Compte client créé avec succès - ID: {}, Numéro: {}",
                    savedCompte.getId(), savedCompte.getNumeroCompte());

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du compte client: {}", e.getMessage(), e);
            // Ne pas faire échouer la création du client pour autant
            log.warn("⚠️ Client créé mais sans compte - À corriger manuellement");
        }
    }

    /**
     * Génération numéro de compte client
     */
    private String generateClientAccountNumber(Client client, Agence agence) {
        String codeAgence = agence.getCodeAgence() != null ? agence.getCodeAgence() : "DEF";
        String clientIdFormatted = String.format("%08d", client.getId());
        return "372" + codeAgence + clientIdFormatted;
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

        // Validation du téléphone camerounais
        if (client.getTelephone() != null && !client.getTelephone().trim().isEmpty()) {
            if (!isValidCameroonianPhone(client.getTelephone())) {
                throw new BusinessException("Format de téléphone camerounais invalide", "INVALID_PHONE_FORMAT");
            }
        }
    }

    // Validation et traitement de la géolocalisation
    private void validateAndProcessGeolocation(Client client) {
        // Si des coordonnées sont fournies, les valider
        if (client.getLatitude() != null || client.getLongitude() != null) {
            if (client.getLatitude() == null || client.getLongitude() == null) {
                throw new BusinessException("Latitude et longitude doivent être fournies ensemble", "INCOMPLETE_COORDINATES");
            }

            double lat = client.getLatitude().doubleValue();
            double lng = client.getLongitude().doubleValue();

            // Validation des coordonnées
            if (lat < -90 || lat > 90) {
                throw new BusinessException("Latitude invalide (doit être entre -90 et 90)", "INVALID_LATITUDE");
            }
            if (lng < -180 || lng > 180) {
                throw new BusinessException("Longitude invalide (doit être entre -180 et 180)", "INVALID_LONGITUDE");
            }

            // Validation spécifique au Cameroun (avec tolérance)
            if (lat < -1.5 || lat > 15.0 || lng < 6.0 || lng > 18.5) {
                log.warn("⚠️ Coordonnées {} {} semblent être en dehors du Cameroun", lat, lng);
            }

            // Coordonnées nulles exactes (0,0) non autorisées
            if (Math.abs(lat) < 0.001 && Math.abs(lng) < 0.001) {
                throw new BusinessException("Coordonnées (0,0) non autorisées", "NULL_ISLAND_COORDINATES");
            }

            // Mettre à jour la date de modification des coordonnées
            client.setDateMajCoordonnees(LocalDateTime.now());

            // Valeurs par défaut si non définies
            if (client.getCoordonneesSaisieManuelle() == null) {
                client.setCoordonneesSaisieManuelle(false);
            }

            log.info("📍 Coordonnées validées: lat={}, lng={}, manuel={}",
                    lat, lng, client.getCoordonneesSaisieManuelle());
        }
    }

    // Validation du format de téléphone camerounais
    private boolean isValidCameroonianPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // Format: +237XXXXXXXXX ou 237XXXXXXXXX ou 6XXXXXXXX ou 7XXXXXXXX ou 9XXXXXXXX
        String cleanPhone = phone.replaceAll("\\s+", "");
        return cleanPhone.matches("^(\\+237|237)?[679]\\d{8}$");
    }

    // Génération d'un numéro de compte unique
    private String generateAccountNumber(Long agenceId) {
        String prefix = "CLI-" + agenceId + "-";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + timestamp;
    }

    @Override
    @CacheEvict(value = "clients", key = "#id")
    @Transactional
    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client", "id", id);
        }

        try {
            // Soft delete au lieu de hard delete
            Client client = clientRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));

            client.setValide(false);
            clientRepository.save(client);

            log.info("Client marqué comme supprimé (soft delete), ID: {}", id);
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
        // Utiliser la nouvelle méthode qui récupère les paramètres de commission
        return clientRepository.findByCollecteurIdWithCommissionParams(collecteurId);
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

        // 🔥 MODIFICATION : Pour les mises à jour, préserver certains champs
        Client existingClient = clientRepository.findById(client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", client.getId()));

        // Préserver les champs qui ne doivent pas être modifiés par le collecteur
        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            client.setNom(existingClient.getNom());
        }
        if (client.getPrenom() == null || client.getPrenom().trim().isEmpty()) {
            client.setPrenom(existingClient.getPrenom());
        }

        // Préserver les relations si non définies
        if (client.getCollecteur() == null) {
            client.setCollecteur(existingClient.getCollecteur());
        }
        if (client.getAgence() == null) {
            client.setAgence(existingClient.getAgence());
        }

        // Préserver les dates de création
        client.setDateCreation(existingClient.getDateCreation());
        client.setNumeroCompte(existingClient.getNumeroCompte());

        return saveClient(client);
    }

    // Méthode pour mettre à jour seulement la localisation
    @Transactional
    public Client updateClientLocation(Long clientId, BigDecimal latitude, BigDecimal longitude,
                                       Boolean saisieManuelle, String adresseComplete) {
        log.info("📍 Mise à jour localisation client: {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        // Utiliser la méthode utilitaire de l'entité Client
        client.updateLocation(latitude, longitude, saisieManuelle, adresseComplete);

        Client savedClient = clientRepository.save(client);
        log.info("✅ Localisation mise à jour: {}", savedClient.getLocationSummary());

        return savedClient;
    }

    // Méthode pour obtenir les clients avec localisation
    public List<Client> getClientsWithLocation() {
        return clientRepository.findAll().stream()
                .filter(Client::hasLocation)
                .toList();
    }

    // Méthode pour obtenir les clients sans localisation
    public List<Client> getClientsWithoutLocation() {
        return clientRepository.findAll().stream()
                .filter(client -> !client.hasLocation())
                .toList();
    }

    // Statistiques de géolocalisation
    public LocationStatistics getLocationStatistics() {
        List<Client> allClients = clientRepository.findAll();

        long totalClients = allClients.size();
        long clientsWithLocation = allClients.stream()
                .mapToLong(client -> client.hasLocation() ? 1 : 0)
                .sum();
        long manualEntries = allClients.stream()
                .mapToLong(client -> client.isManualLocation() ? 1 : 0)
                .sum();

        return new LocationStatistics(
                totalClients,
                clientsWithLocation,
                totalClients - clientsWithLocation,
                manualEntries,
                clientsWithLocation - manualEntries,
                totalClients > 0 ? (double) clientsWithLocation / totalClients * 100 : 0
        );
    }

    // Classe interne pour les statistiques
    public static class LocationStatistics {
        private final long totalClients;
        private final long clientsWithLocation;
        private final long clientsWithoutLocation;
        private final long manualEntries;
        private final long gpsEntries;
        private final double coveragePercentage;

        public LocationStatistics(long totalClients, long clientsWithLocation, long clientsWithoutLocation,
                                  long manualEntries, long gpsEntries, double coveragePercentage) {
            this.totalClients = totalClients;
            this.clientsWithLocation = clientsWithLocation;
            this.clientsWithoutLocation = clientsWithoutLocation;
            this.manualEntries = manualEntries;
            this.gpsEntries = gpsEntries;
            this.coveragePercentage = coveragePercentage;
        }

        // Getters
        public long getTotalClients() { return totalClients; }
        public long getClientsWithLocation() { return clientsWithLocation; }
        public long getClientsWithoutLocation() { return clientsWithoutLocation; }
        public long getManualEntries() { return manualEntries; }
        public long getGpsEntries() { return gpsEntries; }
        public double getCoveragePercentage() { return coveragePercentage; }
    }
}