package org.example.collectfocep.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteCollecteur;
import org.example.collectfocep.services.impl.CollecteurAccountService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompteUtility {

    private final CollecteurAccountService collecteurAccountService;

    /**
     * Assure qu'un compte d'attente existe pour un collecteur
     */
    public CompteCollecteur ensureCompteAttenteExists(Collecteur collecteur) {
        return collecteurAccountService.ensureCompteAttenteExists(collecteur);
    }
}