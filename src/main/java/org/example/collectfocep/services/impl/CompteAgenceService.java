package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.CompteAgence;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.CompteAgenceRepository;
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
}