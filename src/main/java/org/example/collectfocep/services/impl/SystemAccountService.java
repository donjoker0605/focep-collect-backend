package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.CompteSysteme;
import org.example.collectfocep.repositories.CompteSystemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemAccountService {

    private final CompteSystemeRepository compteSystemeRepository;

    @Transactional
    public void ensureSystemAccountsExist() {
        log.info("Vérification et création des comptes système...");

        ensureSystemCompteExists("TAXE", "Compte TVA", "TAXE-SYS");
        ensureSystemCompteExists("PRODUIT", "Compte Produit FOCEP", "PROD-SYS");
        ensureSystemCompteExists("ATTENTE", "Compte Attente Système", "ATT-SYS");

        log.info("Comptes système initialisés avec succès");
    }

    @Transactional
    public CompteSysteme ensureSystemCompteExists(String typeCompte, String nomCompte, String numeroCompte) {
        Optional<CompteSysteme> existingOpt = compteSystemeRepository.findByTypeCompte(typeCompte);

        if (existingOpt.isPresent()) {
            log.debug("Compte système existant trouvé: {}", typeCompte);
            return existingOpt.get();
        }

        log.info("Création du compte système: {}", typeCompte);
        CompteSysteme compte = CompteSysteme.builder()
                .typeCompte(typeCompte)
                .nomCompte(nomCompte)
                .numeroCompte(numeroCompte)
                .solde(0.0)
                .build();

        return compteSystemeRepository.save(compte);
    }
}