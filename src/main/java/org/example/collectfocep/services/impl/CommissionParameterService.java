package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CommissionParameter;
import org.example.collectfocep.entities.CommissionType;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.CommissionParameterMapper;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.CommissionParameterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CommissionParameterService {

    private final CommissionParameterRepository repository;
    private final CommissionParameterMapper mapper;
    private final ClientRepository clientRepository;
    private final CollecteurRepository collecteurRepository;
    private final AgenceRepository agenceRepository;

    /**
     * Créer un nouveau paramètre de commission
     */
    public CommissionParameter createCommissionParameter(CommissionParameterDTO dto) {
        log.info("Création paramètre commission: type={}, clientId={}", dto.getType(), dto.getClientId());

        // Vérifier que le type existe avant de comparer
        if (dto.getType() != null && CommissionType.TIER.equals(dto.getType())) {
            if (!mapper.validateTiers(dto.getPaliersCommission())) {
                throw new IllegalArgumentException("Paliers de commission invalides");
            }
        }

        CommissionParameter entity = mapper.toEntity(dto);

        // Gérer les relations manuellement car ignorées dans le mapper
        if (dto.getClientId() != null) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", dto.getClientId()));
            entity.setClient(client);
        }

        if (dto.getCollecteurId() != null) {
            Collecteur collecteur = collecteurRepository.findById(dto.getCollecteurId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur", "id", dto.getCollecteurId()));
            entity.setCollecteur(collecteur);
        }

        if (dto.getAgenceId() != null) {
            Agence agence = agenceRepository.findById(dto.getAgenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agence", "id", dto.getAgenceId()));
            entity.setAgence(agence);
        }

        CommissionParameter saved = repository.save(entity);
        log.info("Paramètre commission créé: id={}", saved.getId());
        return saved;
    }

    /**
     * Mettre à jour un paramètre existant (avec historique)
     */
    public CommissionParameter updateCommissionParameter(Long id, CommissionParameterDTO dto) {
        log.info("Mise à jour paramètre commission: id={}", id);

        CommissionParameter existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommissionParameter", "id", id));

        // Créer un historique en désactivant l'ancien
        existing.setActive(false);
        existing.setValidTo(LocalDate.now().minusDays(1));
        repository.save(existing);

        // Créer le nouveau paramètre
        dto.setId(null); // Force la création d'un nouveau
        dto.setValidFrom(LocalDate.now());
        dto.setActive(true);

        return createCommissionParameter(dto);
    }

    /**
     * Récupérer le paramètre actif selon la hiérarchie
     */
    public Optional<CommissionParameter> getEffectiveCommissionParameter(Long clientId) {
        log.debug("Recherche paramètre effectif pour client: {}", clientId);

        // 1. Niveau client
        Optional<CommissionParameter> clientParam = repository.findActiveCommissionParameter(clientId);
        if (clientParam.isPresent()) {
            log.debug("Paramètre trouvé au niveau client");
            return clientParam;
        }

        // 2. Niveau collecteur
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        Optional<CommissionParameter> collecteurParam = repository
                .findActiveCommissionParameterByCollecteur(client.getCollecteur().getId());
        if (collecteurParam.isPresent()) {
            log.debug("Paramètre trouvé au niveau collecteur");
            return collecteurParam;
        }

        // 3. Niveau agence
        Optional<CommissionParameter> agenceParam = repository
                .findActiveCommissionParameterByAgence(client.getAgence().getId());
        if (agenceParam.isPresent()) {
            log.debug("Paramètre trouvé au niveau agence");
            return agenceParam;
        }

        log.warn("Aucun paramètre de commission trouvé pour client: {}", clientId);
        return Optional.empty();
    }

    /**
     * Récupérer l'historique des paramètres d'un client
     */
    public List<CommissionParameter> getCommissionHistory(Long clientId) {
        return repository.findAllByClientId(clientId);
    }

    /**
     * Désactiver un paramètre de commission
     */
    public void deactivateCommissionParameter(Long id) {
        log.info("Désactivation paramètre commission: id={}", id);

        CommissionParameter param = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommissionParameter", "id", id));

        param.setActive(false);
        param.setValidTo(LocalDate.now());
        repository.save(param);
    }

    /**
     * Créer paramètres par défaut pour une nouvelle agence
     */
    public CommissionParameter createDefaultAgenceCommission(Long agenceId) {
        log.info("Création commission par défaut pour agence: {}", agenceId);

        Agence agence = agenceRepository.findById(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Agence", "id", agenceId));

        CommissionParameter defaultParam = CommissionParameter.builder()
                .type(CommissionType.PERCENTAGE)
                .valeur(BigDecimal.valueOf(5.0)) // ✅ CORRECTION : BigDecimal au lieu de double
                .codeProduit("DEFAULT_AGENCE")
                .validFrom(LocalDate.now())
                .active(true)
                .agence(agence)
                .build();

        return repository.save(defaultParam);
    }

    /**
     * Créer paramètres par défaut pour un nouveau collecteur
     */
    public CommissionParameter createDefaultCollecteurCommission(Long collecteurId) {
        log.info("Création commission par défaut pour collecteur: {}", collecteurId);

        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur", "id", collecteurId));

        CommissionParameter defaultParam = CommissionParameter.builder()
                .type(CommissionType.PERCENTAGE)
                .valeur(BigDecimal.valueOf(5.0)) // ✅ CORRECTION : BigDecimal au lieu de double
                .codeProduit("DEFAULT_COLLECTEUR")
                .validFrom(LocalDate.now())
                .active(true)
                .collecteur(collecteur)
                .build();

        return repository.save(defaultParam);
    }

    /**
     * Créer paramètre avec BigDecimal directement
     */
    public CommissionParameter createParameterWithBigDecimal(
            CommissionType type,
            BigDecimal valeur,
            Long clientId,
            Long collecteurId,
            Long agenceId) {

        CommissionParameter.CommissionParameterBuilder builder = CommissionParameter.builder()
                .type(type)
                .valeur(valeur)
                .validFrom(LocalDate.now())
                .active(true);

        // Associer l'entité appropriée
        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));
            builder.client(client);
        } else if (collecteurId != null) {
            Collecteur collecteur = collecteurRepository.findById(collecteurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Collecteur", "id", collecteurId));
            builder.collecteur(collecteur);
        } else if (agenceId != null) {
            Agence agence = agenceRepository.findById(agenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agence", "id", agenceId));
            builder.agence(agence);
        }

        return repository.save(builder.build());
    }

    /**
     * Obtenir tous paramètres actifs d'une agence
     */
    public List<CommissionParameter> getActiveParametersByAgence(Long agenceId) {
        return repository.findActiveByAgenceId(agenceId);
    }

    /**
     * Obtenir tous paramètres actifs d'un collecteur
     */
    public List<CommissionParameter> getActiveParametersByCollecteur(Long collecteurId) {
        return repository.findActiveByCollecteurId(collecteurId);
    }
}