package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteCollecteur;
import org.example.collectfocep.exceptions.CompteGenerationException;
import org.example.collectfocep.repositories.CompteCollecteurRepository;
import org.example.collectfocep.repositories.CompteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollecteurAccountService {

    private final CompteCollecteurRepository compteCollecteurRepository;
    private final CompteRepository compteRepository;

    @Transactional
    public CompteCollecteur ensureCompteAttenteExists(Collecteur collecteur) {
        Optional<CompteCollecteur> existingOpt = compteCollecteurRepository
                .findByCollecteurAndTypeCompte(collecteur, "ATTENTE");

        if (existingOpt.isPresent()) {
            log.debug("Compte d'attente existant trouvé pour collecteur {}", collecteur.getId());
            return existingOpt.get();
        }

        String numeroCompte = generateUniqueAccountNumber("ATT", collecteur);

        CompteCollecteur compte = CompteCollecteur.builder()
                .collecteur(collecteur)
                .nomCompte("Compte Attente - " + collecteur.getNom())
                .numeroCompte(numeroCompte)
                .typeCompte("ATTENTE")
                .solde(0.0)
                .build();

        log.info("Création compte d'attente pour collecteur {}: {}",
                collecteur.getId(), numeroCompte);

        return compteCollecteurRepository.save(compte);
    }

    private String generateUniqueAccountNumber(String prefix, Collecteur collecteur) {
        String baseNumber = String.format("%s%s%d",
                prefix,
                collecteur.getAgence().getCodeAgence(),
                collecteur.getId());

        int attempt = 0;
        while (attempt < 100) {
            String candidateNumber = attempt == 0 ? baseNumber :
                    baseNumber + "-" + String.format("%03d",
                            ThreadLocalRandom.current().nextInt(100, 1000));

            if (!compteRepository.existsByNumeroCompte(candidateNumber)) {
                return candidateNumber;
            }
            attempt++;
        }

        throw new CompteGenerationException(
                "Impossible de générer un numéro unique pour " + prefix);
    }
}