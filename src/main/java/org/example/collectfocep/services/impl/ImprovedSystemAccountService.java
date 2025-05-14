package org.example.collectfocep.services.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.CompteSysteme;
import org.example.collectfocep.repositories.CompteSystemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Profile("init")
public class ImprovedSystemAccountService {

    @Autowired
    private CompteSystemeRepository compteSystemeRepository;

    private final String[][] compteSystemes = {
            {"TAXE", "TAXE-SYS", "Compte TVA système"},
            {"EMF", "EMF-SYS", "Compte EMF système"},
            {"COMMISSION", "COMM-SYS", "Compte Commission système"},
            {"PRODUIT", "PROD-SYS", "Compte Produit système"}
    };

    @PostConstruct
    @Transactional
    public void initializeSystemAccounts() {
        log.info("Initialisation améliorée des comptes système...");

        for (String[] config : compteSystemes) {
            String typeCompte = config[0];
            String numeroCompte = config[1];
            String nomCompte = config[2];

            try {
                // Vérifier si le compte existe déjà
                if (!compteSystemeRepository.existsByNumeroCompte(numeroCompte)) {
                    CompteSysteme compte = CompteSysteme.create(typeCompte, nomCompte, numeroCompte);
                    compteSystemeRepository.save(compte);
                    log.info("Compte système créé: {} ({})", typeCompte, numeroCompte);
                } else {
                    log.info("Compte système déjà existant: {} ({})", typeCompte, numeroCompte);
                }
            } catch (Exception e) {
                log.error("Erreur lors de la création du compte {}: {}", typeCompte, e.getMessage());
                // Au lieu de propager l'erreur, on la log et on continue
                continue;
            }
        }

        log.info("Initialisation des comptes système terminée");
    }

    @Transactional
    public void resetSystemAccounts() {
        log.info("Réinitialisation des comptes système...");

        // Supprimer tous les comptes système existants
        for (String[] config : compteSystemes) {
            String numeroCompte = config[1];
            compteSystemeRepository.findByNumeroCompte(numeroCompte)
                    .ifPresent(compte -> {
                        compteSystemeRepository.delete(compte);
                        log.info("Compte système supprimé: {}", numeroCompte);
                    });
        }

        // Recréer les comptes
        initializeSystemAccounts();
    }
}