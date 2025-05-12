package org.example.collectfocep.collectfocep.integration;

import org.example.collectfocep.CollectFocepApplication;
import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.CommissionCalculationService;
import org.example.collectfocep.services.CommissionRepartitionService;
import org.example.collectfocep.services.impl.MouvementService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CollectFocepApplication.class)
@ActiveProfiles("test")
@Transactional
public class CommissionCalculationIntegrationTest {

    @Autowired
    private CommissionCalculationService commissionCalculationService;

    @Autowired
    private CommissionRepartitionService commissionRepartitionService;

    @Autowired
    private CompteService compteService;

    @Autowired
    private MouvementService mouvementService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CollecteurRepository collecteurRepository;

    @Autowired
    private AgenceRepository agenceRepository;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private CompteRepository compteRepository;

    @Autowired
    private CompteClientRepository compteClientRepository;

    @Autowired
    private CompteCollecteurRepository compteCollecteurRepository;

    @Autowired
    private CommissionParameterRepository commissionParameterRepository;

    @Autowired
    private CommissionRepository commissionRepository;

    @Value("${commission.tva.rate:0.1925}")
    private double TVA_RATE;

    @Value("${commission.emf.rate:0.30}")
    private double EMF_RATE;

    private Agence agence;
    private Collecteur collecteur;
    private Client client1;
    private Client client2;
    private Journal journal;
    private CompteClient compteClient1;
    private CompteClient compteClient2;
    private CompteCollecteur compteCollecteur;
    private CompteCollecteur compteAttente;
    private CompteCollecteur compteRemunerationCollecteur;
    private Compte compteTaxe;
    private Compte compteProduit;
    private CommissionParameter commissionParameter;
    private MouvementRepository mouvementRepository;

    @BeforeEach
    void setUp() {
        // Créer et sauvegarder une agence
        agence = new Agence();
        agence.setCodeAgence("A01");
        agence.setNomAgence("Agence Test");
        agence = agenceRepository.save(agence);

        // Créer et sauvegarder un collecteur
        collecteur = new Collecteur();
        collecteur.setNom("Nom Collecteur");
        collecteur.setPrenom("Prénom Collecteur");
        collecteur.setAdresseMail("collecteur@example.com");
        collecteur.setPassword("password123");
        collecteur.setNumeroCni("1234567890");
        collecteur.setTelephone("123456789");
        collecteur.setRole("COLLECTEUR");
        collecteur.setAgence(agence);
        collecteur.setAncienneteEnMois(5); // Plus de 3 mois (seuil pour nouveau collecteur)
        collecteur.setMontantMaxRetrait(200000.0);
        collecteur.setActive(true);
        collecteur = collecteurRepository.save(collecteur);

        // Créer les comptes du collecteur
        compteService.createCollecteurAccounts(collecteur);

        // Créer et sauvegarder des clients
        client1 = new Client();
        client1.setNom("Nom Client 1");
        client1.setPrenom("Prénom Client 1");
        client1.setNumeroCni("1111111111");
        client1.setTelephone("111111111");
        client1.setAgence(agence);
        client1.setCollecteur(collecteur);
        client1.setValide(true);
        client1 = clientRepository.save(client1);

        client2 = new Client();
        client2.setNom("Nom Client 2");
        client2.setPrenom("Prénom Client 2");
        client2.setNumeroCni("2222222222");
        client2.setTelephone("222222222");
        client2.setAgence(agence);
        client2.setCollecteur(collecteur);
        client2.setValide(true);
        client2 = clientRepository.save(client2);

        // Créer des comptes clients
        compteClient1 = new CompteClient();
        compteClient1.setNomCompte("Compte " + client1.getNom());
        compteClient1.setNumeroCompte("CL" + client1.getId() + "001");
        compteClient1.setTypeCompte("EPARGNE_JOURNALIERE");
        compteClient1.setSolde(0.0);
        compteClient1.setClient(client1);
        compteClient1 = compteClientRepository.save(compteClient1);

        compteClient2 = new CompteClient();
        compteClient2.setNomCompte("Compte " + client2.getNom());
        compteClient2.setNumeroCompte("CL" + client2.getId() + "001");
        compteClient2.setTypeCompte("EPARGNE_JOURNALIERE");
        compteClient2.setSolde(0.0);
        compteClient2.setClient(client2);
        compteClient2 = compteClientRepository.save(compteClient2);

        // Récupérer les comptes spéciaux
        compteCollecteur = compteService.findServiceAccount(collecteur);
        compteAttente = compteService.findWaitingAccount(collecteur);
        compteRemunerationCollecteur = compteService.findSalaryAccount(collecteur);

        // Créer/récupérer les comptes système
        compteTaxe = compteRepository.findByTypeCompte("TAXE").orElseGet(() -> {
            Compte compte = new CompteClient(); // On utilise un type concret pour le test
            compte.setNomCompte("Compte Taxe");
            compte.setNumeroCompte("TAXE001");
            compte.setTypeCompte("TAXE");
            compte.setSolde(0.0);
            return compteRepository.save(compte);
        });

        compteProduit = compteRepository.findByTypeCompte("PRODUIT").orElseGet(() -> {
            Compte compte = new CompteClient(); // On utilise un type concret pour le test
            compte.setNomCompte("Compte Produit");
            compte.setNumeroCompte("PRODUIT001");
            compte.setTypeCompte("PRODUIT");
            compte.setSolde(0.0);
            return compteRepository.save(compte);
        });

        // Créer un journal pour la collecte
        journal = new Journal();
        journal.setDateDebut(LocalDate.now());
        journal.setDateFin(LocalDate.now().plusDays(1));
        journal.setCollecteur(collecteur);
        journal.setEstCloture(false);
        journal = journalRepository.save(journal);

        // Créer un paramètre de commission (pourcentage)
        commissionParameter = new CommissionParameter();
        commissionParameter.setType(CommissionType.PERCENTAGE);
        commissionParameter.setValeur(2.0); // 2% de commission
        commissionParameter.setActive(true);
        commissionParameter.setValidFrom(LocalDate.now().minusMonths(1));
        commissionParameter.setValidTo(LocalDate.now().plusMonths(1));
        commissionParameter.setAgence(agence);
        commissionParameter = commissionParameterRepository.save(commissionParameter);
    }

