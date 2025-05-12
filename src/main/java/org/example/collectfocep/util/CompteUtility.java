package org.example.collectfocep.util;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
public class CompteUtility {

    private final CompteRepository compteRepository;
    private final CompteSystemeRepository compteSystemeRepository;
    private final CompteCollecteurRepository compteCollecteurRepository;
    private final CollecteurRepository collecteurRepository;

    @Autowired
    public CompteUtility(
            CompteRepository compteRepository,
            CompteSystemeRepository compteSystemeRepository,
            CompteCollecteurRepository compteCollecteurRepository,
            CollecteurRepository collecteurRepository) {
        this.compteRepository = compteRepository;
        this.compteSystemeRepository = compteSystemeRepository;
        this.compteCollecteurRepository = compteCollecteurRepository;
        this.collecteurRepository = collecteurRepository;
    }

    /**
     * Initialise les comptes système au démarrage de l'application
     * Cette méthode est sécurisée pour ne pas bloquer le démarrage de l'application
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Initialisation des comptes système...");
            ensureSystemAccountsExist();
            log.info("Comptes système initialisés avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des comptes système: {}", e.getMessage());
            log.debug("Détails de l'erreur:", e);
        }
    }

    /**
     * Assure que les comptes système nécessaires existent
     */
    @Transactional
    public void ensureSystemAccountsExist() {
        try {
            // Comptes système communs
            ensureSystemCompteExists("TAXE", "Compte TVA", "TAXE-SYS");
            ensureSystemCompteExists("PRODUIT", "Compte Produit FOCEP", "PROD-SYS");
            ensureSystemCompteExists("ATTENTE", "Compte Attente Système", "ATT-SYS");
        } catch (Exception e) {
            log.error("Erreur lors de la création des comptes système: {}", e.getMessage());
            log.debug("Détails de l'erreur:", e);
            throw e;
        }
    }
    /**
     * Assure qu'un compte système spécifique existe
     * @param typeCompte Type du compte à créer
     * @param nomCompte Nom du compte
     * @param numeroCompte Numéro du compte
     * @return Le compte existant ou nouvellement créé
     */
    @Transactional
    public CompteSysteme ensureSystemCompteExists(String typeCompte, String nomCompte, String numeroCompte) {
        try {
            // Vérifier d'abord par le numéro de compte pour éviter les duplications
            Optional<CompteSysteme> compteOpt = compteSystemeRepository.findByNumeroCompte(numeroCompte);
            if (compteOpt.isPresent()) {
                log.debug("Compte système {} déjà existant avec numéro {}: {}", typeCompte, numeroCompte, compteOpt.get().getId());
                return compteOpt.get();
            }

            // Ensuite vérifier par type de compte
            compteOpt = compteSystemeRepository.findByTypeCompte(typeCompte);
            if (compteOpt.isPresent()) {
                log.debug("Compte système {} déjà existant: {}", typeCompte, compteOpt.get().getId());
                return compteOpt.get();
            }

            log.info("Création du compte système: {} avec numéro {}", typeCompte, numeroCompte);
            CompteSysteme compte = CompteSysteme.create(typeCompte, nomCompte, numeroCompte);
            CompteSysteme savedCompte = compteSystemeRepository.save(compte);

            log.info("Compte système {} créé avec l'ID: {}", typeCompte, savedCompte.getId());
            return savedCompte;
        } catch (Exception e) {
            log.error("Erreur lors de la création du compte système {}: {}", typeCompte, e.getMessage());
            // Si l'erreur est due à une contrainte unique, essayer de récupérer le compte existant
            if (e.getMessage().contains("Duplicate entry")) {
                log.warn("Tentative de création d'un compte système déjà existant, récupération du compte existant");
                return compteSystemeRepository.findByTypeCompte(typeCompte)
                        .orElseThrow(() -> new RuntimeException("Impossible de récupérer le compte système existant"));
            }
            throw e;
        }
    }

    /**
     * Assure que le compte d'attente existe pour un collecteur donné
     * Cette méthode est sécurisée pour éviter les erreurs lors de la création des comptes
     */
    @Transactional
    public CompteCollecteur ensureCompteAttenteExists(Collecteur collecteur) {
        try {
            // Validation préliminaire
            if (collecteur == null) {
                log.error("Impossible de créer un compte d'attente pour un collecteur null");
                throw new IllegalArgumentException("Le collecteur ne peut pas être null");
            }

            if (collecteur.getId() == null) {
                log.error("Impossible de créer un compte d'attente pour un collecteur sans ID");
                throw new IllegalArgumentException("L'ID du collecteur ne peut pas être null");
            }

            // Recherche du compte d'attente existant
            log.debug("Recherche du compte d'attente pour le collecteur ID={}", collecteur.getId());
            Optional<CompteCollecteur> optCompte = compteCollecteurRepository
                    .findByCollecteurAndTypeCompte(collecteur, "ATTENTE");

            if (optCompte.isPresent()) {
                log.debug("Compte d'attente trouvé pour le collecteur ID={}: {}",
                        collecteur.getId(), optCompte.get().getNumeroCompte());
                return optCompte.get();
            }

            // Vérification de l'existence du collecteur dans la base
            boolean collecteurExists = collecteurRepository.existsById(collecteur.getId());
            if (!collecteurExists) {
                log.error("Le collecteur avec ID={} n'existe pas dans la base de données", collecteur.getId());
                throw new EntityNotFoundException("Collecteur non trouvé avec ID: " + collecteur.getId());
            }

            // Rechargement du collecteur pour éviter les erreurs de session
            Collecteur collecteurAttache = collecteurRepository.findById(collecteur.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Collecteur non trouvé avec ID: " + collecteur.getId()));

            // Création d'un nouveau compte d'attente
            log.info("Création d'un compte d'attente pour le collecteur ID={}, Nom={}",
                    collecteurAttache.getId(), collecteurAttache.getNom());

            CompteCollecteur compte = new CompteCollecteur();
            compte.setCollecteur(collecteurAttache);
            compte.setNomCompte("Compte Attente - " + collecteurAttache.getNom());
            compte.setNumeroCompte("ATT" + collecteurAttache.getId());
            compte.setTypeCompte("ATTENTE");
            compte.setSolde(0.0);

            CompteCollecteur savedCompte = compteCollecteurRepository.save(compte);
            log.info("Compte d'attente créé avec succès: ID={}", savedCompte.getId());
            return savedCompte;
        } catch (Exception e) {
            log.error("Erreur lors de la création du compte d'attente pour le collecteur: {}",
                    collecteur != null ? collecteur.getId() : "null", e);
            // Ne pas masquer l'exception, la propager pour être gérée par l'appelant
            throw e;
        }
    }
}