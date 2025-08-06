package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.RubriqueRemunerationRepository;
import org.example.collectfocep.services.impl.MouvementServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Processeur de rémunération selon la spécification FOCEP
 * Implémente la logique Vi vs S avec rubriques
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RemunerationProcessor {

    private final RubriqueRemunerationRepository rubriqueRepository;
    private final CompteSpecialiseService compteSpecialiseService;
    private final MouvementServiceImpl mouvementService;

    /**
     * Traite la rémunération complète d'un collecteur selon la spec FOCEP
     * 
     * @param collecteurId ID du collecteur
     * @param S Somme des commissions collectées du collecteur pour la période
     * @return Résultat détaillé de la rémunération
     */
    @Transactional
    public RemunerationResult processRemuneration(Long collecteurId, BigDecimal S) {
        log.info("Début rémunération collecteur {} - S initial: {}", collecteurId, S);

        if (S == null || S.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("S doit être positif ou nul");
        }

        Long agenceId = getAgenceIdByCollecteur(collecteurId);
        BigDecimal restantS = S;
        BigDecimal totalVi = BigDecimal.ZERO;
        List<Mouvement> mouvements = new ArrayList<>();

        // 1. Récupération des comptes
        ComptePassageCommissionCollecte comptePCCC = compteSpecialiseService.getOrCreateCPCC(agenceId);
        CompteChargeCollecte compteCCC = compteSpecialiseService.getOrCreateCCC(agenceId);
        CompteSalaireCollecteur compteCSC = compteSpecialiseService.getCSC(collecteurId);

        // 2. Récupération des rubriques actives pour ce collecteur
        List<RubriqueRemuneration> rubriques = rubriqueRepository.findActiveRubriquesByCollecteur(
                collecteurId, LocalDate.now());

        log.info("Rubriques trouvées: {}", rubriques.size());

        // 3. Traitement de chaque rubrique
        for (RubriqueRemuneration rubrique : rubriques) {
            BigDecimal Vi = rubrique.calculateVi(S); // Vi calculé sur S initial, pas restant
            totalVi = totalVi.add(Vi);

            log.info("Rubrique '{}' - Vi: {}, Restant S: {}", rubrique.getNom(), Vi, restantS);

            if (Vi.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Pas de mouvement si Vi = 0
            }

            // Logique selon spec FOCEP
            if (Vi.compareTo(restantS) <= 0) {
                // Cas 1: Vi <= S
                // Débit C.P.C.C → Crédit C.S.C
                Mouvement mvt = createMouvement(
                        comptePCCC, compteCSC, Vi,
                        String.format("Rémunération - %s (Vi <= S)", rubrique.getNom())
                );
                mouvements.add(mvt);
                mouvementService.effectuerMouvement(mvt);

                restantS = restantS.subtract(Vi);
                log.info("Vi <= S - Débit C.P.C.C: {}, Nouveau restant S: {}", Vi, restantS);
            } else {
                // Cas 2: Vi > S  
                // Débit C.P.C.C de restantS + Débit C.C.C de (Vi - restantS)
                if (restantS.compareTo(BigDecimal.ZERO) > 0) {
                    Mouvement mvt1 = createMouvement(
                            comptePCCC, compteCSC, restantS,
                            String.format("Rémunération - %s (part C.P.C.C)", rubrique.getNom())
                    );
                    mouvements.add(mvt1);
                    mouvementService.effectuerMouvement(mvt1);
                    log.info("Vi > S - Débit C.P.C.C: {}", restantS);
                }

                BigDecimal deficitVi = Vi.subtract(restantS);
                Mouvement mvt2 = createMouvement(
                        compteCCC, compteCSC, deficitVi,
                        String.format("Rémunération - %s (complément C.C.C)", rubrique.getNom())
                );
                mouvements.add(mvt2);
                mouvementService.effectuerMouvement(mvt2);

                log.info("Vi > S - Débit C.C.C: {}", deficitVi);
                restantS = BigDecimal.ZERO;
                break; // Plus de S disponible, arrêt du traitement
            }
        }

        // 4. Rémunération EMF si restant S > 0
        if (restantS.compareTo(BigDecimal.ZERO) > 0) {
            CompteProduitCollecte compteProduitCollecte = compteSpecialiseService.getOrCreateCPC(agenceId);

            Mouvement mvtEMF = createMouvement(
                    comptePCCC, compteProduitCollecte, restantS,
                    "Rémunération EMF - Surplus commissions"
            );
            mouvements.add(mvtEMF);
            mouvementService.effectuerMouvement(mvtEMF);

            log.info("Rémunération EMF: {}", restantS);
        }

        // 5. Traitement des taxes (TVA sur S initial)
        processTaxes(agenceId, S, mouvements);

        log.info("Rémunération terminée - Total Vi: {}, Surplus EMF: {}", totalVi, restantS);

        return RemunerationResult.builder()
                .collecteurId(collecteurId)
                .montantSInitial(S)
                .totalRubriqueVi(totalVi)
                .montantEMF(restantS)
                .mouvements(mouvements)
                .success(true)
                .build();
    }

    /**
     * Traite la TVA selon spec : 19,25% de S initial
     */
    private void processTaxes(Long agenceId, BigDecimal S, List<Mouvement> mouvements) {
        BigDecimal tauxTVA = BigDecimal.valueOf(0.1925); // 19,25%
        BigDecimal montantTVA = S.multiply(tauxTVA).setScale(2, java.math.RoundingMode.HALF_UP);

        if (montantTVA.compareTo(BigDecimal.ZERO) > 0) {
            ComptePassageTaxe comptePT = compteSpecialiseService.getOrCreateCPT(agenceId);
            CompteTaxe compteTaxe = compteSpecialiseService.getOrCreateCT(agenceId);

            Mouvement mvtTaxe = createMouvement(
                    comptePT, compteTaxe, montantTVA,
                    "TVA sur commission collecteur (19,25%)"
            );
            mouvements.add(mvtTaxe);
            mouvementService.effectuerMouvement(mvtTaxe);

            log.info("TVA traitée - Montant: {} ({}% de {})", montantTVA, 19.25, S);
        }
    }

    private Mouvement createMouvement(Compte source, Compte destination, BigDecimal montant, String libelle) {
        return Mouvement.builder()
                .compteSource(source)
                .compteDestination(destination)
                .montant(montant.doubleValue())
                .libelle(libelle)
                .sens("DEBIT")
                .dateOperation(LocalDateTime.now())
                .build();
    }

    private Long getAgenceIdByCollecteur(Long collecteurId) {
        // TODO: Implémenter la récupération réelle de l'agenceId
        // Ceci est un placeholder - tu devras adapter selon ta structure
        return 1L; // Temporaire
    }

    /**
     * Classe pour le résultat de la rémunération
     */
    @lombok.Builder
    @lombok.Getter
    public static class RemunerationResult {
        private Long collecteurId;
        private BigDecimal montantSInitial;
        private BigDecimal totalRubriqueVi;
        private BigDecimal montantEMF;
        private List<Mouvement> mouvements;
        private boolean success;
        private String errorMessage;

        public static RemunerationResult failure(Long collecteurId, String errorMessage) {
            return RemunerationResult.builder()
                    .collecteurId(collecteurId)
                    .success(false)
                    .errorMessage(errorMessage)
                    .mouvements(new ArrayList<>())
                    .build();
        }
    }
}