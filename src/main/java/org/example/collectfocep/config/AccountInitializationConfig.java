package org.example.collectfocep.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.services.impl.ClientAccountInitializationService;
import org.example.collectfocep.util.CompteUtility;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class AccountInitializationConfig implements ApplicationListener<ApplicationReadyEvent> {

    private final CompteUtility compteUtility;
    private final ClientAccountInitializationService clientAccountInitializationService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Initialisation des comptes au démarrage de l'application");

        try {
            // S'assurer que les comptes système existent
            compteUtility.ensureSystemAccountsExist();

            // Créer les comptes clients manquants
            clientAccountInitializationService.createAccountsForAllClientsWithoutAccounts();

            log.info("Initialisation des comptes terminée avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des comptes: {}", e.getMessage(), e);
            // Ne pas bloquer le démarrage de l'application, juste logger l'erreur
        }
    }
}