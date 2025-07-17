package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.dto.CollecteurComptesDTO;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.CompteCollecteur;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.CompteCollecteurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CompteCollecteurService {

    private final CompteCollecteurRepository compteRepository;
    private final CollecteurRepository collecteurRepository;

    public CompteCollecteur getOrCreateCompte(Long collecteurId,
                                              CompteCollecteur.TypeCompteCollecteur type) {
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        return compteRepository.findByCollecteurAndTypeCompteCollecteur(collecteur, type)
                .orElseGet(() -> createCompte(collecteur, type));
    }

    @Transactional
    public void updateSolde(Long collecteurId,
                            CompteCollecteur.TypeCompteCollecteur type,
                            Double nouveauSolde) {
        CompteCollecteur compte = getOrCreateCompte(collecteurId, type);

        // Validation du solde négatif
        if (nouveauSolde < 0 && !compte.allowsNegativeBalance()) {
            throw new IllegalArgumentException(
                    "Solde négatif non autorisé pour le compte " + type.getLibelle()
            );
        }

        compte.setSolde(nouveauSolde);
        compteRepository.save(compte);

        log.info("✅ Solde mis à jour: {} {} = {} FCFA",
                compte.getCollecteur().getNom(), type.getLibelle(), nouveauSolde);
    }

    @Transactional
    public void transferer(Long collecteurId,
                           CompteCollecteur.TypeCompteCollecteur typeSource,
                           CompteCollecteur.TypeCompteCollecteur typeDestination,
                           Double montant,
                           String motif) {

        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        log.info("💰 Transfert: {} FCFA de {} vers {} pour collecteur {} - Motif: {}",
                montant, typeSource.getLibelle(), typeDestination.getLibelle(),
                collecteurId, motif);

        try {
            // Débiter le compte source
            CompteCollecteur compteSource = getOrCreateCompte(collecteurId, typeSource);
            Double nouveauSoldeSource = compteSource.getSolde() - montant;
            updateSolde(collecteurId, typeSource, nouveauSoldeSource);

            // Créditer le compte destination
            CompteCollecteur compteDestination = getOrCreateCompte(collecteurId, typeDestination);
            Double nouveauSoldeDestination = compteDestination.getSolde() + montant;
            updateSolde(collecteurId, typeDestination, nouveauSoldeDestination);

            log.info("✅ Transfert réussi");

        } catch (Exception e) {
            log.error("❌ Erreur lors du transfert: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors du transfert: " + e.getMessage(), e);
        }
    }

    public CollecteurComptesDTO getCollecteurComptes(Long collecteurId) {
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé"));

        List<CompteCollecteur> comptes = compteRepository.findByCollecteur(collecteur);

        // Mapper les comptes par type
        Map<CompteCollecteur.TypeCompteCollecteur, CompteCollecteur> comptesMap =
                comptes.stream().collect(Collectors.toMap(
                        CompteCollecteur::getTypeCompteCollecteur,
                        compte -> compte
                ));

        CompteCollecteur compteService = comptesMap.get(CompteCollecteur.TypeCompteCollecteur.SERVICE);
        CompteCollecteur compteManquant = comptesMap.get(CompteCollecteur.TypeCompteCollecteur.MANQUANT);
        CompteCollecteur compteAttente = comptesMap.get(CompteCollecteur.TypeCompteCollecteur.ATTENTE);
        CompteCollecteur compteRemuneration = comptesMap.get(CompteCollecteur.TypeCompteCollecteur.REMUNERATION);

        Double totalCreances = compteManquant != null ? compteManquant.getSolde() : 0.0;
        Double totalAvoirs = (compteAttente != null ? compteAttente.getSolde() : 0.0) +
                (compteRemuneration != null ? compteRemuneration.getSolde() : 0.0);

        return CollecteurComptesDTO.builder()
                .collecteurId(collecteurId)
                .collecteurNom(collecteur.getNom() + " " + collecteur.getPrenom())
                .compteServiceSolde(compteService != null ? compteService.getSolde() : 0.0)
                .compteManquantSolde(compteManquant != null ? compteManquant.getSolde() : 0.0)
                .compteAttenteSolde(compteAttente != null ? compteAttente.getSolde() : 0.0)
                .compteRemunerationSolde(compteRemuneration != null ? compteRemuneration.getSolde() : 0.0)
                .totalCreances(totalCreances)
                .totalAvoirs(totalAvoirs)
                .soldeNet(totalAvoirs - totalCreances)
                .build();
    }

    // INITIALISATION DES COMPTES LORS DE LA CRÉATION D'UN COLLECTEUR
    @Transactional
    public void initializeCollecteurAccounts(Collecteur collecteur) {
        log.info("🏗️ Initialisation des comptes pour collecteur: {}", collecteur.getId());

        String agenceCode = collecteur.getAgence().getCodeAgence();
        String collecteurCode = String.format("%08d", collecteur.getId());

        // Créer tous les types de comptes
        for (CompteCollecteur.TypeCompteCollecteur type : CompteCollecteur.TypeCompteCollecteur.values()) {
            createCompte(collecteur, type, agenceCode, collecteurCode);
        }

        log.info("✅ Comptes initialisés pour collecteur: {}", collecteur.getId());
    }

    private CompteCollecteur createCompte(Collecteur collecteur,
                                          CompteCollecteur.TypeCompteCollecteur type) {
        String agenceCode = collecteur.getAgence().getCodeAgence();
        String collecteurCode = String.format("%08d", collecteur.getId());
        return createCompte(collecteur, type, agenceCode, collecteurCode);
    }

    private CompteCollecteur createCompte(Collecteur collecteur,
                                          CompteCollecteur.TypeCompteCollecteur type,
                                          String agenceCode,
                                          String collecteurCode) {

        String prefix = getAccountPrefix(type);
        String numeroCompte = prefix + agenceCode + collecteurCode;
        String nomCompte = type.getLibelle() + " - " + collecteur.getNom();

        CompteCollecteur compte = CompteCollecteur.builder()
                .collecteur(collecteur)
                .typeCompteCollecteur(type)
                .nomCompte(nomCompte)
                .numeroCompte(numeroCompte)
                .solde(0.0)
                .build();

        return compteRepository.save(compte);
    }

    private String getAccountPrefix(CompteCollecteur.TypeCompteCollecteur type) {
        switch (type) {
            case SERVICE: return "SRV";
            case MANQUANT: return "MNQ";
            case ATTENTE: return "ATT";
            case REMUNERATION: return "REM";
            case CHARGE: return "CHG";
            default: return "UNK";
        }
    }
}