package org.example.collectfocep.collectfocep.integration;

import org.example.collectfocep.CollectFocepApplication;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.ReportGenerationService;
import org.example.collectfocep.services.ReportService;
import org.example.collectfocep.services.impl.MouvementService;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CollectFocepApplication.class)
@ActiveProfiles("service-test")
@Transactional
public class ReportGenerationIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportGenerationService reportGenerationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private CollecteurService collecteurService;

    @Autowired
    private CompteService compteService;

    @Autowired
    private JournalService journalService;

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
    private MouvementRepository mouvementRepository;

    @Autowired
    private CompteClientRepository compteClientRepository;

    @Autowired
    private CompteCollecteurRepository compteCollecteurRepository;

    private Agence agence;
    private Collecteur collecteur;
    private List<Client> clients;
    private List<CompteClient> compteClients;
    private Journal journal;
    private CompteCollecteur compteCollecteur;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    @BeforeEach
    void setUp() {
        dateDebut = LocalDate.now().withDayOfMonth(1); // Premier jour du mois
        dateFin = YearMonth.from(dateDebut).atEndOfMonth(); // Dernier jour du mois

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

        // Récupérer le compte service du collecteur
        compteCollecteur = compteService.findServiceAccount(collecteur);

        // Créer un journal pour la collecte
        journal = new Journal();
        journal.setDateDebut(dateDebut);
        journal.setDateFin(dateFin);
        journal.setCollecteur(collecteur);
        journal.setEstCloture(false);
        journal = journalRepository.save(journal);

        // Créer plusieurs clients et leurs comptes
        clients = new ArrayList<>();
        compteClients = new ArrayList<>();

        // Créer 5 clients avec des mouvements sur plusieurs jours
        for (int i = 1; i <= 5; i++) {
            Client client = new Client();
            client.setNom("Client " + i);
            client.setPrenom("Prénom Client " + i);
            client.setNumeroCni("111111111" + i);
            client.setTelephone("11111111" + i);
            client.setAgence(agence);
            client.setCollecteur(collecteur);
            client.setValide(true);
            client = clientRepository.save(client);
            clients.add(client);

            CompteClient compteClient = new CompteClient();
            compteClient.setNomCompte("Compte " + client.getNom());
            compteClient.setNumeroCompte("CL" + client.getId() + "001");
            compteClient.setTypeCompte("EPARGNE_JOURNALIERE");
            compteClient.setSolde(0.0);
            compteClient.setClient(client);
            compteClient = compteClientRepository.save(compteClient);
            compteClients.add(compteClient);
        }

        // Créer des mouvements d'épargne sur différents jours du mois
        createMovementsForDay(1, 1000.0); // Jour 1
        createMovementsForDay(5, 2000.0); // Jour 5
        createMovementsForDay(10, 3000.0); // Jour 10
        createMovementsForDay(15, 1500.0); // Jour 15
        createMovementsForDay(20, 2500.0); // Jour 20

        // Créer quelques retraits
        LocalDate withdrawalDate = dateDebut.plusDays(12);
        for (int i = 0; i < 2; i++) {
            Client client = clients.get(i);
            createWithdrawalForClient(client, withdrawalDate, 500.0);
        }
    }

    private void createMovementsForDay(int dayOfMonth, double amount) {
        LocalDate movementDate = dateDebut.withDayOfMonth(dayOfMonth);

        // Créer un journal spécifique pour ce jour si besoin
        Journal dailyJournal = journalRepository.findByCollecteurAndDateDebut(collecteur, movementDate)
                .orElseGet(() -> {
                    Journal j = new Journal();
                    j.setDateDebut(movementDate);
                    j.setDateFin(movementDate);
                    j.setCollecteur(collecteur);
                    j.setEstCloture(false);
                    return journalRepository.save(j);
                });

        // Créer un mouvement d'épargne pour chaque client ce jour-là
        for (Client client : clients) {
            Mouvement mouvement = mouvementService.enregistrerEpargne(client, amount, dailyJournal);
            mouvement.setDateOperation(LocalDateTime.of(movementDate.getYear(),
                    movementDate.getMonthValue(),
                    movementDate.getDayOfMonth(),
                    9, 0)); // 9h00
            mouvementRepository.save(mouvement);
        }
    }

    private void createWithdrawalForClient(Client client, LocalDate date, double amount) {
        // Créer un journal spécifique pour ce jour si besoin
        Journal dailyJournal = journalRepository.findByCollecteurAndDateDebut(collecteur, date)
                .orElseGet(() -> {
                    Journal j = new Journal();
                    j.setDateDebut(date);
                    j.setDateFin(date);
                    j.setCollecteur(collecteur);
                    j.setEstCloture(false);
                    return journalRepository.save(j);
                });

        // Créer un mouvement de retrait
        Mouvement mouvement = mouvementService.enregistrerRetrait(client, amount, dailyJournal);
        mouvement.setDateOperation(LocalDateTime.of(date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                14, 0)); // 14h00
        mouvementRepository.save(mouvement);
    }

    @Test
    void testGenerationRapportMensuelCollecteur() {
        // 1. Générer le rapport mensuel pour le collecteur
        byte[] reportData = reportGenerationService.generateMonthlyReport(
                collecteur.getId(),
                dateDebut.getMonthValue(),
                dateDebut.getYear());

        // 2. Vérifier que le rapport a bien été généré
        assertNotNull(reportData);
        assertTrue(reportData.length > 0, "Le rapport devrait contenir des données");

        // Le rapport est un fichier Excel, il n'est pas facile de vérifier son contenu ici,
        // mais nous pouvons au moins vérifier qu'il a été généré avec une taille raisonnable
        assertTrue(reportData.length > 1000, "Le rapport semble trop petit pour être valide");
    }

    @Test
    void testGenerationRapportParAgence() {
        // 1. Générer le rapport pour l'agence
        byte[] reportData = reportService.generateAgenceReport(
                agence.getId(), dateDebut, dateFin);

        // 2. Vérifier que le rapport a bien été généré
        assertNotNull(reportData);
        assertTrue(reportData.length > 0, "Le rapport devrait contenir des données");
    }

    @Test
    void testGenerationRapportGlobal() {
        // 1. Générer le rapport global
        byte[] reportData = reportService.generateGlobalReport(dateDebut, dateFin);

        // 2. Vérifier que le rapport a bien été généré
        assertNotNull(reportData);
        assertTrue(reportData.length > 0, "Le rapport devrait contenir des données");
    }

    @Test
    void testGenerationRapportCollecteurParPeriode() {
        // 1. Générer le rapport pour une période spécifique
        LocalDate periodeDebut = dateDebut.plusDays(5);
        LocalDate periodeFin = dateDebut.plusDays(15);

        byte[] reportData = reportService.generateCollecteurReport(
                collecteur.getId(), periodeDebut, periodeFin);

        // 2. Vérifier que le rapport a bien été généré
        assertNotNull(reportData);
        assertTrue(reportData.length > 0, "Le rapport devrait contenir des données");

        // 3. Vérifier que le rapport contient bien les données de la période
        // Note: Ceci est difficile à vérifier sans parser le fichier Excel.
        // Dans un environnement réel, on pourrait utiliser Apache POI pour lire le contenu du rapport
        // et vérifier que les données correspondent à la période spécifiée.
    }
}
