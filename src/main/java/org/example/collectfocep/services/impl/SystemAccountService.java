package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.CompteSysteme;
import org.example.collectfocep.repositories.CompteSystemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemAccountService {

    private final CompteSystemeRepository compteSystemeRepository;

    @Transactional
    public void initializeSystemAccounts() {
        log.info("Vérification et création des comptes système...");

        // Créer uniquement les comptes système nécessaires
        ensureSystemAccountExists("TAXE", "Compte Système TVA", "TAXE-SYS");
        ensureSystemAccountExists("EMF", "Compte Système EMF", "EMF-SYS");
        ensureSystemAccountExists("COMMISSION", "Compte Système Commission", "COMM-SYS");
        ensureSystemAccountExists("PRODUIT", "Compte Système Produit", "PROD-SYS");
        ensureSystemAccountExists("ATTENTE", "Compte Système Attente", "ATTENTE-SYS");

        log.info("Initialisation des comptes système terminée");
    }

    @Transactional
    public void ensureSystemAccountExists(String typeCompte, String nomCompte, String numeroCompte) {
        // Vérification par numero_compte (contrainte unique)
        Optional<CompteSysteme> existing = compteSystemeRepository.findByNumeroCompte(numeroCompte);

        if (existing.isPresent()) {
            log.debug("Compte système {} déjà existant", numeroCompte);
            return;
        }

        log.info("Création du compte système: {}", typeCompte);

        // Créer un CompteSysteme
        CompteSysteme compte = CompteSysteme.create(typeCompte, nomCompte, numeroCompte);

        compteSystemeRepository.save(compte);
        log.info("Compte système créé: {} ({})", typeCompte, numeroCompte);
    }

    // MÉTHODES AJOUTÉES pour compatibilité avec MouvementService

    /**
     * S'assure que tous les comptes système existent
     * (Alias pour initializeSystemAccounts pour compatibilité)
     */
    @Transactional
    public void ensureSystemAccountsExist() {
        log.debug("Vérification de l'existence de tous les comptes système...");
        initializeSystemAccounts();
    }

    /**
     * Assure l'existence d'un compte système spécifique et le retourne
     * @param typeCompte Le type de compte (TAXE, EMF, etc.)
     * @param nomCompte Le nom du compte
     * @param numeroCompte Le numéro du compte
     * @return Le CompteSysteme créé ou existant
     */
    @Transactional
    public CompteSysteme ensureSystemCompteExists(String typeCompte, String nomCompte, String numeroCompte) {
        // Vérifier si le compte existe déjà
        Optional<CompteSysteme> existing = compteSystemeRepository.findByNumeroCompte(numeroCompte);

        if (existing.isPresent()) {
            log.debug("Compte système {} existe déjà", numeroCompte);
            return existing.get();
        }

        // Créer le compte s'il n'existe pas
        log.info("Création du compte système: {}", typeCompte);
        CompteSysteme compte = CompteSysteme.create(typeCompte, nomCompte, numeroCompte);
        CompteSysteme savedCompte = compteSystemeRepository.save(compte);

        log.info("Compte système créé: {} ({})", typeCompte, numeroCompte);
        return savedCompte;
    }

    /**
     * Récupère un compte système par son type
     * @param typeCompte Le type de compte cherché
     * @return Optional contenant le compte s'il existe
     */
    @Transactional(readOnly = true)
    public Optional<CompteSysteme> findByTypeCompte(String typeCompte) {
        return compteSystemeRepository.findByTypeCompte(typeCompte);
    }

    /**
     * Récupère un compte système par son numéro
     * @param numeroCompte Le numéro de compte cherché
     * @return Optional contenant le compte s'il existe
     */
    @Transactional(readOnly = true)
    public Optional<CompteSysteme> findByNumeroCompte(String numeroCompte) {
        return compteSystemeRepository.findByNumeroCompte(numeroCompte);
    }
}