    @Test
    void testCalculEtRepartitionCommission() {
        // 1. Effectuer des épargnes pour les clients
        double montantEpargne1 = 100000.0; // Montant significatif pour tester les commissions
        double montantEpargne2 = 50000.0;

        // Enregistrer les épargnes
        Mouvement mouvementEpargne1 = mouvementService.enregistrerEpargne(client1, montantEpargne1, journal);
        Mouvement mouvementEpargne2 = mouvementService.enregistrerEpargne(client2, montantEpargne2, journal);

        // 2. Définir la période pour le calcul des commissions
        LocalDate dateDebut = LocalDate.now().minusDays(1);
        LocalDate dateFin = LocalDate.now().plusDays(1);

        // 3. Calculer les commissions pour le collecteur
        CommissionResult result = commissionCalculationService.calculateCommissions(
                collecteur.getId(), dateDebut, dateFin);

        // Vérifier le résultat du calcul
        assertNotNull(result);

        // Calculer la valeur attendue des commissions (2% du montant total collecté)
        double montantTotalCollecte = montantEpargne1 + montantEpargne2;
        double commissionAttendue = montantTotalCollecte * 0.02;
        double tvaAttendue = commissionAttendue * TVA_RATE;

        assertEquals(commissionAttendue, result.getMontantCommission(), 0.01);
        assertEquals(tvaAttendue, result.getMontantTVA(), 0.01);

        // 4. Appliquer la répartition des commissions
        commissionRepartitionService.processRepartition(result);

        // 5. Vérifier les comptes après répartition
        // Recharger tous les comptes
        compteAttente = compteCollecteurRepository.findById(compteAttente.getId()).orElseThrow();
        compteRemunerationCollecteur = compteCollecteurRepository.findById(compteRemunerationCollecteur.getId()).orElseThrow();
        compteTaxe = compteRepository.findById(compteTaxe.getId()).orElseThrow();
        compteProduit = compteRepository.findById(compteProduit.getId()).orElseThrow();

        // Calculer la répartition attendue (70% collecteur, 30% EMF pour collecteur > 3 mois)
        double partCollecteurAttendue = commissionAttendue * 0.70;
        double partEMFAttendue = commissionAttendue * 0.30;
        double tvaEMFAttendue = partEMFAttendue * TVA_RATE;

        // Vérifier les soldes des comptes
        assertEquals(partCollecteurAttendue, compteRemunerationCollecteur.getSolde(), 0.01, "Rémunération collecteur incorrecte");
        assertEquals(tvaAttendue + tvaEMFAttendue, compteTaxe.getSolde(), 0.01, "TVA totale incorrecte");
        assertEquals(partEMFAttendue - tvaEMFAttendue, compteProduit.getSolde(), 0.01, "Part EMF incorrecte");

        // 6. Vérifier que des commissions ont été créées en base
        List<Commission> commissions = commissionRepository.findByCollecteurId(collecteur.getId());
        assertFalse(commissions.isEmpty(), "Aucune commission n'a été enregistrée");

        // Vérifier les mouvements de commission
        List<Mouvement> mouvementsCommission = mouvementRepository.findByLibelleContaining("Commission");
        assertFalse(mouvementsCommission.isEmpty(), "Aucun mouvement de commission n'a été créé");
    }

    @Test
    void testCommissionNouveauCollecteur() {
        // Modifier l'ancienneté du collecteur pour qu'il soit considéré comme nouveau
        collecteur.setAncienneteEnMois(2); // Moins de 3 mois
        collecteur = collecteurRepository.save(collecteur);

        // Effectuer des épargnes pour les clients
        double montantEpargne1 = 100000.0;
        Mouvement mouvementEpargne1 = mouvementService.enregistrerEpargne(client1, montantEpargne1, journal);

        // Définir la période pour le calcul des commissions
        LocalDate dateDebut = LocalDate.now().minusDays(1);
        LocalDate dateFin = LocalDate.now().plusDays(1);

        // Calculer les commissions pour le nouveau collecteur
        CommissionResult result = commissionCalculationService.calculateCommissions(
                collecteur.getId(), dateDebut, dateFin);

        // Vérifier le résultat du calcul
        assertNotNull(result);

        // Appliquer la répartition des commissions
        commissionRepartitionService.processRepartition(result);

        // Recharger le compte de rémunération
        compteRemunerationCollecteur = compteCollecteurRepository.findById(compteRemunerationCollecteur.getId()).orElseThrow();

        // Pour un nouveau collecteur, le montant fixe est normalement 40000 FCFA
        // Vérifier si la rémunération est fixe pour un nouveau collecteur
        assertEquals(40000.0, compteRemunerationCollecteur.getSolde(), 0.01,
                "La rémunération d'un nouveau collecteur devrait être un montant fixe");
    }
}
