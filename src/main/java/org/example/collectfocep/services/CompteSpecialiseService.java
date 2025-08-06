package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des comptes spécialisés selon la nomenclature FOCEP
 * Gère la création et récupération des comptes C.P.C.C, C.P.T, C.P.C, C.C.C, C.S.C, C.T
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CompteSpecialiseService {

    private final CompteRepository compteRepository;
    private final CompteAgenceRepository compteAgenceRepository;
    
    /**
     * Récupère ou crée le C.P.C.C (Compte Passage Commission Collecte) pour une agence
     */
    @Transactional
    public ComptePassageCommissionCollecte getOrCreateCPCC(Long agenceId) {
        log.debug("Récupération C.P.C.C pour agence: {}", agenceId);
        
        return compteRepository.findByAgenceIdAndTypeCompte(agenceId, "PASSAGE_COMMISSION_COLLECTE")
                .map(compte -> (ComptePassageCommissionCollecte) compte)
                .orElseGet(() -> createCPCC(agenceId));
    }

    /**
     * Récupère ou crée le C.P.T (Compte Passage Taxe) pour une agence  
     */
    @Transactional
    public ComptePassageTaxe getOrCreateCPT(Long agenceId) {
        log.debug("Récupération C.P.T pour agence: {}", agenceId);
        
        return compteRepository.findByAgenceIdAndTypeCompte(agenceId, "PASSAGE_TAXE")
                .map(compte -> (ComptePassageTaxe) compte)
                .orElseGet(() -> createCPT(agenceId));
    }

    /**
     * Récupère ou crée le C.P.C (Compte Produit Collecte) pour une agence
     */
    @Transactional  
    public CompteProduitCollecte getOrCreateCPC(Long agenceId) {
        log.debug("Récupération C.P.C pour agence: {}", agenceId);
        
        return compteRepository.findByAgenceIdAndTypeCompte(agenceId, "PRODUIT_COLLECTE")
                .map(compte -> (CompteProduitCollecte) compte)
                .orElseGet(() -> createCPC(agenceId));
    }

    /**
     * Récupère ou crée le C.C.C (Compte Charge Collecte) pour une agence
     */
    @Transactional
    public CompteChargeCollecte getOrCreateCCC(Long agenceId) {
        log.debug("Récupération C.C.C pour agence: {}", agenceId);
        
        return compteRepository.findByAgenceIdAndTypeCompte(agenceId, "CHARGE_COLLECTE") 
                .map(compte -> (CompteChargeCollecte) compte)
                .orElseGet(() -> createCCC(agenceId));
    }

    /**
     * Récupère ou crée le C.T (Compte Taxe) pour une agence
     */
    @Transactional
    public CompteTaxe getOrCreateCT(Long agenceId) {
        log.debug("Récupération C.T pour agence: {}", agenceId);
        
        return compteRepository.findByAgenceIdAndTypeCompte(agenceId, "TAXE")
                .map(compte -> (CompteTaxe) compte) 
                .orElseGet(() -> createCT(agenceId));
    }

    /**
     * Récupère le C.S.C (Compte Salaire Collecteur) pour un collecteur
     */
    @Transactional(readOnly = true)
    public CompteSalaireCollecteur getCSC(Long collecteurId) {
        log.debug("Récupération C.S.C pour collecteur: {}", collecteurId);
        
        return compteRepository.findByCollecteurIdAndTypeCompte(collecteurId, "SALAIRE_COLLECTEUR")
                .map(compte -> (CompteSalaireCollecteur) compte)
                .orElseThrow(() -> new RuntimeException("C.S.C non trouvé pour collecteur: " + collecteurId));
    }

    private ComptePassageCommissionCollecte createCPCC(Long agenceId) {
        log.info("Création C.P.C.C pour agence: {}", agenceId);
        
        Agence agence = getAgenceById(agenceId);
        
        ComptePassageCommissionCollecte cpcc = ComptePassageCommissionCollecte.builder()
                .agence(agence)
                .numeroCompte(generateNumeroCompte("CPCC", agenceId))
                .solde(0.0)
                .build();
                
        return compteRepository.save(cpcc);
    }

    private ComptePassageTaxe createCPT(Long agenceId) {
        log.info("Création C.P.T pour agence: {}", agenceId);
        
        Agence agence = getAgenceById(agenceId);
        
        ComptePassageTaxe cpt = ComptePassageTaxe.builder()
                .agence(agence)
                .numeroCompte(generateNumeroCompte("CPT", agenceId))
                .solde(0.0)
                .build();
                
        return compteRepository.save(cpt);
    }

    private CompteProduitCollecte createCPC(Long agenceId) {
        log.info("Création C.P.C pour agence: {}", agenceId);
        
        Agence agence = getAgenceById(agenceId);
        
        CompteProduitCollecte cpc = CompteProduitCollecte.builder()
                .agence(agence)
                .numeroCompte(generateNumeroCompte("CPC", agenceId))
                .solde(0.0)
                .build();
                
        return compteRepository.save(cpc);
    }

    private CompteChargeCollecte createCCC(Long agenceId) {
        log.info("Création C.C.C pour agence: {}", agenceId);
        
        Agence agence = getAgenceById(agenceId);
        
        CompteChargeCollecte ccc = CompteChargeCollecte.builder()
                .agence(agence)
                .numeroCompte(generateNumeroCompte("CCC", agenceId))
                .solde(0.0)
                .build();
                
        return compteRepository.save(ccc);
    }

    private CompteTaxe createCT(Long agenceId) {
        log.info("Création C.T pour agence: {}", agenceId);
        
        Agence agence = getAgenceById(agenceId);
        
        CompteTaxe ct = CompteTaxe.builder()
                .agence(agence)
                .numeroCompte(generateNumeroCompte("CT", agenceId))
                .solde(0.0)
                .build();
                
        return compteRepository.save(ct);
    }

    private Agence getAgenceById(Long agenceId) {
        return compteAgenceRepository.findById(agenceId)
                .map(CompteAgence::getAgence)
                .orElseThrow(() -> new RuntimeException("Agence non trouvée: " + agenceId));
    }

    private String generateNumeroCompte(String prefix, Long agenceId) {
        return String.format("%s-%06d-%d", prefix, System.currentTimeMillis() % 1000000, agenceId);
    }
}