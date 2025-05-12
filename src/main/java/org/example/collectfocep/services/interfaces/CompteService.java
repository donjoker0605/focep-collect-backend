package org.example.collectfocep.services.interfaces;

import org.example.collectfocep.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CompteService {
    // Méthodes pour retrouver les différents types de comptes
    CompteCollecteur findServiceAccount(Collecteur collecteur);
    CompteCollecteur findWaitingAccount(Collecteur collecteur);
    CompteCollecteur findSalaryAccount(Collecteur collecteur);
    CompteLiaison findLiaisonAccount(Agence agence);
    Compte findProduitAccount();
    Compte findTVAAccount();
    Compte findChargeAccount(Collecteur collecteur);

    // Méthodes standard pour la gestion des comptes
    List<Compte> getAllComptes();
    Page<Compte> getAllComptes(Pageable pageable);
    Optional<Compte> getCompteById(Long id);
    Compte saveCompte(Compte compte);
    void deleteCompte(Long id);

    // Méthodes pour les opérations comptables
    double getSolde(Long compteId);

    // Méthodes pour rechercher des comptes par critères
    Optional<Compte> findByTypeCompte(String typeCompte);
    void createCollecteurAccounts(Collecteur collecteur);
    List<Compte> findByAgenceId(Long agenceId);
    Page<Compte> findByAgenceId(Long agenceId, Pageable pageable);
    List<Compte> findByCollecteurId(Long collecteurId);
    Page<Compte> findByCollecteurId(Long collecteurId, Pageable pageable);
    List<Compte> findByClientId(Long clientId);
}