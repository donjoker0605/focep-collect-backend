package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CompteSystemeDTO;
import org.example.collectfocep.entities.CompteSysteme;
import org.example.collectfocep.repositories.CompteSystemeRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemAccountInitializationService {

    private final CompteSystemeRepository compteSystemeRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeSystemAccounts() {
        log.info("Initialisation des comptes système...");

        List<CompteSystemeDTO> systemAccounts = getRequiredSystemAccounts();

        for (CompteSystemeDTO dto : systemAccounts) {
            ensureSystemAccountExists(dto);
        }

        log.info("Initialisation des comptes système terminée");
    }

    private List<CompteSystemeDTO> getRequiredSystemAccounts() {
        return List.of(
                createDTO("TAXE", "Compte TVA", "TAXE-SYS"),
                createDTO("PRODUIT", "Compte Produit FOCEP", "PROD-SYS"),
                createDTO("ATTENTE", "Compte Attente Système", "ATT-SYS")
        );
    }

    private CompteSystemeDTO createDTO(String type, String nom, String numero) {
        CompteSystemeDTO dto = new CompteSystemeDTO();
        dto.setTypeCompte(type);
        dto.setNomCompte(nom);
        dto.setNumeroCompte(numero);
        return dto;
    }

    @Transactional
    public CompteSysteme ensureSystemAccountExists(CompteSystemeDTO dto) {
        Optional<CompteSysteme> existingOpt = compteSystemeRepository
                .findByTypeCompte(dto.getTypeCompte());

        if (existingOpt.isPresent()) {
            log.debug("Compte système existant trouvé: {}", dto.getTypeCompte());
            return existingOpt.get();
        }

        log.info("Création du compte système: {}", dto.getTypeCompte());
        CompteSysteme compte = CompteSysteme.builder()
                .typeCompte(dto.getTypeCompte())
                .nomCompte(dto.getNomCompte())
                .numeroCompte(dto.getNumeroCompte())
                .solde(0.0)
                .build();

        return compteSystemeRepository.save(compte);
    }
}