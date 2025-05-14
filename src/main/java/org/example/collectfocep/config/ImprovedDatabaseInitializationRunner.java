package org.example.collectfocep.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.example.collectfocep.services.impl.ImprovedSystemAccountService;

@Component
@Slf4j
@Profile("init")
@Order(1)
public class ImprovedDatabaseInitializationRunner implements CommandLineRunner {

    @Autowired
    private ImprovedSystemAccountService systemAccountService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== DÉBUT DE L'INITIALISATION AMÉLIORÉE DE LA BASE DE DONNÉES ===");

        try {
            // Nettoyer et initialiser les comptes système
            systemAccountService.resetSystemAccounts();

            log.info("=== INITIALISATION DE LA BASE DE DONNÉES TERMINÉE AVEC SUCCÈS ===");
        } catch (Exception e) {
            log.error("=== ERREUR LORS DE L'INITIALISATION DE LA BASE DE DONNÉES ===", e);
            // Ne pas faire échouer l'application, juste logguer l'erreur
            log.warn("L'application va continuer malgré l'erreur d'initialisation");
        }
    }
}