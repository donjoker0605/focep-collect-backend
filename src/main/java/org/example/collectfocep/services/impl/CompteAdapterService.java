package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 🔧 Service d'adaptation pour l'Option A
 * Ce service fait le lien entre les repositories spécialisés
 * et les demandes génériques de CompteCollecteur
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CompteAdapterService {

    // Repositories spécialisés (Option A)
    private final CompteServiceRepository compteServiceRepository;
    private final CompteManquantRepository compteManquantRepository;
    private final CompteAttenteRepository compteAttenteRepository;
    private final CompteSalaireCollecteurRepository compteSalaireCollecteurRepository;
    private final CompteChargeCollecteRepository compteChargeCollecteRepository;

    // Repository adapteur
    private final CompteCollecteurRepository compteCollecteurRepository;

    /**
     * ✅ MÉTHODE PRINCIPALE: Trouve ou crée un CompteCollecteur adapté
     */
    @Transactional(readOnly = true)
    public CompteCollecteur findByCollecteurAndTypeCompte(Collecteur collecteur, String typeCompte) {
        log.debug("Recherche compte {} pour collecteur {}", typeCompte, collecteur.getId());

        // 1. D'abord chercher dans CompteCollecteur (cache/adapters)
        Optional<CompteCollecteur> existing = compteCollecteurRepository
                .findByCollecteurAndTypeCompte(collecteur, typeCompte);

        if (existing.isPresent()) {
            log.debug("CompteCollecteur trouvé: {}", existing.get().getId());
            return existing.get();
        }

        // 2. Sinon, chercher dans les repositories spécialisés et adapter
        switch (typeCompte.toUpperCase()) {
            case "SERVICE":
                return adaptFromServiceEntity(collecteur);
            case "MANQUANT":
                return adaptFromManquantEntity(collecteur);
            case "ATTENTE":
                return adaptFromAttenteEntity(collecteur);
            case "REMUNERATION":
                return adaptFromRemunerationEntity(collecteur);
            case "CHARGE":
                return adaptFromChargeEntity(collecteur);
            default:
                throw new IllegalArgumentException("Type de compte non supporté: " + typeCompte);
        }
    }

    /**
     * ✅ ADAPTATION: CompteServiceEntity → CompteCollecteur
     */
    private CompteCollecteur adaptFromServiceEntity(Collecteur collecteur) {
        Optional<CompteServiceEntity> serviceEntity = compteServiceRepository
                .findFirstByCollecteur(collecteur);

        if (serviceEntity.isPresent()) {
            return createAdapterFromServiceEntity(serviceEntity.get(), collecteur);
        }

        throw new ResourceNotFoundException("Compte service non trouvé pour collecteur: " + collecteur.getId());
    }

    /**
     * ✅ ADAPTATION: CompteManquant → CompteCollecteur
     */
    private CompteCollecteur adaptFromManquantEntity(Collecteur collecteur) {
        Optional<CompteManquant> manquantEntity = compteManquantRepository
                .findFirstByCollecteur(collecteur);

        if (manquantEntity.isPresent()) {
            return createAdapterFromCompte(manquantEntity.get(), collecteur, "MANQUANT");
        }

        throw new ResourceNotFoundException("Compte manquant non trouvé pour collecteur: " + collecteur.getId());
    }

    /**
     * ✅ ADAPTATION: CompteAttente → CompteCollecteur
     */
    private CompteCollecteur adaptFromAttenteEntity(Collecteur collecteur) {
        Optional<CompteAttente> attenteEntity = compteAttenteRepository
                .findFirstByCollecteur(collecteur);

        if (attenteEntity.isPresent()) {
            return createAdapterFromCompte(attenteEntity.get(), collecteur, "ATTENTE");
        }

        throw new ResourceNotFoundException("Compte attente non trouvé pour collecteur: " + collecteur.getId());
    }

    /**
     * ✅ ADAPTATION: CompteSalaireCollecteur → CompteCollecteur
     */
    private CompteCollecteur adaptFromRemunerationEntity(Collecteur collecteur) {
        Optional<CompteSalaireCollecteur> remunerationEntity = compteSalaireCollecteurRepository
                .findFirstByCollecteur(collecteur);

        if (remunerationEntity.isPresent()) {
            return createAdapterFromCompte(remunerationEntity.get(), collecteur, "REMUNERATION");
        }

        throw new ResourceNotFoundException("Compte rémunération non trouvé pour collecteur: " + collecteur.getId());
    }

    /**
     * ✅ ADAPTATION: CompteChargeCollecte → CompteCollecteur
     */
    private CompteCollecteur adaptFromChargeEntity(Collecteur collecteur) {
        Optional<CompteChargeCollecte> chargeEntity = compteChargeCollecteRepository
                .findFirstByCollecteur(collecteur);

        if (chargeEntity.isPresent()) {
            return createAdapterFromCompte(chargeEntity.get(), collecteur, "CHARGE");
        }

        throw new ResourceNotFoundException("Compte charge non trouvé pour collecteur: " + collecteur.getId());
    }

    // =====================================
    // MÉTHODES UTILITAIRES DE CRÉATION D'ADAPTERS
    // =====================================

    private CompteCollecteur createAdapterFromServiceEntity(CompteServiceEntity serviceEntity, Collecteur collecteur) {
        CompteCollecteur adapter = new CompteCollecteur();
        adapter.setId(serviceEntity.getId());
        adapter.setCollecteur(collecteur);
        adapter.setNomCompte(serviceEntity.getNomCompte());
        adapter.setNumeroCompte(serviceEntity.getNumeroCompte());
        adapter.setSolde(serviceEntity.getSolde());
        adapter.setTypeCompte("SERVICE");
        adapter.setVersion(serviceEntity.getVersion());

        log.debug("Adapter créé pour CompteServiceEntity ID={}", serviceEntity.getId());
        return adapter;
    }

    private CompteCollecteur createAdapterFromCompte(Compte compte, Collecteur collecteur, String typeCompte) {
        CompteCollecteur adapter = new CompteCollecteur();
        adapter.setId(compte.getId());
        adapter.setCollecteur(collecteur);
        adapter.setNomCompte(compte.getNomCompte());
        adapter.setNumeroCompte(compte.getNumeroCompte());
        adapter.setSolde(compte.getSolde());
        adapter.setTypeCompte(typeCompte);
        adapter.setVersion(compte.getVersion());

        log.debug("Adapter créé pour {} ID={}", typeCompte, compte.getId());
        return adapter;
    }
}