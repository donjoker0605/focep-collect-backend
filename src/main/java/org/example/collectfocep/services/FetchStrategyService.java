package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.repositories.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service centralisé pour gérer les stratégies de fetch Hibernate
 * Évite la duplication de code et standardise les patterns de loading
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FetchStrategyService {
    
    private final ClientRepository clientRepository;
    
    /**
     * Récupère les clients d'une agence avec leurs collecteurs (pour affichage liste admin)
     * Performance: 1 requête avec @EntityGraph + LEFT JOIN FETCH
     */
    public Page<Client> getClientsWithCollecteur(Long agenceId, Pageable pageable) {
        log.debug("🎯 Fetch clients agence {} avec collecteurs (EntityGraph)", agenceId);
        return clientRepository.findByAgenceIdWithCollecteurOrderByIdDesc(agenceId, pageable);
    }
    
    /**
     * Récupère les clients avec collecteurs ET comptes (pour calculs complexes)  
     * Performance: 1 requête avec plusieurs @EntityGraph + LEFT JOIN FETCH
     */
    public List<Client> getClientsWithFullData(Long agenceId) {
        log.debug("🎯 Fetch clients agence {} avec données complètes (EntityGraph)", agenceId);
        return clientRepository.findByAgenceIdWithFullDataOrderByIdDesc(agenceId);
    }
    
    /**
     * Récupère des clients spécifiques avec collecteurs (pour validation transfert)
     * Performance: 1 requête IN + @EntityGraph + LEFT JOIN FETCH
     */
    public List<Client> getClientsWithCollecteurByIds(List<Long> clientIds) {
        log.debug("🎯 Fetch {} clients avec collecteurs (EntityGraph)", clientIds.size());
        return clientRepository.findByIdInWithCollecteurOrderById(clientIds);
    }
    
    /**
     * Récupère un client avec toutes ses données (pour détail/édition)
     * Performance: 1 requête avec tous les @EntityGraph + LEFT JOIN FETCH
     */
    public Optional<Client> getClientFullData(Long clientId) {
        log.debug("🎯 Fetch client {} avec données complètes (EntityGraph)", clientId);
        return clientRepository.findByIdWithFullData(clientId);
    }
}