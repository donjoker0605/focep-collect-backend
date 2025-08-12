package org.example.collectfocep.services;

import org.example.collectfocep.dto.ClientSummaryDTO;
import org.example.collectfocep.dto.MouvementDTO;
import org.example.collectfocep.entities.Client;

import java.util.List;

/**
 * Service pour les calculs de statistiques client
 * Utilisé pour enrichir les données client avec totaux et historique
 */
public interface ClientStatsService {
    
    /**
     * Enrichit un client avec ses statistiques complètes
     */
    ClientSummaryDTO enrichClientWithStats(Client client);
    
    /**
     * Récupère les transactions récentes d'un client
     */
    List<MouvementDTO> getRecentTransactions(Long clientId, int limit);
    
    /**
     * Calcule le total des épargnes d'un client
     */
    Double getTotalEpargne(Long clientId);
    
    /**
     * Calcule le total des retraits d'un client
     */
    Double getTotalRetraits(Long clientId);
    
    /**
     * Récupère les paramètres de commission d'un client (si définis)
     */
    ClientSummaryDTO.CommissionParameterDTO getCommissionParameters(Long clientId);
    
    /**
     * Enrichit une liste de clients avec leurs stats (optimisé)
     */
    List<ClientSummaryDTO> enrichMultipleClientsWithStats(List<Client> clients);
}