package org.example.collectfocep.services;

import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.CompteNotFoundException;
import org.example.collectfocep.interfaces.ICommissionCalculationService;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.impl.MouvementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
public class CommissionCalculationService implements ICommissionCalculationService {
    @Value("${commission.tva.rate:0.1925}")
    private double TVA_RATE;

    @Value("${commission.emf.rate:0.30}")
    private double EMF_RATE;

    @Value("${commission.nouveau.collecteur:40000}")
    private double MONTANT_NOUVEAU_COLLECTEUR;

    private final CommissionParameterRepository commissionParameterRepository;
    private final CompteRepository compteRepository;
    private final MouvementService mouvementService;
    private final ClientRepository clientRepository;
    private final CollecteurRepository collecteurRepository;
    private final MouvementRepository mouvementRepository;
    private final CompteClientRepository compteClientRepository;

    @Autowired
    public CommissionCalculationService(
            CommissionParameterRepository commissionParameterRepository,
            CompteRepository compteRepository,
            MouvementService mouvementService,
            ClientRepository clientRepository,
            CollecteurRepository collecteurRepository,
            MouvementRepository mouvementRepository,
            CompteClientRepository compteClientRepository) {
        this.commissionParameterRepository = commissionParameterRepository;
        this.compteRepository = compteRepository;
        this.mouvementService = mouvementService;
        this.clientRepository = clientRepository;
        this.collecteurRepository = collecteurRepository;
        this.mouvementRepository = mouvementRepository;
        this.compteClientRepository = compteClientRepository;
    }

    @Override
    public CommissionResult calculateCommissionForClient(Client client, LocalDate dateDebut, LocalDate dateFin, double montantTotal) {
        // Recherche des paramètres par priorité
        CommissionParameter params = getCommissionParameters(client);

        // Calcul de la commission
        double montantCommission = calculateCommissionByType(params, montantTotal);
        double montantTVA = montantCommission * TVA_RATE;
        double montantNet = montantCommission - montantTVA;

        // Récupérer le compte client
        CompteClient compteClient = compteClientRepository.findByClient(client)
                .orElseThrow(() -> new CompteNotFoundException("Compte client non trouvé pour client: " + client.getId()));

        // Compte d'attente pour recevoir les fonds
        Compte compteAttente = compteRepository.findByTypeCompte("ATTENTE")
                .orElseThrow(() -> new CompteNotFoundException("Compte d'attente non trouvé"));

        // 1. Débiter la commission du compte client
        compteClient.setSolde(compteClient.getSolde() - montantCommission);
        compteAttente.setSolde(compteAttente.getSolde() + montantCommission);

        // Créer un mouvement pour la commission
        Mouvement mouvementCommission = new Mouvement();
        mouvementCommission.setCompteSource(compteClient);
        mouvementCommission.setCompteDestination(compteAttente);
        mouvementCommission.setMontant(montantCommission);
        mouvementCommission.setSens("DEBIT");
        mouvementCommission.setLibelle("Commission client: " + client.getNom());
        mouvementCommission.setDateOperation(LocalDateTime.now());
        mouvementRepository.save(mouvementCommission);

        // 2. Débiter la TVA sur commission du compte client
        compteClient.setSolde(compteClient.getSolde() - montantTVA);

        // Compte taxe pour la TVA
        Compte compteTaxe = compteRepository.findByTypeCompte("TAXE")
                .orElseThrow(() -> new CompteNotFoundException("Compte taxe non trouvé"));

        compteTaxe.setSolde(compteTaxe.getSolde() + montantTVA);

        // Créer un mouvement pour la TVA
        Mouvement mouvementTVA = new Mouvement();
        mouvementTVA.setCompteSource(compteClient);
        mouvementTVA.setCompteDestination(compteTaxe);
        mouvementTVA.setMontant(montantTVA);
        mouvementTVA.setSens("DEBIT");
        mouvementTVA.setLibelle("TVA sur commission client: " + client.getNom());
        mouvementTVA.setDateOperation(LocalDateTime.now());
        mouvementRepository.save(mouvementTVA);

        // Sauvegarder les changements sur les comptes
        compteRepository.save(compteClient);
        compteRepository.save(compteAttente);
        compteRepository.save(compteTaxe);

        return CommissionResult.builder()
                .montantCommission(montantCommission)
                .montantTVA(montantTVA)
                .montantNet(montantNet)
                .typeCalcul(params.getType().name())
                .dateCalcul(LocalDateTime.now())
                .clientId(client.getId())
                .collecteurId(client.getCollecteur().getId())
                .build();
    }

