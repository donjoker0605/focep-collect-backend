package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.impl.MouvementServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrateur principal du processus de commission selon spécification FOCEP
 * 
 * Processus complet :
 * 1. Calcul "x" pour chaque client avec hiérarchie (client → collecteur → agence)
 * 2. TVA 19,25% sur chaque "x" 
 * 3. Mouvements : Débit client → Crédit C.P.C.C et C.P.T
 * 4. Stockage "S" = sum("x") par collecteur
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionOrchestrator {

    private final CommissionCalculatorService calculatorService;
    private final CompteSpecialiseService compteSpecialiseService;
    private final MouvementServiceImpl mouvementService;
    private final CommissionParameterRepository parameterRepository;
    private final ClientRepository clientRepository;
    private final CollecteurRepository collecteurRepository;

    /**
     * Lance le calcul de commission complet pour un collecteur sur une période
     */
    @Transactional
    public CommissionResult processCommissions(Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {
        log.info("Début calcul commission - Collecteur: {}, Période: {} → {}", 
                collecteurId, dateDebut, dateFin);

        try {
            // 1. Récupération des données
            Collecteur collecteur = getCollecteur(collecteurId);
            Long agenceId = collecteur.getAgence().getId();
            List<Client> clients = clientRepository.findByCollecteurId(collecteurId);

            // 2. Calcul des commissions par client
            List<CommissionClientDetail> commissionsClients = new ArrayList<>();
            BigDecimal totalCommissions = BigDecimal.ZERO;
            BigDecimal totalTVA = BigDecimal.ZERO;

            for (Client client : clients) {
                CommissionClientDetail detail = calculateClientCommission(client, dateDebut, dateFin);
                if (detail != null && detail.getCommissionX().compareTo(BigDecimal.ZERO) > 0) {
                    commissionsClients.add(detail);
                    totalCommissions = totalCommissions.add(detail.getCommissionX());
                    totalTVA = totalTVA.add(detail.getTva());
                }
            }

            log.info("Commissions calculées - {} clients, Total: {}, TVA: {}", 
                    commissionsClients.size(), totalCommissions, totalTVA);

            // 3. Exécution des mouvements comptables
            executeCommissionMovements(agenceId, commissionsClients);

            // 4. Construction du résultat
            return CommissionResult.builder()
                    .collecteurId(collecteurId)
                    .agenceId(agenceId)
                    .periode(String.format("%s → %s", dateDebut, dateFin))
                    .commissionsClients(commissionsClients)
                    .montantSCollecteur(totalCommissions) // S du collecteur
                    .totalTVA(totalTVA)
                    .dateCalcul(LocalDateTime.now())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du calcul de commission pour collecteur {}: {}", 
                    collecteurId, e.getMessage(), e);
            return CommissionResult.failure(collecteurId, e.getMessage());
        }
    }

    /**
     * Calcule la commission "x" d'un client selon la hiérarchie des paramètres
     */
    private CommissionClientDetail calculateClientCommission(Client client, LocalDate dateDebut, LocalDate dateFin) {
        log.debug("Calcul commission client: {}", client.getNom());

        // 1. Récupération du montant total épargné sur la période
        BigDecimal montantTotal = calculateMontantEpargne(client.getId(), dateDebut, dateFin);
        
        if (montantTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Pas d'épargne pour client {}", client.getNom());
            return null;
        }

        // 2. Récupération des paramètres selon hiérarchie
        CommissionParameter parameter = getCommissionParameterHierarchy(client);
        
        if (parameter == null) {
            log.warn("Aucun paramètre de commission trouvé pour client {}", client.getNom());
            return null;
        }

        // 3. Calcul de la commission "x"
        BigDecimal commissionX = calculatorService.calculateCommission(montantTotal, parameter);
        BigDecimal tva = calculatorService.calculateTVA(commissionX);
        BigDecimal soldeNet = calculatorService.calculateSoldeNet(
                BigDecimal.valueOf(client.getSolde()), commissionX, tva);

        return CommissionClientDetail.builder()
                .clientId(client.getId())
                .clientNom(client.getNom())
                .montantEpargne(montantTotal)
                .commissionX(commissionX)
                .tva(tva)
                .ancienSolde(BigDecimal.valueOf(client.getSolde()))
                .nouveauSolde(soldeNet)
                .parameterUsed(parameter.getType().name())
                .build();
    }

    /**
     * Récupère les paramètres de commission selon la hiérarchie FOCEP :
     * 1. Client (priorité haute)
     * 2. Collecteur (priorité moyenne) 
     * 3. Agence (priorité basse)
     */
    private CommissionParameter getCommissionParameterHierarchy(Client client) {
        // 1. Paramètres du client
        CommissionParameter clientParam = parameterRepository.findByClientId(client.getId()).orElse(null);
        if (clientParam != null) {
            log.debug("Utilisation paramètres CLIENT pour {}", client.getNom());
            return clientParam;
        }

        // 2. Paramètres du collecteur
        CommissionParameter collecteurParam = parameterRepository.findByCollecteurId(client.getCollecteur().getId()).orElse(null);
        if (collecteurParam != null) {
            log.debug("Utilisation paramètres COLLECTEUR pour {}", client.getNom());
            return collecteurParam;
        }

        // 3. Paramètres de l'agence
        CommissionParameter agenceParam = parameterRepository.findByAgenceId(client.getCollecteur().getAgence().getId()).orElse(null);
        if (agenceParam != null) {
            log.debug("Utilisation paramètres AGENCE pour {}", client.getNom());
            return agenceParam;
        }

        return null;
    }

    /**
     * Exécute les mouvements comptables pour toutes les commissions
     */
    private void executeCommissionMovements(Long agenceId, List<CommissionClientDetail> commissionsClients) {
        log.info("Exécution mouvements comptables - {} clients", commissionsClients.size());

        // Récupération des comptes de passage
        ComptePassageCommissionCollecte comptePCCC = compteSpecialiseService.getOrCreateCPCC(agenceId);
        ComptePassageTaxe comptePT = compteSpecialiseService.getOrCreateCPT(agenceId);

        for (CommissionClientDetail detail : commissionsClients) {
            // Récupération du compte client
            CompteClient compteClient = getCompteClient(detail.getClientId());

            // Mouvement 1 : Débit Client → Crédit C.P.C.C (commission "x")
            if (detail.getCommissionX().compareTo(BigDecimal.ZERO) > 0) {
                Mouvement mvtCommission = createMouvement(
                        compteClient, comptePCCC, detail.getCommissionX(),
                        String.format("Commission collecte - Client %s", detail.getClientNom())
                );
                mouvementService.effectuerMouvement(mvtCommission);
            }

            // Mouvement 2 : Débit Client → Crédit C.P.T (TVA sur "x")
            if (detail.getTva().compareTo(BigDecimal.ZERO) > 0) {
                Mouvement mvtTVA = createMouvement(
                        compteClient, comptePT, detail.getTva(),
                        String.format("TVA commission (19,25%%) - Client %s", detail.getClientNom())
                );
                mouvementService.effectuerMouvement(mvtTVA);
            }

            log.debug("Mouvements exécutés - Client: {}, Commission: {}, TVA: {}", 
                    detail.getClientNom(), detail.getCommissionX(), detail.getTva());
        }
    }

    private BigDecimal calculateMontantEpargne(Long clientId, LocalDate dateDebut, LocalDate dateFin) {
        // TODO: Implémenter le calcul réel basé sur les mouvements d'épargne
        // Ceci est un placeholder - tu devras adapter selon ta structure
        return BigDecimal.valueOf(100000); // Temporaire pour les tests
    }

    private Collecteur getCollecteur(Long collecteurId) {
        return collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new RuntimeException("Collecteur non trouvé: " + collecteurId));
    }

    private CompteClient getCompteClient(Long clientId) {
        // TODO: Récupération du compte client réel
        // Placeholder temporaire
        CompteClient compte = new CompteClient();
        compte.setId(clientId);
        return compte;
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

    // Classes internes pour les résultats
    
    @lombok.Builder
    @lombok.Getter
    public static class CommissionResult {
        private Long collecteurId;
        private Long agenceId;
        private String periode;
        private List<CommissionClientDetail> commissionsClients;
        private BigDecimal montantSCollecteur;
        private BigDecimal totalTVA;
        private LocalDateTime dateCalcul;
        private boolean success;
        private String errorMessage;

        public static CommissionResult failure(Long collecteurId, String errorMessage) {
            return CommissionResult.builder()
                    .collecteurId(collecteurId)
                    .success(false)
                    .errorMessage(errorMessage)
                    .commissionsClients(new ArrayList<>())
                    .build();
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class CommissionClientDetail {
        private Long clientId;
        private String clientNom;
        private BigDecimal montantEpargne;
        private BigDecimal commissionX;
        private BigDecimal tva;
        private BigDecimal ancienSolde;
        private BigDecimal nouveauSolde;
        private String parameterUsed;
    }
}