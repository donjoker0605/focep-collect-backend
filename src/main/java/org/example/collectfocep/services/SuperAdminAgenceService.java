package org.example.collectfocep.services;

import org.example.collectfocep.dto.AgenceDTO;
import org.example.collectfocep.dto.ParametreCommissionDTO;
import org.example.collectfocep.dto.SuperAdminDTO;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.dto.ClientDTO;
import org.example.collectfocep.dto.AgenceDetailDTO;
import org.example.collectfocep.dto.CreateAdminDTO;
import org.example.collectfocep.dto.CreateCollecteurDTO;
import org.example.collectfocep.dto.SuperAdminAdminDTO;
import org.example.collectfocep.dto.JournalDTO;
import org.example.collectfocep.dto.MouvementDTO;
import org.example.collectfocep.entities.Agence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 🏢 Service SuperAdmin pour la gestion complète des agences
 * 
 * Fonctionnalités:
 * - CRUD complet agences
 * - Gestion paramètres commission par agence
 * - Validation business anti-erreurs
 * - Statistiques et monitoring
 */
public interface SuperAdminAgenceService {

    // ================================
    // CRUD AGENCES
    // ================================
    
    /**
     * Récupère toutes les agences avec pagination
     */
    Page<AgenceDTO> getAllAgences(Pageable pageable);
    
    /**
     * Récupère toutes les agences (sans pagination)
     */
    List<AgenceDTO> getAllAgences();
    
    /**
     * Récupère une agence par ID avec tous ses détails
     */
    AgenceDTO getAgenceById(Long agenceId);
    
    /**
     * Crée une nouvelle agence avec validation
     */
    AgenceDTO createAgence(AgenceDTO agenceDTO);
    
    /**
     * Met à jour une agence existante
     */
    AgenceDTO updateAgence(Long agenceId, AgenceDTO agenceDTO);
    
    /**
     * Active/Désactive une agence
     */
    AgenceDTO toggleAgenceStatus(Long agenceId);
    
    /**
     * Supprime une agence (seulement si vide)
     */
    void deleteAgence(Long agenceId);

    // ================================
    // GESTION PARAMÈTRES COMMISSION
    // ================================
    
    /**
     * Récupère les paramètres de commission d'une agence
     */
    List<ParametreCommissionDTO> getAgenceCommissionParams(Long agenceId);
    
    /**
     * Définit les paramètres de commission pour une agence
     */
    List<ParametreCommissionDTO> setAgenceCommissionParams(Long agenceId, List<ParametreCommissionDTO> parametres);
    
    /**
     * Met à jour un paramètre de commission spécifique
     */
    ParametreCommissionDTO updateCommissionParam(Long agenceId, Long parametreId, ParametreCommissionDTO parametre);
    
    /**
     * Supprime un paramètre de commission
     */
    void deleteCommissionParam(Long agenceId, Long parametreId);

    // ================================
    // VALIDATION & BUSINESS RULES
    // ================================
    
    /**
     * Valide qu'une agence peut être créée
     */
    void validateAgenceCreation(AgenceDTO agenceDTO);
    
    /**
     * Valide qu'une agence peut être modifiée
     */
    void validateAgenceUpdate(Long agenceId, AgenceDTO agenceDTO);
    
    /**
     * Valide qu'une agence peut être supprimée
     */
    void validateAgenceDeletion(Long agenceId);
    
    /**
     * Génère un code agence unique
     */
    String generateUniqueAgenceCode(String nomAgence);

    // ================================
    // STATISTIQUES & MONITORING
    // ================================
    
    /**
     * Statistiques complètes d'une agence
     */
    AgenceDTO getAgenceWithStats(Long agenceId);
    
    /**
     * Liste des agences avec performances
     */
    List<AgenceDTO> getAgencesWithPerformance();
    
    /**
     * Agences inactives ou avec problèmes
     */
    List<AgenceDTO> getProblematicAgences();
    
    /**
     * Nombre total d'agences par statut
     */
    Long countAgencesByStatus(boolean active);
    
    // ================================
    // GESTION UTILISATEURS PAR AGENCE
    // ================================
    
    /**
     * Récupère tous les admins d'une agence
     */
    List<SuperAdminDTO> getAdminsByAgence(Long agenceId);
    
    /**
     * Récupère tous les collecteurs d'une agence
     */
    List<CollecteurDTO> getCollecteursByAgence(Long agenceId);
    
    /**
     * Récupère tous les clients d'une agence
     */
    List<ClientDTO> getClientsByAgence(Long agenceId);
    
    /**
     * Récupère les clients d'un collecteur spécifique
     */
    List<ClientDTO> getClientsByCollecteur(Long collecteurId);
    
    /**
     * Récupère les détails complets d'une agence avec tous ses utilisateurs
     */
    AgenceDetailDTO getAgenceDetailsComplete(Long agenceId);
    
    // ================================
    // GESTION COMPLÈTE DES ADMINS
    // ================================
    
    /**
     * Crée un nouvel administrateur
     */
    SuperAdminAdminDTO createAdmin(CreateAdminDTO createAdminDTO);
    
    /**
     * Modifie un administrateur
     */
    SuperAdminAdminDTO updateAdmin(Long adminId, CreateAdminDTO updateAdminDTO);
    
    // ================================
    // GESTION COMPLÈTE DES COLLECTEURS
    // ================================
    
    /**
     * Récupère tous les collecteurs
     */
    List<CollecteurDTO> getAllCollecteurs();
    
    /**
     * Récupère les détails d'un collecteur
     */
    CollecteurDTO getCollecteurDetails(Long collecteurId);
    
    /**
     * Crée un nouveau collecteur avec paramètres de commission
     */
    CollecteurDTO createCollecteur(CreateCollecteurDTO createCollecteurDTO);
    
    /**
     * Modifie un collecteur
     */
    CollecteurDTO updateCollecteur(Long collecteurId, CreateCollecteurDTO updateCollecteurDTO);
    
    /**
     * Active/désactive un collecteur
     */
    CollecteurDTO toggleCollecteurStatus(Long collecteurId);
    
    // ================================
    // JOURNAUX D'ACTIVITÉS
    // ================================
    
    /**
     * Récupère tous les journaux avec filtres optionnels
     */
    List<JournalDTO> getAllJournaux(int page, int size, Long agenceId, Long collecteurId);
    
    // ================================
    // GESTION COMPLÈTE DES CLIENTS
    // ================================
    
    /**
     * Récupère tous les clients avec filtres optionnels
     */
    List<ClientDTO> getAllClients(int page, int size, Long agenceId, Long collecteurId);

    /**
     * 💰 Récupère les détails complets d'un client (données financières, localisation, commission)
     */
    ClientDTO getClientDetailsComplete(Long clientId);

    /**
     * 💳 Récupère l'historique des transactions d'un client
     */
    List<MouvementDTO> getClientTransactions(Long clientId, int page, int size);
}