    @Override
    public CommissionParameter getCommissionParameters(Client client) {
        // Recherche des paramètres par niveau de priorité
        return commissionParameterRepository.findByClient(client)
                .orElseGet(() -> commissionParameterRepository.findByCollecteur(client.getCollecteur())
                        .orElseGet(() -> commissionParameterRepository.findByAgence(client.getAgence())
                                .orElseThrow(() -> new RuntimeException("Aucun paramètre de commission trouvé"))));
    }

    @Override
    public void processCollecteurRemuneration(Collecteur collecteur, LocalDate dateDebut, LocalDate dateFin) {
        // Implémentation du traitement de la rémunération
        // Cette méthode sera appelée après le calcul des commissions pour tous les clients d'un collecteur
    }

    /**
     * Calcule la commission en fonction du type de paramètre
     */
    private double calculateCommissionByType(CommissionParameter params, double montantTotal) {
        switch (params.getType()) {
            case FIXED:
                return params.getValeur();
            case PERCENTAGE:
                return montantTotal * (params.getValeur() / 100.0);
            case TIER:
                // Recherche du palier applicable
                return params.getTiers().stream()
                        .filter(tier -> montantTotal >= tier.getMontantMin() && montantTotal <= tier.getMontantMax())
                        .findFirst()
                        .map(tier -> montantTotal * (tier.getTaux() / 100.0))
                        .orElse(0.0);
            default:
                return 0.0;
        }
    }

    /**
     * Implémentation de calculateCommissions requise par AsyncCommissionService
     */
    public CommissionResult calculateCommissions(Long collecteurId, LocalDate startDate, LocalDate endDate) {
        // Récupérer le collecteur
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new RuntimeException("Collecteur non trouvé"));

        // Calculer le montant total collecté pour la période
        double totalCollecte = 0.0;
        double totalCommissions = 0.0;
        double totalTVA = 0.0;

        // Pour chaque client du collecteur
        for (Client client : collecteur.getClients()) {
            double montantClient = getMontantCollecte(client, startDate, endDate);
            totalCollecte += montantClient;

            if (montantClient > 0) {
                CommissionResult clientResult = calculateCommissionForClient(client, startDate, endDate, montantClient);
                totalCommissions += clientResult.getMontantCommission();
                totalTVA += clientResult.getMontantTVA();
            }
        }

