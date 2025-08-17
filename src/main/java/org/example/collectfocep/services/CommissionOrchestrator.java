package org.example.collectfocep.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.collectfocep.services.CommissionCalculatorService;
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
    private final MouvementRepository mouvementRepository;
    private final CompteClientRepository compteClientRepository;
    
    // 🔥 NOUVEAU: Repository pour l'historique des calculs
    private final HistoriqueCalculCommissionRepository historiqueRepository;

    /**
     * 🔥 NOUVELLE VERSION: Lance le calcul de commission avec protection anti-doublon
     * 
     * Exigence métier: Un collecteur ne peut avoir qu'un seul calcul par période
     */
    @Transactional
    public CommissionResult processCommissions(Long collecteurId, LocalDate dateDebut, LocalDate dateFin) {
        log.info("🔥 Début calcul commission avec anti-doublon - Collecteur: {}, Période: {} → {}", 
                collecteurId, dateDebut, dateFin);

        // 🔥 ÉTAPE 1: Vérification anti-doublon
        if (historiqueRepository.existsCalculForPeriod(collecteurId, dateDebut, dateFin)) {
            log.warn("❌ DOUBLON DÉTECTÉ: Calcul déjà effectué pour collecteur {} sur période {} → {}", 
                    collecteurId, dateDebut, dateFin);
            
            // Récupération du calcul existant
            HistoriqueCalculCommission calculExistant = historiqueRepository
                    .findByCollecteurAndPeriod(collecteurId, dateDebut, dateFin)
                    .orElseThrow(() -> new RuntimeException("Calcul existant introuvable"));
            
            return CommissionResult.builder()
                    .collecteurId(collecteurId)
                    .agenceId(calculExistant.getAgenceId())
                    .periode(calculExistant.getPeriodeDescription())
                    .commissionsClients(new ArrayList<>()) // Vide car déjà calculé
                    .montantSCollecteur(calculExistant.getMontantCommissionTotal())
                    .totalTVA(calculExistant.getMontantTvaTotal())
                    .dateCalcul(calculExistant.getDateCalcul())
                    .success(true)
                    .message("⚠️ Calcul déjà effectué le " + calculExistant.getDateCalcul())
                    .build();
        }

        try {
            // 1. Récupération des données
            Collecteur collecteur = getCollecteur(collecteurId);
            Long agenceId = collecteur.getAgence().getId();
            List<Client> clients = clientRepository.findByCollecteurId(collecteurId);

            // 2. 🔥 OPTIMISATION N+1: Récupération groupée des données
            List<Long> clientIds = clients.stream().map(Client::getId).toList();
            
            // Récupération groupée des montants d'épargne
            Map<Long, BigDecimal> epargnesParClient = getEpargnesGroupees(clientIds, dateDebut, dateFin);
            
            // Récupération groupée des paramètres de commission
            Map<Long, CommissionParameter> parametresParClient = getParametresGroupes(clients, collecteurId, agenceId);

            // 3. Calcul des commissions par client (optimisé)
            List<CommissionClientDetail> commissionsClients = new ArrayList<>();
            BigDecimal totalCommissions = BigDecimal.ZERO;
            BigDecimal totalTVA = BigDecimal.ZERO;

            for (Client client : clients) {
                CommissionClientDetail detail = calculateClientCommissionOptimized(
                    client, epargnesParClient.get(client.getId()), 
                    parametresParClient.get(client.getId()));
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

            // 🔥 ÉTAPE 4: Sauvegarde dans l'historique (anti-doublon)
            LocalDateTime maintenant = LocalDateTime.now();
            HistoriqueCalculCommission historique = HistoriqueCalculCommission.builder()
                    .collecteur(collecteur)
                    .dateDebut(dateDebut)
                    .dateFin(dateFin)
                    .montantCommissionTotal(totalCommissions)
                    .montantTvaTotal(totalTVA)
                    .nombreClients(commissionsClients.size())
                    .statut(HistoriqueCalculCommission.StatutCalcul.CALCULE)
                    .detailsCalcul(serializeCommissionDetails(commissionsClients))
                    .agenceId(agenceId)
                    .remunere(false)
                    .build();
            
            HistoriqueCalculCommission historiqueSauve = historiqueRepository.save(historique);
            log.info("✅ Historique de calcul sauvegardé - ID: {}", historiqueSauve.getId());

            // 5. Construction du résultat
            return CommissionResult.builder()
                    .collecteurId(collecteurId)
                    .agenceId(agenceId)
                    .periode(String.format("%s → %s", dateDebut, dateFin))
                    .commissionsClients(commissionsClients)
                    .montantSCollecteur(totalCommissions) // S du collecteur
                    .totalTVA(totalTVA)
                    .dateCalcul(maintenant)
                    .success(true)
                    .historiqueId(historiqueSauve.getId()) // 🔥 NOUVEAU: ID de l'historique
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
        log.debug("Calcul montant épargne - Client: {}, Période: {} à {}", 
                clientId, dateDebut, dateFin);
        
        LocalDateTime startDateTime = dateDebut.atStartOfDay();
        LocalDateTime endDateTime = dateFin.atTime(23, 59, 59);
        
        // Somme des mouvements d'épargne (CREDIT) pour le client sur la période
        Double totalEpargne = mouvementRepository.sumAmountByClientAndPeriod(
                clientId, startDateTime, endDateTime);
        
        if (totalEpargne == null) {
            totalEpargne = 0.0;
        }
        
        BigDecimal result = BigDecimal.valueOf(totalEpargne);
        log.debug("Montant épargne calculé - Client {}: {}", clientId, result);
        
        return result;
    }

    private Collecteur getCollecteur(Long collecteurId) {
        return collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new RuntimeException("Collecteur non trouvé: " + collecteurId));
    }

    private CompteClient getCompteClient(Long clientId) {
        log.debug("Récupération compte client: {}", clientId);
        
        // Récupérer le client d'abord
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé: " + clientId));
        
        return compteClientRepository.findByClient(client)
                .orElseThrow(() -> new RuntimeException(
                        "Compte client non trouvé pour client: " + clientId));
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

    // 🔥 NOUVELLES MÉTHODES OPTIMISÉES POUR RÉSOUDRE N+1

    /**
     * Sérialise les détails de commission en JSON pour stockage
     */
    private String serializeCommissionDetails(List<CommissionClientDetail> commissionsClients) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(commissionsClients);
        } catch (Exception e) {
            log.warn("Erreur lors de la sérialisation des détails de commission: {}", e.getMessage());
            return "[]"; // JSON vide en cas d'erreur
        }
    }

    /**
     * Récupère les montants d'épargne pour tous les clients en une seule requête
     */
    private Map<Long, BigDecimal> getEpargnesGroupees(List<Long> clientIds, LocalDate dateDebut, LocalDate dateFin) {
        LocalDateTime startDateTime = dateDebut.atStartOfDay();
        LocalDateTime endDateTime = dateFin.atTime(23, 59, 59);
        
        Map<Long, BigDecimal> resultMap = new HashMap<>();
        
        // Initialiser avec 0 pour tous les clients
        clientIds.forEach(id -> resultMap.put(id, BigDecimal.ZERO));
        
        // Une seule requête groupée au lieu de N requêtes
        List<Object[]> results = mouvementRepository.sumAmountByClientsAndPeriod(
                clientIds, startDateTime, endDateTime);
        
        // Mapper les résultats 
        for (Object[] result : results) {
            Long clientId = (Long) result[0];
            Double montant = (Double) result[1];
            resultMap.put(clientId, BigDecimal.valueOf(montant != null ? montant : 0.0));
        }
        
        return resultMap;
    }

    /**
     * Récupère les paramètres de commission pour tous les clients selon la hiérarchie
     */
    private Map<Long, CommissionParameter> getParametresGroupes(List<Client> clients, Long collecteurId, Long agenceId) {
        Map<Long, CommissionParameter> resultMap = new HashMap<>();
        
        // 1. Récupération groupée des paramètres clients
        List<Long> clientIds = clients.stream().map(Client::getId).toList();
        List<CommissionParameter> parametresClients = parameterRepository.findByClientIdIn(clientIds);
        Map<Long, CommissionParameter> parametresClientsMap = parametresClients.stream()
                .collect(HashMap::new, (map, param) -> {
                    if (param.getClient() != null) {
                        map.put(param.getClient().getId(), param);
                    }
                }, HashMap::putAll);
        
        // 2. Paramètre collecteur (une seule requête)
        CommissionParameter parametreCollecteur = parameterRepository.findByCollecteurId(collecteurId).orElse(null);
        
        // 3. Paramètre agence (une seule requête)
        CommissionParameter parametreAgence = parameterRepository.findByAgenceId(agenceId).orElse(null);
        
        // 4. Application de la hiérarchie pour chaque client
        for (Client client : clients) {
            CommissionParameter parametre = parametresClientsMap.get(client.getId());
            if (parametre == null) {
                parametre = parametreCollecteur;
            }
            if (parametre == null) {
                parametre = parametreAgence;
            }
            resultMap.put(client.getId(), parametre);
        }
        
        return resultMap;
    }

    /**
     * Version optimisée du calcul de commission (sans requêtes DB)
     */
    private CommissionClientDetail calculateClientCommissionOptimized(Client client, 
            BigDecimal montantEpargne, CommissionParameter parameter) {
        
        if (montantEpargne == null || montantEpargne.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Pas d'épargne pour client {}", client.getNom());
            return null;
        }
        
        if (parameter == null) {
            log.warn("Aucun paramètre de commission trouvé pour client {}", client.getNom());
            return null;
        }

        // Calcul de la commission "x"
        BigDecimal commissionX = calculatorService.calculateCommission(montantEpargne, parameter);
        BigDecimal tva = calculatorService.calculateTVA(commissionX);
        BigDecimal soldeNet = calculatorService.calculateSoldeNet(
                BigDecimal.valueOf(client.getSolde()), commissionX, tva);

        return CommissionClientDetail.builder()
                .clientId(client.getId())
                .clientNom(client.getNom())
                .montantEpargne(montantEpargne)
                .commissionX(commissionX)
                .tva(tva)
                .ancienSolde(BigDecimal.valueOf(client.getSolde()))
                .nouveauSolde(soldeNet)
                .parameterUsed(parameter.getType().name())
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
        
        // 🔥 NOUVEAUX CHAMPS pour le système anti-doublon
        private Long historiqueId;  // ID de l'historique de calcul
        private String message;     // Message d'information (ex: "déjà calculé")

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