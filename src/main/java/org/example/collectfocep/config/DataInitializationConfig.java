package org.example.collectfocep.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Admin;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.repositories.AdminRepository;
import org.example.collectfocep.repositories.AgenceRepository;
import org.example.collectfocep.security.config.RoleConfig;
import org.example.collectfocep.util.CompteUtility;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializationConfig {

    private final AgenceRepository agenceRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompteUtility compteUtility;

    @Bean
    @Profile("init")
    public CommandLineRunner initializeData() {
        return args -> {
            log.info("Initialisation des données de base...");

            // Initialiser les comptes système en premier
            try {
                log.info("Initialisation des comptes système...");
                compteUtility.ensureSystemAccountsExist();
                log.info("Comptes système initialisés avec succès");
            } catch (Exception e) {
                log.error("Erreur lors de l'initialisation des comptes système: {}", e.getMessage());
                // Ne pas bloquer le démarrage de l'application
            }

            // Vérifier si des agences existent déjà
            if (agenceRepository.count() == 0) {
                log.info("Création d'une agence par défaut");

                Agence agence = new Agence();
                agence.setCodeAgence("A01");
                agence.setNomAgence("Agence Principale");

                // Sauvegarder l'agence
                agence = agenceRepository.save(agence);

                // Vérifier si des admins existent déjà
                if (adminRepository.count() == 0) {
                    log.info("Création d'un admin par défaut");

                    Admin admin = Admin.builder()
                            .nom("Admin")
                            .prenom("System")
                            .adresseMail("admin@collectfocep.com")
                            .password(passwordEncoder.encode("admin123"))
                            .numeroCni("1234567890123")
                            .telephone("123456789")
                            .role(RoleConfig.ADMIN)
                            .agence(agence)
                            .build();

                    adminRepository.save(admin);
                }
            }

            log.info("Initialisation terminée !");
        };
    }
}