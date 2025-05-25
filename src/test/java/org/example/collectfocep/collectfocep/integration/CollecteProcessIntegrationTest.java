package org.example.collectfocep.collectfocep.integration;

import org.example.collectfocep.CollectFocepApplication;
import org.example.collectfocep.collectfocep.config.TestDatabaseConfig;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.impl.MouvementServiceImpl;
import org.example.collectfocep.services.interfaces.ClientService;
import org.example.collectfocep.services.interfaces.CollecteurService;
import org.example.collectfocep.services.interfaces.CompteService;
import org.example.collectfocep.services.interfaces.JournalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CollectFocepApplication.class, TestDatabaseConfig.class})
@ActiveProfiles("test")
@Transactional
public class CollecteProcessIntegrationTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private CollecteurService collecteurService;

    @Autowired
    private CompteService compteService;

    @Autowired
    private JournalService journalService;

    @Autowired
    private MouvementServiceImpl mouvementServiceImpl;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CollecteurRepository collecteurRepository;

    @Autowired
    private AgenceRepository agenceRepository;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private MouvementRepository mouvementRepository;

    @Autowired
    private CompteRepository compteRepository;

    @Autowired
    private CompteClientRepository compteClientRepository;

    @Autowired
    private CompteCollecteurRepository compteCollecteurRepository;

    private Agence agence;
    private Collecteur collecteur;
    private Client client1;
    private Client client2;
    private Journal journal;
    private CompteClient compteClient1;
    private CompteClient compteClient2;
    private CompteCollecteur compteCollecteur;

    @BeforeEach
    void setUp() {
        // Nettoyer les tables
        mouvementRepository.deleteAll();
        journalRepository.deleteAll();
        compteClientRepository.deleteAll();
        compteCollecteurRepository.deleteAll();
        compteRepository.deleteAll();
        clientRepository.deleteAll();
        collecteurRepository.deleteAll();
        agenceRepository.deleteAll();

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
        collecteur.setAncienneteEnMois(5);
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

        // Récupérer le compte service du collecteur
        compteCollecteur = compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SERVICE")
                .orElseThrow(() -> new RuntimeException("Compte service collecteur non trouvé"));

        // Créer un journal pour la collecte
        journal = new Journal();
        journal.setDateDebut(LocalDate.now());
        journal.setDateFin(LocalDate.now().plusDays(1));
        journal.setCollecteur(collecteur);
        journal.setEstCloture(false);
        journal = journalRepository.save(journal);
    }

    @Test
    void testProcessusCompletCollecte() {
        // 1. Effectuer une épargne pour le client 1
        double montantEpargne1 = 5000.0;
        Mouvement mouvementEpargne1 = mouvementServiceImpl.enregistrerEpargne(client1, montantEpargne1, journal);

        // Vérifier l'enregistrement de l'épargne
        assertNotNull(mouvementEpargne1);
        assertEquals(montantEpargne1, mouvementEpargne1.getMontant());
        assertEquals("epargne", mouvementEpargne1.getSens());
        assertNotNull(mouvementEpargne1.getDateOperation());
        assertEquals(compteCollecteur.getId(), mouvementEpargne1.getCompteSource().getId());
        assertEquals(compteClient1.getId(), mouvementEpargne1.getCompteDestination().getId());

        // Vérifier les soldes après épargne
        compteClient1 = compteClientRepository.findById(compteClient1.getId()).orElseThrow();
        compteCollecteur = compteCollecteurRepository.findById(compteCollecteur.getId()).orElseThrow();

        assertEquals(montantEpargne1, compteClient1.getSolde());
        assertEquals(-montantEpargne1, compteCollecteur.getSolde()); // Le compte service est débité

        // 2. Effectuer une épargne pour le client 2
        double montantEpargne2 = 3000.0;
        Mouvement mouvementEpargne2 = mouvementServiceImpl.enregistrerEpargne(client2, montantEpargne2, journal);

        // Vérifier l'enregistrement de l'épargne
        assertNotNull(mouvementEpargne2);
        assertEquals(montantEpargne2, mouvementEpargne2.getMontant());

        // Vérifier les soldes après la deuxième épargne
        compteClient2 = compteClientRepository.findById(compteClient2.getId()).orElseThrow();
        compteCollecteur = compteCollecteurRepository.findById(compteCollecteur.getId()).orElseThrow();

        assertEquals(montantEpargne2, compteClient2.getSolde());
        assertEquals(-(montantEpargne1 + montantEpargne2), compteCollecteur.getSolde());

        // 3. Effectuer un retrait pour le client 1
        double montantRetrait = 2000.0;
        Mouvement mouvementRetrait = mouvementServiceImpl.enregistrerRetrait(client1, montantRetrait, journal);

        // Vérifier l'enregistrement du retrait
        assertNotNull(mouvementRetrait);
        assertEquals(montantRetrait, mouvementRetrait.getMontant());
        assertEquals("retrait", mouvementRetrait.getSens());
        assertNotNull(mouvementRetrait.getDateOperation());
        assertEquals(compteClient1.getId(), mouvementRetrait.getCompteSource().getId());
        assertEquals(compteCollecteur.getId(), mouvementRetrait.getCompteDestination().getId());

        // Vérifier les soldes après retrait
        compteClient1 = compteClientRepository.findById(compteClient1.getId()).orElseThrow();
        compteCollecteur = compteCollecteurRepository.findById(compteCollecteur.getId()).orElseThrow();

        assertEquals(montantEpargne1 - montantRetrait, compteClient1.getSolde());
        assertEquals(-(montantEpargne1 + montantEpargne2 - montantRetrait), compteCollecteur.getSolde());

        // 4. Récupérer la liste des mouvements du journal
        List<Mouvement> mouvements = mouvementServiceImpl.findByJournalId(journal.getId());
        assertEquals(3, mouvements.size());

        // 5. Clôturer le journal
        Journal journalCloture = journalService.cloturerJournal(journal.getId());
        assertTrue(journalCloture.isEstCloture());
        assertNotNull(journalCloture.getDateCloture());

        // 6. Vérifier qu'on ne peut plus ajouter de mouvements au journal clôturé
        Journal journalFinal = journalRepository.findById(journal.getId()).orElseThrow();
        assertTrue(journalFinal.isEstCloture());

        // Option 1: si votre code vérifie la clôture de journal avant d'ajouter des mouvements
        try {
            Mouvement mouvementApresClotureAttendu = mouvementServiceImpl.enregistrerEpargne(client1, 1000.0, journalFinal);
            // Selon l'implémentation, vous pouvez soit recevoir une exception, soit avoir un comportement spécifique
            // Cette assertion dépend de votre implémentation
            fail("Une exception aurait dû être levée pour un journal clôturé");
        } catch (Exception e) {
            // L'exception est attendue
            assertTrue(e.getMessage().contains("clôtur") || e.getMessage().contains("journ"));
        }

        // 7. Vérifier les totaux des mouvements du journal
        double totalEpargnes = mouvements.stream()
                .filter(m -> "epargne".equals(m.getSens()))
                .mapToDouble(Mouvement::getMontant)
                .sum();

        double totalRetraits = mouvements.stream()
                .filter(m -> "retrait".equals(m.getSens()))
                .mapToDouble(Mouvement::getMontant)
                .sum();

        assertEquals(montantEpargne1 + montantEpargne2, totalEpargnes);
        assertEquals(montantRetrait, totalRetraits);
    }

    @Test
    void testVersementEtVentilationCollecte() {
        // 1. Effectuer des épargnes pour les clients
        double montantEpargne1 = 5000.0;
        double montantEpargne2 = 3000.0;

        Mouvement mouvementEpargne1 = mouvementServiceImpl.enregistrerEpargne(client1, montantEpargne1, journal);
        Mouvement mouvementEpargne2 = mouvementServiceImpl.enregistrerEpargne(client2, montantEpargne2, journal);

        // Vérifier l'état initial
        compteCollecteur = compteCollecteurRepository.findById(compteCollecteur.getId()).orElseThrow();
        assertEquals(-(montantEpargne1 + montantEpargne2), compteCollecteur.getSolde());

        // 2. Récupérer le compte de liaison de l'agence
        CompteLiaison compteLiaison = compteService.findLiaisonAccount(agence);
        assertNotNull(compteLiaison);
        double soldeLiaisonInitial = compteLiaison.getSolde();

        // 3. Simuler un versement du collecteur à l'agence
        double montantVersement = montantEpargne1 + montantEpargne2;
        Mouvement mouvementVersement = mouvementServiceImpl.enregistrerVersement(collecteur, montantVersement, journal);

        // Vérifier l'enregistrement du versement
        assertNotNull(mouvementVersement);
        assertEquals(montantVersement, mouvementVersement.getMontant());
        assertEquals("versement", mouvementVersement.getSens());
        assertEquals(compteLiaison.getId(), mouvementVersement.getCompteSource().getId());
        assertEquals(compteCollecteur.getId(), mouvementVersement.getCompteDestination().getId());

        // 4. Vérifier les soldes après versement
        compteCollecteur = compteCollecteurRepository.findById(compteCollecteur.getId()).orElseThrow();
        compteLiaison = compteService.findLiaisonAccount(agence);

        assertEquals(0.0, compteCollecteur.getSolde(), 0.001); // Le solde du compte service devrait être à 0
        assertEquals(soldeLiaisonInitial - montantVersement, compteLiaison.getSolde(), 0.001);

        // 5. Vérifier les mouvements du journal
        List<Mouvement> mouvements = mouvementServiceImpl.findByJournalId(journal.getId());
        assertEquals(3, mouvements.size()); // 2 épargnes + 1 versement

        // 6. Clôturer le journal
        Journal journalCloture = journalService.cloturerJournal(journal.getId());
        assertTrue(journalCloture.isEstCloture());
    }
}
