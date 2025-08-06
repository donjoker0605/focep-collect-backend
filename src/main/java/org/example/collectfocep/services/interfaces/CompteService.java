package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface CompteService {

    // =====================================
    // MÉTHODES EXISTANTES CONSERVÉES
    // =====================================

    void createCollecteurAccounts(Collecteur collecteur);
    CompteCollecteur findServiceAccount(Collecteur collecteur);
    CompteCollecteur findWaitingAccount(Collecteur collecteur);
    CompteCollecteur findSalaryAccount(Collecteur collecteur);
    CompteLiaison findLiaisonAccount(Agence agence);
    Compte findProduitAccount();
    Compte findTVAAccount();
    Compte findChargeAccount(Collecteur collecteur);
    Optional<Compte> findByTypeCompte(String typeCompte);
    double getSolde(Long compteId);
    List<Compte> findByAgenceId(Long agenceId);
    List<Compte> findByCollecteurId(Long collecteurId);
    Page<Compte> findByCollecteurId(Long collecteurId, Pageable pageable);
    Page<Compte> findByAgenceId(Long agenceId, Pageable pageable);
    Page<Compte> getAllComptes(Pageable pageable);
    List<Compte> getAllComptes();
    void deleteCompte(Long id);
    Compte saveCompte(Compte compte);
    Optional<Compte> getCompteById(Long id);
    List<Compte> findByClientId(Long clientId);

    // =====================================
    // NOUVELLES MÉTHODES POUR VERSEMENTS
    // =====================================

    /**
     * Trouve le compte manquant d'un collecteur
     */
    CompteManquant findManquantAccount(Collecteur collecteur);

    /**
     * Trouve le compte attente d'un collecteur
     */
    CompteAttente findAttenteAccount(Collecteur collecteur);

    /**
     * Trouve le compte salaire d'un collecteur
     */
    CompteSalaireCollecteur findSalaireAccount(Collecteur collecteur);

    /**
     * Met à jour le solde d'un compte
     */
    void updateSoldeCompte(Long compteId, Double nouveauSolde);

    /**
     * Ajoute un montant au solde d'un compte
     */
    void ajouterAuSolde(Long compteId, Double montant);

    /**
     * Retire un montant du solde d'un compte
     */
    void retirerDuSolde(Long compteId, Double montant);

    /**
     * Transfère un montant entre deux comptes
     */
    void transfererEntreComptes(Long compteSourceId, Long compteDestinationId, Double montant, String motif);

    /**
     * Remet à zéro le solde d'un compte
     */
    void remettreAZero(Long compteId);

    /**
     * Vérifie si un compte existe pour un collecteur
     */
    boolean hasCompteService(Collecteur collecteur);
    boolean hasCompteManquant(Collecteur collecteur);
    boolean hasCompteAttente(Collecteur collecteur);
    boolean hasCompteRemuneration(Collecteur collecteur);

    /**
     * Récupère tous les comptes d'un collecteur avec leurs soldes
     */
    List<Compte> getAllComptesByCollecteur(Collecteur collecteur);

    /**
     * Calcule le solde total d'un type de compte pour tous les collecteurs d'une agence
     */
    Double calculateTotalSoldeByTypeAndAgence(String typeCompte, Long agenceId);

    /**
     * Récupère les collecteurs ayant un solde non nul dans un type de compte
     */
    List<Collecteur> getCollecteursWithNonZeroBalance(String typeCompte, Long agenceId);

    /**
     * Archive les anciens mouvements de compte (pour performance)
     */
    void archiverAnciensMouvements(int nombreJours);
}


