package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteCollecteur;
import org.example.collectfocep.exceptions.CompteGenerationException;
import org.example.collectfocep.repositories.CompteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollecteurAccountService {

    private final CompteAdapterService compteAdapterService;  // ✅ UTILISER LE SERVICE ADAPTER
    private final CompteRepository compteRepository;

    @Transactional
    public CompteCollecteur ensureCompteAttenteExists(Collecteur collecteur) {
        try {
            // ✅ UTILISER LE SERVICE ADAPTER
            CompteCollecteur existing = compteAdapterService
                    .findByCollecteurAndTypeCompte(collecteur, "ATTENTE");

            log.debug("Compte d'attente existant trouvé pour collecteur {}", collecteur.getId());
            return existing;

        } catch (Exception e) {
            // Si pas trouvé, il faudrait créer un nouveau compte via CompteService
            log.warn("Compte attente non trouvé pour collecteur {}: {}", collecteur.getId(), e.getMessage());
            throw e; // Pour l'instant, on propage l'erreur
        }
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