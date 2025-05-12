package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.interfaces.CompteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CompteServiceImpl implements CompteService {
    private final CompteRepository compteRepository;
    private final CompteCollecteurRepository compteCollecteurRepository;
    private final CompteLiaisonRepository compteLiaisonRepository;
    private final CompteServiceRepository compteServiceRepository;
    private final CompteManquantRepository compteManquantRepository;
    private final CompteRemunerationRepository compteRemunerationRepository;
    private final CompteAttenteRepository compteAttenteRepository;
    private final CompteChargeRepository compteChargeRepository;
    private final CollecteurRepository collecteurRepository;
    private final CompteClientRepository compteClientRepository;
    private final ClientRepository clientRepository;

    @Override
    public Page<Compte> getAllComptes(Pageable pageable) {
        log.debug("Récupération paginée de tous les comptes");
        return compteRepository.findAll(pageable);
    }

    @Override
    public List<Compte> getAllComptes() {
        log.debug("Récupération de tous les comptes");
        return compteRepository.findAll();
    }

    @Override
    public CompteCollecteur findServiceAccount(Collecteur collecteur) {
        log.debug("Recherche du compte service pour collecteur ID={}", collecteur.getId());

        // 1. Essayer d'abord de trouver un CompteCollecteur de type SERVICE
        Optional<CompteCollecteur> compteCollecteur = compteCollecteurRepository
                .findByCollecteurAndTypeCompte(collecteur, "SERVICE");

        if (compteCollecteur.isPresent()) {
            log.debug("CompteCollecteur de type SERVICE trouvé: ID={}", compteCollecteur.get().getId());
            return compteCollecteur.get();
        }

        // 2. Sinon, chercher un CompteServiceEntity
        Optional<CompteServiceEntity> compteService = compteServiceRepository
                .findFirstByCollecteur(collecteur);

        if (compteService.isPresent()) {
            log.debug("CompteServiceEntity trouvé, conversion en CompteCollecteur");

            // Créer un CompteCollecteur à partir du CompteServiceEntity existant
            CompteCollecteur converted = createCompteCollecteurFromService(compteService.get(), collecteur);

            return converted;
        }

        log.error("Aucun compte service trouvé pour collecteur ID={}", collecteur.getId());
        throw new ResourceNotFoundException("Compte service non trouvé pour le collecteur: " + collecteur.getId());
    }

    /**
     * Créer un CompteCollecteur à partir d'un CompteServiceEntity
     * sans modifier la base de données
     */
    private CompteCollecteur createCompteCollecteurFromService(CompteServiceEntity serviceEntity, Collecteur collecteur) {
        CompteCollecteur compteCollecteur = new CompteCollecteur();
        compteCollecteur.setId(serviceEntity.getId());
        compteCollecteur.setCollecteur(collecteur);
        compteCollecteur.setNomCompte(serviceEntity.getNomCompte());
        compteCollecteur.setNumeroCompte(serviceEntity.getNumeroCompte());
        compteCollecteur.setSolde(serviceEntity.getSolde());
        compteCollecteur.setTypeCompte("SERVICE");

        return compteCollecteur;
    }

    @Override
    public CompteCollecteur findWaitingAccount(Collecteur collecteur) {
        // Essayer d'abord de trouver un CompteCollecteur de type ATTENTE
        Optional<CompteCollecteur> compteCollecteur = compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "ATTENTE");

        if (compteCollecteur.isPresent()) {
            return compteCollecteur.get();
        }

        // Sinon, chercher un CompteAttente
        Optional<CompteAttente> compteAttente = compteAttenteRepository.findFirstByCollecteur(collecteur);

        if (compteAttente.isPresent()) {
            return convertToCompteCollecteur(compteAttente.get(), collecteur);
        }

        throw new ResourceNotFoundException("Compte d'attente non trouvé pour le collecteur: " + collecteur.getId());
    }

    @Override
    public CompteCollecteur findSalaryAccount(Collecteur collecteur) {
        // Essayer d'abord de trouver un CompteCollecteur de type SALAIRE
        Optional<CompteCollecteur> compteCollecteur = compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SALAIRE");

        if (compteCollecteur.isPresent()) {
            return compteCollecteur.get();
        }

        // Sinon, chercher un CompteRemuneration
        Optional<CompteRemuneration> compteRemuneration = compteRemunerationRepository.findFirstByCollecteur(collecteur);

        if (compteRemuneration.isPresent()) {
            return convertToCompteCollecteur(compteRemuneration.get(), collecteur);
        }

        throw new ResourceNotFoundException("Compte salaire non trouvé pour le collecteur: " + collecteur.getId());
    }

    @Override
    public CompteLiaison findLiaisonAccount(Agence agence) {
        return compteLiaisonRepository.findByAgenceAndTypeCompte(agence, "LIAISON")
                .orElseThrow(() -> new ResourceNotFoundException("Compte de liaison non trouvé pour l'agence: " + agence.getId()));
    }

    @Override
    public Compte findProduitAccount() {
        return compteRepository.findByTypeCompte("PRODUIT")
                .orElseThrow(() -> new ResourceNotFoundException("Compte produit non trouvé"));
    }

    @Override
    public Compte findTVAAccount() {
        return compteRepository.findByTypeCompte("TVA")
                .orElseThrow(() -> new ResourceNotFoundException("Compte TVA non trouvé"));
    }

    @Override
    public Compte findChargeAccount(Collecteur collecteur) {
        // Essayer d'abord de trouver un CompteCharge
        Optional<CompteCharge> compteCharge = compteChargeRepository.findFirstByCollecteur(collecteur);

        if (compteCharge.isPresent()) {
            return compteCharge.get();
        }

        // Sinon, utiliser la méthode originale
        return compteRepository.findByTypeCompteAndCollecteurId("CHARGE", collecteur.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte de charge non trouvé"));
    }

    @Override
    public Optional<Compte> findByTypeCompte(String typeCompte) {
        return compteRepository.findByTypeCompte(typeCompte);
    }

    @Override
    public double getSolde(Long compteId) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé: " + compteId));
        return compte.getSolde();
    }

    @Override
    public List<Compte> findByAgenceId(Long agenceId) {
        return compteRepository.findByAgenceId(agenceId);
    }

    @Override
    public List<Compte> findByCollecteurId(Long collecteurId) {
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        List<Compte> comptes = new ArrayList<>();

        // Ajouter les CompteCollecteur
        comptes.addAll(compteCollecteurRepository.findByCollecteur(collecteur));

        // Ajouter les CompteServiceEntity
        comptes.addAll(compteServiceRepository.findAllByCollecteur(collecteur));

        // Ajouter les CompteManquant
        comptes.addAll(compteManquantRepository.findAllByCollecteur(collecteur));

        // Ajouter les CompteRemuneration
        comptes.addAll(compteRemunerationRepository.findAllByCollecteur(collecteur));

        // Ajouter les CompteAttente
        comptes.addAll(compteAttenteRepository.findAllByCollecteur(collecteur));

        // Ajouter les CompteCharge
        comptes.addAll(compteChargeRepository.findAllByCollecteur(collecteur));

        return comptes;
    }

    @Override
    public Page<Compte> findByCollecteurId(Long collecteurId, Pageable pageable) {
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé: " + collecteurId));

        List<Compte> comptes = new ArrayList<>();

        // Ajouter les CompteCollecteur
        comptes.addAll(compteCollecteurRepository.findByCollecteur(collecteur));

        // Ajouter les CompteServiceEntity
        compteServiceRepository.findAllByCollecteur(collecteur).forEach(comptes::add);

        // Ajouter les CompteManquant
        comptes.addAll(compteManquantRepository.findAllByCollecteur(collecteur));

        // Ajouter les CompteRemuneration
        comptes.addAll(compteRemunerationRepository.findAllByCollecteur(collecteur));

        // Ajouter les CompteAttente
        comptes.addAll(compteAttenteRepository.findAllByCollecteur(collecteur));

        // Ajouter les CompteCharge
        comptes.addAll(compteChargeRepository.findAllByCollecteur(collecteur));

        // Appliquer la pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), comptes.size());

        // Gérer le cas où start > size de la liste
        if (start > comptes.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, comptes.size());
        }

        return new PageImpl<>(comptes.subList(start, end), pageable, comptes.size());
    }

    @Override
    public Page<Compte> findByAgenceId(Long agenceId, Pageable pageable) {
        // Implémentation similaire à celle de findByCollecteurId avec pagination
        List<Compte> comptes = compteRepository.findByAgenceId(agenceId);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), comptes.size());

        // Gérer le cas où start > size de la liste
        if (start > comptes.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, comptes.size());
        }

        return new PageImpl<>(comptes.subList(start, end), pageable, comptes.size());
    }

    @Transactional
    public void createCollecteurAccounts(Collecteur collecteur) {
        log.info("Début de la création des comptes pour le collecteur ID: {}", collecteur.getId());

        String codeAgence = collecteur.getAgence().getCodeAgence();
        String collecteurIdFormatted = String.format("%08d", collecteur.getId());

        // Créer un CompteService
        if (!compteServiceRepository.existsByCollecteur(collecteur)) {
            CompteServiceEntity compteService = new CompteServiceEntity();
            compteService.setCollecteur(collecteur);
            compteService.setTypeCompte("SERVICE");
            compteService.setNomCompte("Compte Principal Collecte " + collecteur.getNom());
            compteService.setNumeroCompte("373" + codeAgence + collecteurIdFormatted);
            compteService.setSolde(0.0);

            compteServiceRepository.save(compteService);
            log.info("Compte SERVICE créé pour le collecteur: {}", collecteur.getId());

            // 2. Créer aussi un CompteCollecteur correspondant pour la compatibilité
            createCompteCollecteurForCompatibility(compteService, collecteur);
        }

        // Créer un CompteManquant
        if (!compteManquantRepository.existsByCollecteur(collecteur)) {
            CompteManquant compteManquant = new CompteManquant();
            compteManquant.setCollecteur(collecteur);
            compteManquant.setTypeCompte("MANQUANT");
            compteManquant.setNomCompte("Compte Manquant " + collecteur.getNom());
            compteManquant.setNumeroCompte("374" + codeAgence + collecteurIdFormatted);
            compteManquant.setSolde(0.0);

            compteManquantRepository.save(compteManquant);
            log.info("Compte MANQUANT créé pour le collecteur: {}", collecteur.getId());
        }

        // Créer un CompteRemuneration
        if (!compteRemunerationRepository.existsByCollecteur(collecteur)) {
            CompteRemuneration compteRemuneration = new CompteRemuneration();
            compteRemuneration.setCollecteur(collecteur);
            compteRemuneration.setTypeCompte("REMUNERATION");
            compteRemuneration.setNomCompte("Compte Rémunération " + collecteur.getNom());
            compteRemuneration.setNumeroCompte("375" + codeAgence + collecteurIdFormatted);
            compteRemuneration.setSolde(0.0);

            compteRemunerationRepository.save(compteRemuneration);
            log.info("Compte REMUNERATION créé pour le collecteur: {}", collecteur.getId());
        }

        // Créer un CompteAttente
        if (!compteAttenteRepository.existsByCollecteur(collecteur)) {
            CompteAttente compteAttente = new CompteAttente();
            compteAttente.setCollecteur(collecteur);
            compteAttente.setTypeCompte("ATTENTE");
            compteAttente.setNomCompte("Compte Attente " + collecteur.getNom());
            compteAttente.setNumeroCompte("ATT" + codeAgence + collecteurIdFormatted);
            compteAttente.setSolde(0.0);

            compteAttenteRepository.save(compteAttente);
            log.info("Compte ATTENTE créé pour le collecteur: {}", collecteur.getId());
        }

        // Créer un CompteCharge
        if (!compteChargeRepository.existsByCollecteur(collecteur)) {
            CompteCharge compteCharge = new CompteCharge();
            compteCharge.setCollecteur(collecteur);
            compteCharge.setTypeCompte("CHARGE");
            compteCharge.setNomCompte("Compte Charge " + collecteur.getNom());
            compteCharge.setNumeroCompte("376" + codeAgence + collecteurIdFormatted);
            compteCharge.setSolde(0.0);

            compteChargeRepository.save(compteCharge);
            log.info("Compte CHARGE créé pour le collecteur: {}", collecteur.getId());
        }

        log.info("Fin de la création des comptes pour le collecteur: {}", collecteur.getId());
    }

    /**
     * Créer un CompteCollecteur pour assurer la compatibilité
     */
    private void createCompteCollecteurForCompatibility(CompteServiceEntity serviceEntity, Collecteur collecteur) {
        try {
            // Vérifier si un CompteCollecteur existe déjà pour ce compte
            Optional<CompteCollecteur> existing = compteCollecteurRepository.findById(serviceEntity.getId());

            if (existing.isEmpty()) {
                CompteCollecteur compteCollecteur = new CompteCollecteur();
                compteCollecteur.setId(serviceEntity.getId()); // Même ID que CompteServiceEntity
                compteCollecteur.setCollecteur(collecteur);
                compteCollecteur.setNomCompte(serviceEntity.getNomCompte());
                compteCollecteur.setNumeroCompte(serviceEntity.getNumeroCompte());
                compteCollecteur.setSolde(serviceEntity.getSolde());
                compteCollecteur.setTypeCompte("SERVICE");

                compteCollecteurRepository.save(compteCollecteur);
                log.info("CompteCollecteur de compatibilité créé pour CompteServiceEntity ID={}", serviceEntity.getId());
            }
        } catch (Exception e) {
            log.warn("Impossible de créer CompteCollecteur de compatibilité: {}", e.getMessage());
            // Ne pas faire échouer la création principale
        }
    }

    /**
     * Génère un numéro de compte unique basé sur l'ID du collecteur
     */
    private String generateAccountNumber(Collecteur collecteur) {
        // Format: 8 chiffres, complétés par des zéros si nécessaire
        return String.format("%08d", collecteur.getId());
    }

    /**
     * Convertit un Compte en CompteCollecteur pour maintenir la compatibilité
     */
    private CompteCollecteur convertToCompteCollecteur(Compte compte, Collecteur collecteur) {
        if (compte == null) return null;

        CompteCollecteur compteCollecteur = new CompteCollecteur();
        compteCollecteur.setId(compte.getId());
        compteCollecteur.setCollecteur(collecteur);
        compteCollecteur.setNomCompte(compte.getNomCompte());
        compteCollecteur.setNumeroCompte(compte.getNumeroCompte());
        compteCollecteur.setSolde(compte.getSolde());
        compteCollecteur.setTypeCompte(compte.getTypeCompte());

        return compteCollecteur;
    }

    @Override
    @Transactional
    public void deleteCompte(Long id) {
        log.info("Suppression du compte avec l'ID: {}", id);

        // Vérifier d'abord que le compte existe
        Compte compte = compteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte non trouvé avec l'ID: " + id));

        // Vérifier les contraintes métier avant la suppression
        // Par exemple, un compte ne devrait pas être supprimé s'il a des mouvements associés
        // ou si le solde n'est pas à zéro
        if (compte.getSolde() != 0) {
            throw new IllegalStateException("Impossible de supprimer un compte avec un solde non nul: " + compte.getSolde());
        }

        // Procéder à la suppression
        compteRepository.deleteById(id);
        log.info("Compte {} supprimé avec succès", id);
    }

    @Override
    @Transactional
    public Compte saveCompte(Compte compte) {
        log.info("Sauvegarde du compte: {}", compte.getId() != null ? compte.getId() : "nouveau compte");

        // Validation basique
        if (compte.getNomCompte() == null || compte.getNomCompte().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du compte ne peut pas être vide");
        }

        // Générer un numéro de compte si nouveau compte
        if (compte.getNumeroCompte() == null || compte.getNumeroCompte().trim().isEmpty()) {
            String prefixe = compte.getTypeCompte().substring(0, Math.min(3, compte.getTypeCompte().length())).toUpperCase();
            String numeroGenere = prefixe + "-" + System.currentTimeMillis();
            compte.setNumeroCompte(numeroGenere);
            log.info("Génération d'un nouveau numéro de compte: {}", numeroGenere);
        }

        // Règles de validation spécifiques selon le type de compte
        // Par exemple, vérifier l'unicité du numéro de compte
        if (compte.getId() != null) { // Vérification uniquement pour les comptes existants
            compteRepository.findAll().stream()
                    .filter(c -> c.getNumeroCompte().equals(compte.getNumeroCompte()))
                    .filter(c -> !c.getId().equals(compte.getId()))
                    .findAny()
                    .ifPresent(c -> {
                        throw new IllegalStateException("Un compte avec le numéro " + compte.getNumeroCompte() + " existe déjà");
                    });
        }

        // Sauvegarde du compte
        Compte compteSauvegarde = compteRepository.save(compte);
        log.info("Compte sauvegardé avec succès: {}", compteSauvegarde.getId());

        return compteSauvegarde;
    }

    @Override
    public Optional<Compte> getCompteById(Long id) {
        log.debug("Récupération du compte avec l'ID: {}", id);
        return compteRepository.findById(id);
    }

    @Override
    public List<Compte> findByClientId(Long clientId) {
        log.debug("Récupération des comptes pour le client ID: {}", clientId);

        // Vérifier que le client existe
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + clientId));

        // Récupérer tous les comptes du client
        List<Compte> comptes = new ArrayList<>();

        // Ajouter les CompteClient - Utiliser findAllByClient au lieu de findByClient
        comptes.addAll(compteClientRepository.findAllByClient(client));

        return comptes;
    }
}