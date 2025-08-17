package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 🏦 Service pour la gestion des comptes agence
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompteAgenceService {

    private final CompteAgenceRepository compteAgenceRepository;
    private final CompteRepository compteRepository;

    /**
     * 🔧 Assure l'existence d'un compte agence pour une agence donnée
     */
    @Transactional
    public CompteAgence ensureCompteAgenceExists(Agence agence) {
        log.debug("🔧 Vérification existence compte agence pour: {}", agence.getNomAgence());

        return compteAgenceRepository.findByAgence(agence)
                .orElseGet(() -> {
                    log.info("✨ Création nouveau compte agence pour: {}", agence.getNomAgence());
                    CompteAgence nouveauCompte = CompteAgence.createForAgence(agence);
                    return compteAgenceRepository.save(nouveauCompte);
                });
    }

    /**
     * 📊 Récupère le compte agence par ID d'agence
     */
    public CompteAgence getCompteAgenceByAgenceId(Long agenceId) {
        return compteAgenceRepository.findByAgenceId(agenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte agence non trouvé pour l'agence ID: " + agenceId));
    }

    /**
     * 💰 Débite le compte agence (augmente la dette selon logique métier)
     */
    @Transactional
    public void debiterCompteAgence(CompteAgence compteAgence, Double montant) {
        log.info("💰 Débit compte agence {} de {} FCFA", compteAgence.getId(), montant);

        // Selon la logique utilisateur, débiter rend le compte plus négatif
        double nouveauSolde = compteAgence.getSolde() - montant;
        compteAgence.setSolde(nouveauSolde);
        compteAgenceRepository.save(compteAgence);

        log.debug("✅ Nouveau solde compte agence: {} FCFA", nouveauSolde);
    }

    /**
     * 📈 Crédite le compte agence (réduit la dette selon logique métier)
     */
    @Transactional
    public void crediterCompteAgence(CompteAgence compteAgence, Double montant) {
        log.info("📈 Crédit compte agence {} de {} FCFA", compteAgence.getId(), montant);

        double nouveauSolde = compteAgence.getSolde() + montant;
        compteAgence.setSolde(nouveauSolde);
        compteAgenceRepository.save(compteAgence);

        log.debug("✅ Nouveau solde compte agence: {} FCFA", nouveauSolde);
    }

    /**
     * 🏥 Diagnostic de santé des comptes agence
     */
    public void diagnosticComptesAgence() {
        log.info("🏥 Diagnostic des comptes agence...");

        List<CompteAgence> comptesPositifs = compteAgenceRepository.findComptesAgenceAvecSoldePositif();
        List<CompteAgence> comptesNegatifs = compteAgenceRepository.findComptesAgenceAvecSoldeNegatif();

        log.info("📊 Comptes agence avec solde POSITIF (anormal): {}", comptesPositifs.size());
        log.info("📊 Comptes agence avec solde NÉGATIF (normal): {}", comptesNegatifs.size());

        if (!comptesPositifs.isEmpty()) {
            log.warn("⚠️ ATTENTION: {} comptes agence ont un solde positif (anormal selon logique métier)",
                    comptesPositifs.size());
            comptesPositifs.forEach(compte ->
                    log.warn("   - Agence: {}, Solde: {} FCFA",
                            compte.getAgence().getNomAgence(), compte.getSolde())
            );
        }

        Double totalVerse = compteAgenceRepository.calculateTotalFondsVersesAgences();
        log.info("💰 Total fonds versés aux agences: {} FCFA", totalVerse);
    }

    /**
     * 📋 Statistiques des versements par agence
     */
    public List<Object[]> getStatistiquesVersements() {
        return compteAgenceRepository.getStatistiquesVersementsParAgence();
    }

    /**
     * ✨ CRÉER AUTOMATIQUEMENT TOUS LES COMPTES D'UNE NOUVELLE AGENCE
     * 
     * Crée tous les comptes requis pour une agence :
     * 1. CompteAgence - Compte principal de l'agence
     * 2. CompteProduitCollecte - Part de la microfinance EMF
     * 3. CompteChargeCollecte - Charges lors de rémunération 
     * 4. ComptePassageCommissionCollecte - Commissions collectées
     * 5. ComptePassageTaxe - TVA (19,25%)
     */
    @Transactional
    public void createAllAgencyAccounts(Agence agence) {
        log.info("🏗️ Création de tous les comptes pour l'agence: {}", agence.getNomAgence());
        
        try {
            // 1. Créer le compte principal de l'agence
            createCompteAgence(agence);
            
            // 2. Créer le compte produit collecte
            createCompteProduitCollecte(agence);
            
            // 3. Créer le compte charge collecte
            createCompteChargeCollecte(agence);
            
            // 4. Créer le compte passage commission collecte
            createComptePassageCommissionCollecte(agence);
            
            // 5. Créer le compte passage taxe
            createComptePassageTaxe(agence);
            
            log.info("✅ Tous les comptes de l'agence {} ont été créés avec succès", agence.getNomAgence());
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création des comptes agence: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la création des comptes agence: " + e.getMessage(), e);
        }
    }

    /**
     * 🏦 Créer le compte principal de l'agence
     */
    @Transactional
    public CompteAgence createCompteAgence(Agence agence) {
        log.debug("🏦 Création CompteAgence pour: {}", agence.getNomAgence());
        
        // Vérifier si le compte existe déjà
        if (compteAgenceRepository.findByAgence(agence).isPresent()) {
            log.warn("⚠️ CompteAgence existe déjà pour l'agence: {}", agence.getNomAgence());
            return compteAgenceRepository.findByAgence(agence).get();
        }
        
        CompteAgence compteAgence = CompteAgence.createForAgence(agence);
        return compteAgenceRepository.save(compteAgence);
    }

    /**
     * 💰 Créer le compte produit collecte
     */
    @Transactional
    public CompteProduitCollecte createCompteProduitCollecte(Agence agence) {
        log.debug("💰 Création CompteProduitCollecte pour: {}", agence.getNomAgence());
        
        CompteProduitCollecte compte = CompteProduitCollecte.builder()
                .agence(agence)
                .typeCompte("PRODUIT_COLLECTE")
                .nomCompte("Compte Produit Collecte - " + agence.getNomAgence())
                .numeroCompte("CPC-" + agence.getCodeAgence() + "-" + System.currentTimeMillis())
                .solde(0.0)
                .version(0L)
                .build();
                
        return (CompteProduitCollecte) compteRepository.save(compte);
    }

    /**
     * 💸 Créer le compte charge collecte
     */
    @Transactional
    public CompteChargeCollecte createCompteChargeCollecte(Agence agence) {
        log.debug("💸 Création CompteChargeCollecte pour: {}", agence.getNomAgence());
        
        CompteChargeCollecte compte = CompteChargeCollecte.builder()
                .agence(agence)
                .typeCompte("CHARGE_COLLECTE")
                .nomCompte("Compte Charge Collecte - " + agence.getNomAgence())
                .numeroCompte("CCC-" + agence.getCodeAgence() + "-" + System.currentTimeMillis())
                .solde(0.0)
                .version(0L)
                .build();
                
        return (CompteChargeCollecte) compteRepository.save(compte);
    }

    /**
     * 🔄 Créer le compte passage commission collecte
     */
    @Transactional
    public ComptePassageCommissionCollecte createComptePassageCommissionCollecte(Agence agence) {
        log.debug("🔄 Création ComptePassageCommissionCollecte pour: {}", agence.getNomAgence());
        
        ComptePassageCommissionCollecte compte = ComptePassageCommissionCollecte.builder()
                .agence(agence)
                .typeCompte("PASSAGE_COMMISSION_COLLECTE")
                .nomCompte("Compte Passage Commission Collecte - " + agence.getNomAgence())
                .numeroCompte("CPCC-" + agence.getCodeAgence() + "-" + System.currentTimeMillis())
                .solde(0.0)
                .version(0L)
                .build();
                
        return (ComptePassageCommissionCollecte) compteRepository.save(compte);
    }

    /**
     * 🧾 Créer le compte passage taxe
     */
    @Transactional
    public ComptePassageTaxe createComptePassageTaxe(Agence agence) {
        log.debug("🧾 Création ComptePassageTaxe pour: {}", agence.getNomAgence());
        
        ComptePassageTaxe compte = ComptePassageTaxe.builder()
                .agence(agence)
                .typeCompte("PASSAGE_TAXE")
                .nomCompte("Compte Passage Taxe - " + agence.getNomAgence())
                .numeroCompte("CPT-" + agence.getCodeAgence() + "-" + System.currentTimeMillis())
                .solde(0.0)
                .tauxTVA(0.1925) // 19,25% par défaut
                .version(0L)
                .build();
                
        return (ComptePassageTaxe) compteRepository.save(compte);
    }

    /**
     * 🔍 Vérifier si tous les comptes d'une agence existent
     */
    public boolean hasAllRequiredAccounts(Agence agence) {
        try {
            // Vérifier CompteAgence
            boolean hasCompteAgence = compteAgenceRepository.findByAgence(agence).isPresent();
            
            // Vérifier autres comptes par type et agence
            boolean hasCompteProduit = compteRepository.existsByTypeCompteAndAgenceId("PRODUIT_COLLECTE", agence.getId());
            boolean hasCompteCharge = compteRepository.existsByTypeCompteAndAgenceId("CHARGE_COLLECTE", agence.getId());
            boolean hasCompteCommission = compteRepository.existsByTypeCompteAndAgenceId("PASSAGE_COMMISSION_COLLECTE", agence.getId());
            boolean hasCompteTaxe = compteRepository.existsByTypeCompteAndAgenceId("PASSAGE_TAXE", agence.getId());
            
            return hasCompteAgence && hasCompteProduit && hasCompteCharge && hasCompteCommission && hasCompteTaxe;
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification des comptes agence: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 🔧 Assurer que tous les comptes requis existent pour une agence
     */
    @Transactional
    public void ensureAllRequiredAccountsExist(Agence agence) {
        log.info("🔧 Vérification et création des comptes manquants pour: {}", agence.getNomAgence());
        
        if (!hasAllRequiredAccounts(agence)) {
            log.info("📝 Création des comptes manquants pour l'agence: {}", agence.getNomAgence());
            createAllAgencyAccounts(agence);
        } else {
            log.debug("✅ Tous les comptes requis existent déjà pour l'agence: {}", agence.getNomAgence());
        }
    }
}