        // Créer un résultat global pour le collecteur
        return CommissionResult.builder()
                .montantCommission(totalCommissions)
                .montantTVA(totalTVA)
                .montantNet(totalCommissions - totalTVA)
                .typeCalcul("COLLECTEUR_GLOBAL")
                .dateCalcul(LocalDateTime.now())
                .collecteurId(collecteurId)
                .build();
    }

    // Ajout des méthodes manquantes
    private double calculateCollecteurRemuneration(Collecteur collecteur, double totalCommissions) {
        // Règle de rémunération selon l'ancienneté
        if (collecteur.getAncienneteEnMois() <= 3) {
            // Les 3 premiers mois: montant fixe
            return MONTANT_NOUVEAU_COLLECTEUR;
        } else {
            // Après 3 mois: 70% du total des commissions
            return totalCommissions * 0.70;
        }
    }

    private Compte getCompteTaxe() {
        return compteRepository.findByTypeCompte("TAXE")
                .orElseThrow(() -> new CompteNotFoundException("Compte taxe non trouvé"));
    }

    private Compte getCompteProduitFOCEP() {
        return compteRepository.findByTypeCompte("PRODUIT")
                .orElseThrow(() -> new CompteNotFoundException("Compte produit FOCEP non trouvé"));
    }

    private Compte getCompteChargeCollecteur(Long collecteurId) {
        return compteRepository.findByTypeCompteAndCollecteurId("CHARGE", collecteurId)
                .orElseThrow(() -> new CompteNotFoundException("Compte de charge du collecteur non trouvé"));
    }

    private void processNormalCase(Compte compteAttente, Compte compteCollecteur, Compte compteTaxe,
                                   Compte compteProduit, double remuneration, double totalCommissions, double totalTVA) {
        // Cas normal: remuneration <= totalCommissions
        // 1. Payer la rémunération du collecteur
        // Débit compteAttente, crédit compteCollecteur
        Mouvement mouvementRemuneration = new Mouvement();
        mouvementRemuneration.setCompteSource(compteAttente);
        mouvementRemuneration.setCompteDestination(compteCollecteur);
        mouvementRemuneration.setMontant(remuneration);
        mouvementRemuneration.setSens("DEBIT");
        mouvementRemuneration.setLibelle("Rémunération collecteur");
        mouvementRemuneration.setDateOperation(LocalDateTime.now());
        mouvementService.effectuerMouvement(mouvementRemuneration);

        // 2. Calculer la TVA EMF sur le reste (30% pour l'EMF)
        double resteCommission = totalCommissions - remuneration;
        double tvaSurReste = resteCommission * TVA_RATE;

        // 3. Transférer la TVA au compte taxe
        Mouvement mouvementTVA = new Mouvement();
        mouvementTVA.setCompteSource(compteAttente);
        mouvementTVA.setCompteDestination(compteTaxe);
        mouvementTVA.setMontant(tvaSurReste);
        mouvementTVA.setSens("DEBIT");
        mouvementTVA.setLibelle("TVA sur part EMF");
        mouvementTVA.setDateOperation(LocalDateTime.now());
        mouvementService.effectuerMouvement(mouvementTVA);

        // 4. Transférer le reste vers le compte produit FOCEP
        double resteFinal = resteCommission - tvaSurReste;
        Mouvement mouvementProduit = new Mouvement();
        mouvementProduit.setCompteSource(compteAttente);
        mouvementProduit.setCompteDestination(compteProduit);
        mouvementProduit.setMontant(resteFinal);
        mouvementProduit.setSens("DEBIT");
        mouvementProduit.setLibelle("Part produit FOCEP");
        mouvementProduit.setDateOperation(LocalDateTime.now());
        mouvementService.effectuerMouvement(mouvementProduit);
    }

    private void processDeficitCase(Collecteur collecteur, Compte compteAttente,
                                    Compte compteCollecteur, double remuneration, double totalCommissions) {
        // Cas de déficit: remuneration > totalCommissions
        // 1. Transférer tout le montant disponible au collecteur
        Mouvement mouvementDisponible = new Mouvement();
        mouvementDisponible.setCompteSource(compteAttente);
        mouvementDisponible.setCompteDestination(compteCollecteur);
        mouvementDisponible.setMontant(totalCommissions);
        mouvementDisponible.setSens("DEBIT");
        mouvementDisponible.setLibelle("Transfert disponible vers collecteur");
        mouvementDisponible.setDateOperation(LocalDateTime.now());
        mouvementService.effectuerMouvement(mouvementDisponible);

        // 2. Compléter avec le compte de charge du collecteur
        double manquant = remuneration - totalCommissions;

        // Récupérer le compte de charge du collecteur
        Compte compteCharge = getCompteChargeCollecteur(collecteur.getId());

        Mouvement mouvementComplement = new Mouvement();
        mouvementComplement.setCompteSource(compteCharge);
        mouvementComplement.setCompteDestination(compteCollecteur);
        mouvementComplement.setMontant(manquant);
        mouvementComplement.setSens("DEBIT");
        mouvementComplement.setLibelle("Complément de rémunération depuis compte charge");
        mouvementComplement.setDateOperation(LocalDateTime.now());
        mouvementService.effectuerMouvement(mouvementComplement);
    }

    private double getMontantCollecte(Client client, LocalDate startDate, LocalDate endDate) {
        return mouvementRepository.sumAmountByClientAndPeriod(
                client.getId(),
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusSeconds(1)
        );
    }
}