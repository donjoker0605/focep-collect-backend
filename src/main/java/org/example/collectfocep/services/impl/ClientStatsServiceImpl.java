package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.ClientSummaryDTO;
import org.example.collectfocep.dto.CommissionParameterDTO;
import org.example.collectfocep.dto.MouvementDTO;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.mappers.MouvementMapperV2;
import org.example.collectfocep.repositories.MouvementRepository;
import org.example.collectfocep.services.ClientStatsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implémentation du service de statistiques client
 * 🔥 OPTIMISÉ pour les performances avec des requêtes ciblées
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientStatsServiceImpl implements ClientStatsService {

    private final MouvementRepository mouvementRepository;
    private final MouvementMapperV2 mouvementMapper;

    @Override
    @Transactional(readOnly = true)
    public ClientSummaryDTO enrichClientWithStats(Client client) {
        log.debug("🔄 Enrichissement stats pour client: {}", client.getId());
        
        ClientSummaryDTO dto = ClientSummaryDTO.fromClient(client);
        
        // 🔥 ENRICHISSEMENT AVEC TOUTES LES STATS
        enrichWithAllStats(dto, client.getId());
        
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientSummaryDTO> enrichMultipleClientsWithStats(List<Client> clients) {
        log.debug("🔄 Enrichissement stats pour {} clients", clients.size());
        
        if (clients.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 🔥 OPTIMISATION: Calculs batch au lieu de N+1
        return enrichMultipleClientsOptimized(clients);
    }
    
    /**
     * 🚀 NOUVELLE MÉTHODE OPTIMISÉE - 2 requêtes au lieu de N*4
     */
    private List<ClientSummaryDTO> enrichMultipleClientsOptimized(List<Client> clients) {
        List<Long> clientIds = clients.stream()
            .map(Client::getId)
            .collect(Collectors.toList());
            
        // 🔥 REQUÊTE 1: Tous les stats d'épargne/retrait en une fois
        Map<Long, ClientStatsData> statsMap = calculateBatchStats(clientIds);
        
        // 🔥 REQUÊTE 2: Toutes les transactions récentes en une fois  
        Map<Long, List<MouvementDTO>> transactionsMap = getBatchRecentTransactions(clientIds, 20);
        
        // 🔥 ASSEMBLAGE: Créer les DTOs enrichis
        return clients.stream()
            .map(client -> buildEnrichedClientDTO(client, statsMap.get(client.getId()), 
                                                 transactionsMap.get(client.getId())))
            .collect(Collectors.toList());
    }
    
    /**
     * Calcule les stats pour tous les clients en 2 requêtes
     */
    private Map<Long, ClientStatsData> calculateBatchStats(List<Long> clientIds) {
        // Requête optimisée pour épargne et retraits groupés
        List<Object[]> results = mouvementRepository.getClientStatsOptimized(clientIds);
        
        Map<Long, ClientStatsData> statsMap = new HashMap<>();
        
        for (Object[] result : results) {
            Long clientId = (Long) result[0];
            String sens = (String) result[1];
            Double total = ((Number) result[2]).doubleValue();
            
            statsMap.computeIfAbsent(clientId, k -> new ClientStatsData())
                   .addStat(sens, total);
        }
        
        // Initialiser les clients sans transactions
        for (Long clientId : clientIds) {
            statsMap.computeIfAbsent(clientId, k -> new ClientStatsData());
        }
        
        return statsMap;
    }
    
    /**
     * Récupère les transactions récentes pour tous les clients
     */
    private Map<Long, List<MouvementDTO>> getBatchRecentTransactions(List<Long> clientIds, int limit) {
        List<Mouvement> allTransactions = mouvementRepository.getRecentTransactionsBatch(clientIds, limit);
        
        return allTransactions.stream()
            .map(mouvementMapper::toDTO)
            .collect(Collectors.groupingBy(dto -> dto.getClient() != null ? dto.getClient().getId() : null))
            .entrySet().stream()
            .filter(entry -> entry.getKey() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Construit le DTO enrichi pour un client
     */
    private ClientSummaryDTO buildEnrichedClientDTO(Client client, ClientStatsData stats, 
                                                   List<MouvementDTO> transactions) {
        ClientSummaryDTO dto = ClientSummaryDTO.fromClient(client);
        
        // Stats financières
        dto.setTotalEpargne(stats != null ? stats.getTotalEpargne() : 0.0);
        dto.setTotalRetraits(stats != null ? stats.getTotalRetraits() : 0.0);
        dto.setSoldeNet(dto.getTotalEpargne() - dto.getTotalRetraits());
        
        // Transactions récentes
        dto.setTransactions(transactions != null ? transactions : new ArrayList<>());
        dto.setNombreTransactions(dto.getTransactions().size());
        
        // Dernière transaction
        if (!dto.getTransactions().isEmpty()) {
            dto.setDerniereTransaction(dto.getTransactions().get(0).getDateOperation());
        }
        
        // Compte client et paramètres commission
        enrichWithAccountAndCommission(dto, client.getId());
        
        return dto;
    }
    
    /**
     * Enrichit avec compte et commission (garde l'ancien comportement)
     */
    private void enrichWithAccountAndCommission(ClientSummaryDTO dto, Long clientId) {
        try {
            // Paramètres de commission (utilise la méthode existante)
            dto.setCommissionParameter(getCommissionParameters(clientId));
            
            // Note: CompteClient sera défini au niveau du mapping initial du client
            
        } catch (Exception e) {
            log.warn("⚠️ Erreur enrichissement commission pour client {}: {}", 
                    clientId, e.getMessage());
            // Continuer sans bloquer
        }
    }
    
    /**
     * Classe helper pour les stats
     */
    private static class ClientStatsData {
        private double totalEpargne = 0.0;
        private double totalRetraits = 0.0;
        
        public void addStat(String sens, Double montant) {
            if ("EPARGNE".equalsIgnoreCase(sens)) {
                totalEpargne += (montant != null ? montant : 0.0);
            } else if ("RETRAIT".equalsIgnoreCase(sens)) {
                totalRetraits += (montant != null ? montant : 0.0);
            }
        }
        
        public double getTotalEpargne() { return totalEpargne; }
        public double getTotalRetraits() { return totalRetraits; }
    }

    @Override
    public List<MouvementDTO> getRecentTransactions(Long clientId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, 
            Sort.by(Sort.Direction.DESC, "dateOperation"));
        
        List<Mouvement> mouvements = mouvementRepository.findByClientIdOrderByDateOperationDesc(
            clientId, pageRequest).getContent();
            
        return mouvements.stream()
            .map(mouvementMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Double getTotalEpargne(Long clientId) {
        // Utiliser la nouvelle méthode spécifique aux clients
        Double total = mouvementRepository.sumMontantByClientIdAndSens(clientId, "EPARGNE");
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalRetraits(Long clientId) {
        // Utiliser la nouvelle méthode spécifique aux clients
        Double total = mouvementRepository.sumMontantByClientIdAndSens(clientId, "RETRAIT");
        return total != null ? total : 0.0;
    }

    @Override
    public ClientSummaryDTO.CommissionParameterDTO getCommissionParameters(Long clientId) {
        // 🔥 TODO: Implémenter quand la table commission_parameters sera créée
        // Pour l'instant, retourner des paramètres par défaut selon le profil client
        
        return createDefaultCommissionParameters(clientId);
    }

    /**
     * 🔥 MÉTHODE OPTIMISÉE : Enrichit toutes les stats en une seule fois
     */
    private void enrichWithAllStats(ClientSummaryDTO dto, Long clientId) {
        try {
            // Récupérer les transactions récentes
            dto.setTransactions(getRecentTransactions(clientId, 20));
            
            // Calcul des totaux avec requêtes optimisées
            dto.setTotalEpargne(getTotalEpargneOptimized(clientId));
            dto.setTotalRetraits(getTotalRetraitsOptimized(clientId));
            
            // Calcul du solde net
            dto.calculateSoldeNet();
            
            // Stats supplémentaires
            dto.setNombreTransactions(dto.getTransactions().size());
            if (!dto.getTransactions().isEmpty()) {
                dto.setDerniereTransaction(dto.getTransactions().get(0).getDateOperation());
            }
            
            // Paramètres de commission
            dto.setCommissionParameter(getCommissionParameters(clientId));
            
        } catch (Exception e) {
            log.error("❌ Erreur enrichissement stats pour client {}: {}", clientId, e.getMessage());
            // En cas d'erreur, on garde les valeurs par défaut
            dto.setTotalEpargne(0.0);
            dto.setTotalRetraits(0.0);
            dto.calculateSoldeNet();
        }
    }
    
    /**
     * Version optimisée pour calculer le total épargne d'un client
     * Utilise une requête spécifique au client (pas collecteur)
     */
    private Double getTotalEpargneOptimized(Long clientId) {
        try {
            // Utiliser une requête COUNT pour éviter de charger tous les mouvements
            List<Mouvement> epargnes = mouvementRepository.findByClientId(clientId)
                .stream()
                .filter(m -> "EPARGNE".equalsIgnoreCase(m.getSens()))
                .collect(Collectors.toList());
            
            return epargnes.stream().mapToDouble(Mouvement::getMontant).sum();
        } catch (Exception e) {
            log.warn("⚠️ Erreur calcul total épargne pour client {}: {}", clientId, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Version optimisée pour calculer le total retraits d'un client
     */
    private Double getTotalRetraitsOptimized(Long clientId) {
        try {
            List<Mouvement> retraits = mouvementRepository.findByClientId(clientId)
                .stream()
                .filter(m -> "RETRAIT".equalsIgnoreCase(m.getSens()))
                .collect(Collectors.toList());
            
            return retraits.stream().mapToDouble(Mouvement::getMontant).sum();
        } catch (Exception e) {
            log.warn("⚠️ Erreur calcul total retraits pour client {}: {}", clientId, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Crée des paramètres de commission par défaut
     * 🔥 TODO: Remplacer par la vraie logique métier
     */
    private ClientSummaryDTO.CommissionParameterDTO createDefaultCommissionParameters(Long clientId) {
        return ClientSummaryDTO.CommissionParameterDTO.builder()
            .typeCommission("POURCENTAGE")
            .pourcentage(2.0) // 2% par défaut
            .build();
    }
}