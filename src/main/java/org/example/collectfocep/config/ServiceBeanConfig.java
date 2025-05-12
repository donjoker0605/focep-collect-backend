package org.example.collectfocep.config;

import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.services.impl.CompteServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceBeanConfig {

    @Bean
    public CompteService compteService(CompteRepository compteRepository,
                                       CompteCollecteurRepository compteCollecteurRepository,
                                       CompteLiaisonRepository compteLiaisonRepository,
                                       CompteServiceRepository compteServiceRepository,
                                       CompteManquantRepository compteManquantRepository,
                                       CompteRemunerationRepository compteRemunerationRepository,
                                       CompteAttenteRepository compteAttenteRepository,
                                       CompteChargeRepository compteChargeRepository,
                                       CollecteurRepository collecteurRepository,
                                       CompteClientRepository compteClientRepository,
                                       ClientRepository clientRepository) {
        return new CompteServiceImpl(
                compteRepository,
                compteCollecteurRepository,
                compteLiaisonRepository,
                compteServiceRepository,
                compteManquantRepository,
                compteRemunerationRepository,
                compteAttenteRepository,
                compteChargeRepository,
                collecteurRepository,
                compteClientRepository,
                clientRepository
        );
    }
}