package org.example.collectfocep.collectfocep.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.BusinessException;
import org.example.collectfocep.exceptions.CompteNotFoundException;
import org.example.collectfocep.exceptions.MontantMaxRetraitException;
import org.example.collectfocep.exceptions.SoldeInsuffisantException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.JournalService;
import org.example.collectfocep.services.impl.CommissionService;
import org.example.collectfocep.services.impl.MouvementService;
import org.example.collectfocep.services.impl.TransactionService;
import org.example.collectfocep.util.CompteUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MouvementServiceTest {

    @Mock
    private CompteUtility compteUtility;

    @Mock
    private MouvementRepository mouvementRepository;

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private CompteClientRepository compteClientRepository;

    @Mock
    private CompteCollecteurRepository compteCollecteurRepository;

    @Mock
    private CompteLiaisonRepository compteLiaisonRepository;

    @Mock
    private JournalService journalService;

    @Mock
    private CommissionService commissionService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private Counter epargneCounter;

    @Mock
    private Timer mouvementTimer;

    @InjectMocks
    private MouvementService mouvementService;

    // Données de test
    private Compte compteSource;
    private Compte compteDestination;
    private CompteClient compteClient;
    private CompteCollecteur compteServiceCollecteur;
    private CompteCollecteur compteAttenteCollecteur;
    private CompteLiaison compteLiaison;
    private Client client;
    private Collecteur collecteur;
    private Agence agence;
    private Journal journal;
    private Mouvement mouvement;

    @BeforeEach
    public void setUp() {
        // Configuration de l'agence
        agence = new Agence();
        agence.setId(1L);
        agence.setCodeAgence("A01");
        agence.setNomAgence("Agence Test");

        // Configuration du collecteur
        collecteur = new Collecteur();
        collecteur.setId(1L);
        collecteur.setNom("Nom Collecteur");
        collecteur.setPrenom("Prénom Collecteur");
        collecteur.setAgence(agence);
        collecteur.setMontantMaxRetrait(200000.0);
        collecteur.setAncienneteEnMois(5);
        collecteur.setActive(true);

        // Configuration du client
        client = new Client();
        client.setId(1L);
        client.setNom("Nom Client");
        client.setPrenom("Prénom Client");
        client.setAgence(agence);
        client.setCollecteur(collecteur);
        client.setValide(true);

        // Configuration des comptes
        compteSource = new CompteCollecteur();
        compteSource.setId(1L);
        compteSource.setNumeroCompte("SRC001");
        compteSource.setSolde(1000.0);
        compteSource.setTypeCompte("SERVICE");
        compteSource.setNomCompte("Compte Source Test");
        compteSource.setVersion(0L);

        compteDestination = new CompteClient();
        compteDestination.setId(2L);
        compteDestination.setNumeroCompte("DST001");
        compteDestination.setSolde(500.0);
        compteDestination.setTypeCompte("EPARGNE_JOURNALIERE");
        compteDestination.setNomCompte("Compte Destination Test");
        compteDestination.setVersion(0L);

        compteClient = new CompteClient();
        compteClient.setId(2L);
        compteClient.setNumeroCompte("CL001");
        compteClient.setSolde(500.0);
        compteClient.setTypeCompte("EPARGNE_JOURNALIERE");
        compteClient.setNomCompte("Compte Client Test");
        compteClient.setClient(client);
        compteClient.setVersion(0L);

        compteServiceCollecteur = new CompteCollecteur();
        compteServiceCollecteur.setId(1L);
        compteServiceCollecteur.setNumeroCompte("CO001");
        compteServiceCollecteur.setSolde(1000.0);
        compteServiceCollecteur.setTypeCompte("SERVICE");
        compteServiceCollecteur.setNomCompte("Compte Service Collecteur");
        compteServiceCollecteur.setCollecteur(collecteur);
        compteServiceCollecteur.setVersion(0L);

        compteAttenteCollecteur = new CompteCollecteur();
        compteAttenteCollecteur.setId(3L);
        compteAttenteCollecteur.setNumeroCompte("ATT001");
        compteAttenteCollecteur.setSolde(0.0);
        compteAttenteCollecteur.setTypeCompte("ATTENTE");
        compteAttenteCollecteur.setNomCompte("Compte Attente Collecteur");
        compteAttenteCollecteur.setCollecteur(collecteur);
        compteAttenteCollecteur.setVersion(0L);

        compteLiaison = new CompteLiaison();
        compteLiaison.setId(4L);
        compteLiaison.setNumeroCompte("LIA001");
        compteLiaison.setSolde(2000.0);
        compteLiaison.setTypeCompte("LIAISON");
        compteLiaison.setNomCompte("Compte Liaison Agence");
        compteLiaison.setAgence(agence);
        compteLiaison.setVersion(0L);

        // Configuration du journal
        journal = new Journal();
        journal.setId(1L);
        journal.setCollecteur(collecteur);
        journal.setDateDebut(LocalDate.now());
        journal.setDateFin(LocalDate.now());
        journal.setEstCloture(false);

        // Configuration du mouvement
        mouvement = new Mouvement();
        mouvement.setId(1L);
        mouvement.setCompteSource(compteSource);
        mouvement.setCompteDestination(compteDestination);
        mouvement.setMontant(200.0);
        mouvement.setSens("epargne");
        mouvement.setLibelle("Test mouvement");
        mouvement.setDateOperation(LocalDateTime.now());
        mouvement.setJournal(journal);
    }

    @Test
    public void testEffectuerMouvement_Success() {
        // Arrange
        when(compteRepository.findById(compteSource.getId())).thenReturn(Optional.of(compteSource));
        when(compteRepository.findById(compteDestination.getId())).thenReturn(Optional.of(compteDestination));
        when(compteRepository.save(any(Compte.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mouvementRepository.save(any(Mouvement.class))).thenReturn(mouvement);

        // Configurez le mock TransactionService pour exécuter directement la callback
        doAnswer(invocation -> {
            TransactionCallback<Mouvement> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            return callback.doInTransaction(status);
        }).when(transactionService).executeInTransaction(any(TransactionCallback.class));

        // Act
        Mouvement result = mouvementService.effectuerMouvement(mouvement);

        // Assert
        assertNotNull(result);
        assertEquals(mouvement.getId(), result.getId());
        assertEquals(mouvement.getMontant(), result.getMontant());
        verify(compteRepository, times(2)).save(any(Compte.class));
        verify(mouvementRepository).save(any(Mouvement.class));
    }

    @Test
    public void testEnregistrerEpargne_Success() {
        // Arrange
        double montant = 200.0;

        // Configurer les mocks pour retourner les comptes nécessaires
        when(compteClientRepository.findByClient(client)).thenReturn(Optional.of(compteClient));
        when(compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SERVICE"))
                .thenReturn(Optional.of(compteServiceCollecteur));

        // Mock du TransactionService pour exécuter directement la callback
        doAnswer(invocation -> {
            TransactionCallback<Mouvement> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            return callback.doInTransaction(status);
        }).when(transactionService).executeInTransaction(any(TransactionCallback.class));

        // Mock de effectuerMouvement pour retourner un mouvement avec succès
        MouvementService spyMouvementService = Mockito.spy(mouvementService);
        doReturn(mouvement).when(spyMouvementService).effectuerMouvement(any(Mouvement.class));

        // Act
        Mouvement result = spyMouvementService.enregistrerEpargne(client, montant, journal);

        // Assert
        assertNotNull(result);
        assertEquals(mouvement.getId(), result.getId());
        assertEquals(mouvement.getMontant(), result.getMontant());
        verify(spyMouvementService).effectuerMouvement(any(Mouvement.class));
        verify(epargneCounter).increment();
    }

    @Test
    public void testEnregistrerRetrait_Success() {
        // Arrange
        double montant = 200.0;

        // Configurer les mocks pour retourner les comptes nécessaires
        when(compteClientRepository.findByClient(client)).thenReturn(Optional.of(compteClient));
        when(compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SERVICE"))
                .thenReturn(Optional.of(compteServiceCollecteur));

        // Mock du TransactionService pour exécuter directement la callback
        doAnswer(invocation -> {
            TransactionCallback<Mouvement> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            return callback.doInTransaction(status);
        }).when(transactionService).executeInTransaction(any(TransactionCallback.class));

        // Créer un mouvement de retrait
        Mouvement mouvementRetrait = new Mouvement();
        mouvementRetrait.setId(2L);
        mouvementRetrait.setMontant(montant);
        mouvementRetrait.setSens("retrait");
        mouvementRetrait.setCompteSource(compteClient);
        mouvementRetrait.setCompteDestination(compteServiceCollecteur);
        mouvementRetrait.setJournal(journal);
        mouvementRetrait.setLibelle("Retrait client");
        mouvementRetrait.setDateOperation(LocalDateTime.now());

        // Mock de effectuerMouvement pour retourner un mouvement avec succès
        MouvementService spyMouvementService = Mockito.spy(mouvementService);
        doReturn(mouvementRetrait).when(spyMouvementService).effectuerMouvement(any(Mouvement.class));

        // Act
        Mouvement result = spyMouvementService.enregistrerRetrait(client, montant, journal);

        // Assert
        assertNotNull(result);
        assertEquals(mouvementRetrait.getId(), result.getId());
        assertEquals(mouvementRetrait.getMontant(), result.getMontant());
        assertEquals("retrait", result.getSens());
        verify(spyMouvementService).effectuerMouvement(any(Mouvement.class));
    }

    @Test
    public void testEnregistrerRetrait_SoldeInsuffisant() {
        // Arrange
        double montant = 1000.0; // Solde client est 500.0

        // Configurer les mocks pour retourner les comptes nécessaires
        when(compteClientRepository.findByClient(client)).thenReturn(Optional.of(compteClient));
        when(compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SERVICE"))
                .thenReturn(Optional.of(compteServiceCollecteur));

        // Mock du TransactionService pour propager l'exception
        doAnswer(invocation -> {
            TransactionCallback<Mouvement> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            try {
                return callback.doInTransaction(status);
            } catch (SoldeInsuffisantException e) {
                verify(status).setRollbackOnly();
                throw e;
            }
        }).when(transactionService).executeInTransaction(any(TransactionCallback.class));

        // Act & Assert
        assertThrows(SoldeInsuffisantException.class, () -> {
            mouvementService.enregistrerRetrait(client, montant, journal);
        });
    }

    @Test
    public void testEnregistrerRetrait_MontantMaxDepasse() {
        // Arrange
        double montant = 300000.0; // Max est 200000.0
        compteClient.setSolde(500000.0); // Solde suffisant

        // Configurer les mocks pour retourner les comptes nécessaires
        when(compteClientRepository.findByClient(client)).thenReturn(Optional.of(compteClient));
        when(compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SERVICE"))
                .thenReturn(Optional.of(compteServiceCollecteur));

        // Mock du TransactionService pour propager l'exception
        doAnswer(invocation -> {
            TransactionCallback<Mouvement> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            try {
                return callback.doInTransaction(status);
            } catch (MontantMaxRetraitException e) {
                verify(status).setRollbackOnly();
                throw e;
            }
        }).when(transactionService).executeInTransaction(any(TransactionCallback.class));

        // Act & Assert
        assertThrows(MontantMaxRetraitException.class, () -> {
            mouvementService.enregistrerRetrait(client, montant, journal);
        });
    }

    @Test
    public void testEnregistrerVersement_Success() {
        // Arrange
        double montant = 500.0;

        // Configurer les mocks pour retourner les comptes nécessaires
        when(compteLiaisonRepository.findByAgenceAndTypeCompte(agence, "LIAISON"))
                .thenReturn(Optional.of(compteLiaison));
        when(compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SERVICE"))
                .thenReturn(Optional.of(compteServiceCollecteur));

        // Mock du TransactionService pour exécuter directement la callback
        doAnswer(invocation -> {
            TransactionCallback<Mouvement> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            return callback.doInTransaction(status);
        }).when(transactionService).executeInTransaction(any(TransactionCallback.class));

        // Créer un mouvement de versement
        Mouvement mouvementVersement = new Mouvement();
        mouvementVersement.setId(3L);
        mouvementVersement.setMontant(montant);
        mouvementVersement.setSens("versement");
        mouvementVersement.setCompteSource(compteLiaison);
        mouvementVersement.setCompteDestination(compteServiceCollecteur);
        mouvementVersement.setJournal(journal);
        mouvementVersement.setLibelle("Versement en agence");
        mouvementVersement.setDateOperation(LocalDateTime.now());

        // Mock de effectuerMouvement pour retourner un mouvement avec succès
        MouvementService spyMouvementService = Mockito.spy(mouvementService);
        doReturn(mouvementVersement).when(spyMouvementService).effectuerMouvement(any(Mouvement.class));

        // Act
        Mouvement result = spyMouvementService.enregistrerVersement(collecteur, montant, journal);

        // Assert
        assertNotNull(result);
        assertEquals(mouvementVersement.getId(), result.getId());
        assertEquals(mouvementVersement.getMontant(), result.getMontant());
        assertEquals("versement", result.getSens());
        verify(spyMouvementService).effectuerMouvement(any(Mouvement.class));
    }

    @Test
    public void testTraiterCommissionsAsync_Success() {
        // Arrange
        when(commissionService.calculerCommission(any(Mouvement.class))).thenReturn(10.0);

        // Configurez les comptes systèmes pour le calcul des commissions
        Compte compteAttente = new Compte() {
            @Override
            public Long getId() { return 5L; }
            @Override
            public void setId(Long id) {}
            @Override
            public String getNomCompte() { return "Compte Attente Système"; }
            @Override
            public void setNomCompte(String nomCompte) {}
            @Override
            public String getNumeroCompte() { return "ATT-SYS"; }
            @Override
            public void setNumeroCompte(String numeroCompte) {}
            @Override
            public double getSolde() { return 0.0; }
            @Override
            public void setSolde(double solde) {}
            @Override
            public String getTypeCompte() { return "ATTENTE"; }
            @Override
            public void setTypeCompte(String typeCompte) {}
            @Override
            public Long getVersion() { return 0L; }
            @Override
            public void setVersion(Long version) {}
        };

        Compte compteTaxe = new Compte() {
            @Override
            public Long getId() { return 6L; }
            @Override
            public void setId(Long id) {}
            @Override
            public String getNomCompte() { return "Compte Taxe Système"; }
            @Override
            public void setNomCompte(String nomCompte) {}
            @Override
            public String getNumeroCompte() { return "TAXE-SYS"; }
            @Override
            public void setNumeroCompte(String numeroCompte) {}
            @Override
            public double getSolde() { return 0.0; }
            @Override
            public void setSolde(double solde) {}
            @Override
            public String getTypeCompte() { return "TAXE"; }
            @Override
            public void setTypeCompte(String typeCompte) {}
            @Override
            public Long getVersion() { return 0L; }
            @Override
            public void setVersion(Long version) {}
        };

        Compte compteProduit = new Compte() {
            @Override
            public Long getId() { return 7L; }
            @Override
            public void setId(Long id) {}
            @Override
            public String getNomCompte() { return "Compte Produit FOCEP"; }
            @Override
            public void setNomCompte(String nomCompte) {}
            @Override
            public String getNumeroCompte() { return "PROD-SYS"; }
            @Override
            public void setNumeroCompte(String numeroCompte) {}
            @Override
            public double getSolde() { return 0.0; }
            @Override
            public void setSolde(double solde) {}
            @Override
            public String getTypeCompte() { return "PRODUIT"; }
            @Override
            public void setTypeCompte(String typeCompte) {}
            @Override
            public Long getVersion() { return 0L; }
            @Override
            public void setVersion(Long version) {}
        };

        when(compteRepository.findByTypeCompte("ATTENTE")).thenReturn(Optional.of(compteAttente));
        when(compteRepository.findByTypeCompte("TAXE")).thenReturn(Optional.of(compteTaxe));
        when(compteRepository.findByTypeCompte("PRODUIT")).thenReturn(Optional.of(compteProduit));
        when(compteRepository.save(any(Compte.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mouvementRepository.save(any(Mouvement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock du TransactionService pour exécuter directement la callback
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            // Exécuter la callback immédiatement
            return callback.doInTransaction(status);
        }).when(transactionService).executeInNewTransaction(any(TransactionCallback.class));

        // Act
        MouvementService spyService = spy(mouvementService);
        spyService.traiterCommissionsAsync(mouvement);

        // Assert
        verify(commissionService).calculerCommission(mouvement);
        verify(compteRepository, atLeast(3)).save(any(Compte.class));
        verify(mouvementRepository, atLeast(3)).save(any(Mouvement.class));
    }

    @Test
    public void testCloturerJournee_Success() {
        // Arrange
        Journal journalACloturer = new Journal();
        journalACloturer.setId(1L);
        journalACloturer.setCollecteur(collecteur);
        journalACloturer.setDateDebut(LocalDate.now());
        journalACloturer.setDateFin(LocalDate.now());
        journalACloturer.setEstCloture(false);

        Journal journalCloture = new Journal();
        journalCloture.setId(1L);
        journalCloture.setCollecteur(collecteur);
        journalCloture.setDateDebut(LocalDate.now());
        journalCloture.setDateFin(LocalDate.now());
        journalCloture.setEstCloture(true);
        journalCloture.setDateCloture(LocalDateTime.now());

        when(journalService.saveJournal(any(Journal.class))).thenReturn(journalCloture);

        // Mock du TransactionService pour exécuter directement la callback
        doAnswer(invocation -> {
            TransactionCallback<Journal> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            return callback.doInTransaction(status);
        }).when(transactionService).executeInTransaction(any(TransactionCallback.class));

        // Act
        Journal result = mouvementService.cloturerJournee(collecteur, journalACloturer);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEstCloture());
        assertNotNull(result.getDateCloture());
        verify(journalService).saveJournal(any(Journal.class));
    }

    @Test
    public void testCloturerJournee_JournalNonValide() {
        // Arrange
        Collecteur autreCollecteur = new Collecteur();
        autreCollecteur.setId(2L);
        autreCollecteur.setNom("Autre Collecteur");
        autreCollecteur.setPrenom("Autre Prénom");

        Journal journalNonValide = new Journal();
        journalNonValide.setId(1L);
        journalNonValide.setCollecteur(autreCollecteur); // Collecteur différent
        journalNonValide.setDateDebut(LocalDate.now());
        journalNonValide.setDateFin(LocalDate.now());
        journalNonValide.setEstCloture(false);

        // Mock du TransactionService pour propager l'exception
        doAnswer(invocation -> {
            TransactionCallback<Journal> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            try {
                return callback.doInTransaction(status);
            } catch (BusinessException e) {
                verify(status).setRollbackOnly();
                throw e;
            }
        }).when(transactionService).executeInTransaction(any(TransactionCallback.class));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            mouvementService.cloturerJournee(collecteur, journalNonValide);
        });
    }

    @Test
    public void testFindByJournalId_Success() {
        // Arrange
        Mouvement mouvement1 = new Mouvement();
        mouvement1.setId(1L);
        mouvement1.setMontant(100.0);
        mouvement1.setSens("epargne");

        Mouvement mouvement2 = new Mouvement();
        mouvement2.setId(2L);
        mouvement2.setMontant(200.0);
        mouvement2.setSens("retrait");

        List<Mouvement> mouvementsAttendus = Arrays.asList(mouvement1, mouvement2);

        when(mouvementRepository.findByJournalId(1L)).thenReturn(mouvementsAttendus);

        // Act
        List<Mouvement> result = mouvementService.findByJournalId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(mouvementsAttendus, result);
        verify(mouvementRepository).findByJournalId(1L);
    }

    @Test
    public void testCompteNotFound_ThrowsException() {
        // Arrange
        Compte compteInvalide = new CompteClient();
        compteInvalide.setId(999L);

        Mouvement mouvementInvalide = new Mouvement();
        mouvementInvalide.setId(99L);
        mouvementInvalide.setCompteSource(compteInvalide);
        mouvementInvalide.setCompteDestination(compteDestination);
        mouvementInvalide.setMontant(100.0);
        mouvementInvalide.setSens("epargne");

        when(compteRepository.findById(999L)).thenReturn(Optional.empty());

        // Mock du TransactionService pour propager l'exception
        doAnswer(invocation -> {
            TransactionCallback<Mouvement> callback = invocation.getArgument(0);
            TransactionStatus status = mock(TransactionStatus.class);
            try {
                return callback.doInTransaction(status);
            } catch (BusinessException e) {
                verify(status).setRollbackOnly();
                throw e;
            }
        }).when(transactionService).executeInTransaction(any(TransactionCallback.class));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            mouvementService.effectuerMouvement(mouvementInvalide);
        });

        // Vérifier que la cause sous-jacente est CompteNotFoundException
        assertTrue(exception.getMessage().contains("Erreur lors de l'exécution du mouvement"));
    }
}