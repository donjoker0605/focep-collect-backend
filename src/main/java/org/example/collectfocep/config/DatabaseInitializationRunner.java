package org.example.collectfocep.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.services.impl.SystemAccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("init") // Ne s'exécute que avec le profil init
@Order(1)
public class DatabaseInitializationRunner implements CommandLineRunner {

    private final SystemAccountService systemAccountService;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== DÉBUT DE L'INITIALISATION DE LA BASE DE DONNÉES ===");

        try {
            // UN SEUL APPEL à l'initialisation des comptes système
            systemAccountService.initializeSystemAccounts();

            log.info("=== INITIALISATION TERMINÉE AVEC SUCCÈS ===");

        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation de la base de données", e);
            throw e;
        }
    }
}

