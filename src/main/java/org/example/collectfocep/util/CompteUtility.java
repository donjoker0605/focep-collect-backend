package org.example.collectfocep.util;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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
            // L'application doit continuer malgré cette erreur
        }
    }

    /**
     * Assure que les comptes système nécessaires existent
     * Chaque compte est créé dans sa propre transaction pour éviter les rollbacks en cascade
     */
    public void ensureSystemAccountsExist() {
        // Créer chaque compte dans sa propre transaction
        createSystemAccountSafely("TAXE", "Compte TVA", "TAXE-SYS");
        createSystemAccountSafely("PRODUIT", "Compte Produit FOCEP", "PROD-SYS");
        createSystemAccountSafely("ATTENTE", "Compte Attente Système", "ATT-SYS");
    }

    /**
     * Crée un compte système dans sa propre transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSystemAccountSafely(String typeCompte, String nomCompte, String numeroCompte) {
        try {
            ensureSystemCompteExists(typeCompte, nomCompte, numeroCompte);
        } catch (Exception e) {
            log.error("Erreur lors de la création du compte {}: {}", typeCompte, e.getMessage());
            // Ne pas propager l'exception pour ne pas bloquer les autres créations
        }
    }

    /**
     * Assure qu'un compte système spécifique existe
     * Utilise une approche défensive avec multiple tentatives de récupération
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompteSysteme ensureSystemCompteExists(String typeCompte, String nomCompte, String numeroCompte) {
        // Étape 1: Vérification double avec flush pour s'assurer de la cohérence
        CompteSysteme existingCompte = findExistingSystemCompte(typeCompte, numeroCompte);
        if (existingCompte != null) {
            return existingCompte;
        }

        try {
            // Étape 2: Tentative de création
            log.info("Création du compte système: {} avec numéro {}", typeCompte, numeroCompte);
            CompteSysteme compte = CompteSysteme.create(typeCompte, nomCompte, numeroCompte);
            CompteSysteme savedCompte = compteSystemeRepository.save(compte);
            compteSystemeRepository.flush(); // Force l'écriture en DB

            log.info("Compte système {} créé avec l'ID: {}", typeCompte, savedCompte.getId());
            return savedCompte;

        } catch (DataIntegrityViolationException e) {
            // Étape 3: Récupération après violation de contrainte
            log.warn("Contrainte d'intégrité violée pour {}, tentative de récupération", typeCompte);
            return recoverExistingSystemCompte(typeCompte, numeroCompte, e);

        } catch (Exception e) {
            log.error("Erreur inattendue lors de la création du compte système {}: {}", typeCompte, e.getMessage());
            throw new RuntimeException("Impossible de créer le compte système: " + typeCompte, e);
        }
    }

    /**
     * Recherche un compte système existant de manière défensive
     */
    private CompteSysteme findExistingSystemCompte(String typeCompte, String numeroCompte) {
        // Méthode 1: Par numéro de compte
        Optional<CompteSysteme> compteOpt = compteSystemeRepository.findByNumeroCompte(numeroCompte);
        if (compteOpt.isPresent()) {
            log.debug("Compte système trouvé par numéro {}: {}", numeroCompte, compteOpt.get().getId());
            return compteOpt.get();
        }

        // Méthode 2: Par type de compte
        compteOpt = compteSystemeRepository.findByTypeCompte(typeCompte);
        if (compteOpt.isPresent()) {
            log.debug("Compte système trouvé par type {}: {}", typeCompte, compteOpt.get().getId());
            return compteOpt.get();
        }

        return null;
    }

    /**
     * Récupère un compte système après une violation de contrainte
     */
    private CompteSysteme recoverExistingSystemCompte(String typeCompte, String numeroCompte, Exception originalException) {
        // Attendre un peu pour laisser la transaction se terminer
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Tentative 1: Par numéro de compte
        Optional<CompteSysteme> compteOpt = compteSystemeRepository.findByNumeroCompte(numeroCompte);
        if (compteOpt.isPresent()) {
            log.info("Compte système récupéré par numéro {}: {}", numeroCompte, compteOpt.get().getId());
            return compteOpt.get();
        }

        // Tentative 2: Par type de compte
        compteOpt = compteSystemeRepository.findByTypeCompte(typeCompte);
        if (compteOpt.isPresent()) {
            log.info("Compte système récupéré par type {}: {}", typeCompte, compteOpt.get().getId());
            return compteOpt.get();
        }

        // Tentative 3: Recherche plus large dans la table des comptes
        Optional<Compte> compteGeneral = compteRepository.findByTypeCompte(typeCompte);
        if (compteGeneral.isPresent() && compteGeneral.get() instanceof CompteSysteme) {
            log.info("Compte système récupéré via table générale: {}", compteGeneral.get().getId());
            return (CompteSysteme) compteGeneral.get();
        }

        // Si toutes les tentatives échouent
        log.error("Impossible de récupérer le compte système {} après violation de contrainte", typeCompte);
        throw new RuntimeException("Impossible de récupérer le compte système existant: " + typeCompte, originalException);
    }

    /**
     * Assure que le compte d'attente existe pour un collecteur donné
     * Version améliorée avec gestion des erreurs plus robuste
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompteCollecteur ensureCompteAttenteExists(Collecteur collecteur) {
        try {
            // Validation préliminaire
            if (collecteur == null || collecteur.getId() == null) {
                throw new IllegalArgumentException("Le collecteur et son ID ne peuvent pas être null");
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

            // Rechargement du collecteur pour s'assurer qu'il existe
            Collecteur collecteurAttache = collecteurRepository.findById(collecteur.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Collecteur non trouvé avec ID: " + collecteur.getId()));

            // Génération d'un numéro de compte unique
            String numeroCompte = generateUniqueAccountNumber("ATT", collecteurAttache);

            // Création du nouveau compte
            log.info("Création d'un compte d'attente pour le collecteur ID={}, Nom={}",
                    collecteurAttache.getId(), collecteurAttache.getNom());

            CompteCollecteur compte = new CompteCollecteur();
            compte.setCollecteur(collecteurAttache);
            compte.setNomCompte("Compte Attente - " + collecteurAttache.getNom());
            compte.setNumeroCompte(numeroCompte);
            compte.setTypeCompte("ATTENTE");
            compte.setSolde(0.0);

            CompteCollecteur savedCompte = compteCollecteurRepository.save(compte);
            compteCollecteurRepository.flush(); // Force l'écriture

            log.info("Compte d'attente créé avec succès: ID={}, Numéro={}",
                    savedCompte.getId(), savedCompte.getNumeroCompte());
            return savedCompte;

        } catch (DataIntegrityViolationException e) {
            log.warn("Violation de contrainte lors de la création du compte d'attente, tentative de récupération");
            // Tenter de récupérer le compte nouvellement créé
            return compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "ATTENTE")
                    .orElseThrow(() -> new RuntimeException("Impossible de créer ou récupérer le compte d'attente", e));

        } catch (Exception e) {
            log.error("Erreur lors de la création du compte d'attente pour le collecteur: {}",
                    collecteur.getId(), e);
            throw e;
        }
    }

    /**
     * Génère un numéro de compte unique pour éviter les conflits
     */
    private String generateUniqueAccountNumber(String prefix, Collecteur collecteur) {
        String baseNumber = prefix + collecteur.getId();

        // Vérifier l'unicité
        if (!compteRepository.existsByNumeroCompte(baseNumber)) {
            return baseNumber;
        }

        // Si le numéro existe déjà, ajouter un suffixe
        for (int i = 1; i <= 999; i++) {
            String uniqueNumber = baseNumber + "-" + String.format("%03d", i);
            if (!compteRepository.existsByNumeroCompte(uniqueNumber)) {
                return uniqueNumber;
            }
        }

        throw new RuntimeException("Impossible de générer un numéro de compte unique pour " + prefix);
    }
}