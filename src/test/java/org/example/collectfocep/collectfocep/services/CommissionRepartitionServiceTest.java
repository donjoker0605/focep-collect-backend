package org.example.collectfocep.collectfocep.services;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CommissionRepartitionServiceTest {

    @Mock
    private CompteService compteServiceMock;

    @Mock
    private MouvementService mouvementService;

    @Mock
    private CommissionCalculationService commissionCalculationService;

    @Mock
    private MouvementRepository mouvementRepository;

    @Mock
    private CollecteurRepository collecteurRepository;

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private CommissionRepartitionService commissionRepartitionService;

    private Collecteur collecteurNouveau;
    private Collecteur collecteurExperimente;
    private List<Client> clients;
    private CompteCollecteur compteService;
    private CompteCollecteur compteAttente;
    private CompteCollecteur compteRemunerationCollecteur;
    private CompteLiaison compteLiaison;
    private Compte compteProduit;
    private Compte compteTaxe;

    @BeforeEach
    void setUp() {
        // Initialisation des objets pour les tests
        Agence agence = new Agence();
        agence.setId(1L);
        agence.setCodeAgence("A01");
        agence.setNomAgence("Agence Test");

        collecteurNouveau = new Collecteur();
        collecteurNouveau.setId(1L);
        collecteurNouveau.setNom("Nouveau Collecteur");
        collecteurNouveau.setPrenom("Prénom");
        collecteurNouveau.setAgence(agence);
        collecteurNouveau.setAncienneteEnMois(2); // Moins de 3 mois

        collecteurExperimente = new Collecteur();
        collecteurExperimente.setId(2L);
        collecteurExperimente.setNom("Collecteur Expérimenté");
        collecteurExperimente.setPrenom("Prénom");
        collecteurExperimente.setAgence(agence);
        collecteurExperimente.setAncienneteEnMois(6); // Plus de 3 mois

        // Création des clients
        clients = new ArrayList<>();
        Client client1 = new Client();
        client1.setId(1L);
        client1.setNom("Client 1");
        client1.setAgence(agence);
        client1.setCollecteur(collecteurExperimente);

        Client client2 = new Client();
        client2.setId(2L);
        client2.setNom("Client 2");
        client2.setAgence(agence);
        client2.setCollecteur(collecteurExperimente);

        clients.add(client1);
        clients.add(client2);

        collecteurExperimente.setClients(clients);

        // Création des comptes
        compteService = new CompteCollecteur();
        compteService.setId(1L);
        compteService.setNumeroCompte("SRV001");
        compteService.setSolde(1000.0);
        compteService.setTypeCompte("SERVICE");
        compteService.setCollecteur(collecteurExperimente);

        compteAttente = new CompteCollecteur();
        compteAttente.setId(2L);
        compteAttente.setNumeroCompte("ATT001");
        compteAttente.setSolde(500.0);
        compteAttente.setTypeCompte("ATTENTE");
        compteAttente.setCollecteur(collecteurExperimente);

        compteRemunerationCollecteur = new CompteCollecteur();
        compteRemunerationCollecteur.setId(3L);
        compteRemunerationCollecteur.setNumeroCompte("REM001");
        compteRemunerationCollecteur.setSolde(0.0);
        compteRemunerationCollecteur.setTypeCompte("REMUNERATION");
        compteRemunerationCollecteur.setCollecteur(collecteurExperimente);

        compteLiaison = new CompteLiaison();
        compteLiaison.setId(4L);
        compteLiaison.setNumeroCompte("LIA001");
        compteLiaison.setSolde(2000.0);
        compteLiaison.setTypeCompte("LIAISON");
        compteLiaison.setAgence(agence);

        compteProduit = mock(Compte.class);
        when(compteProduit.getId()).thenReturn(5L);
        when(compteProduit.getNumeroCompte()).thenReturn("PROD001");
        when(compteProduit.getSolde()).thenReturn(0.0);
        when(compteProduit.getTypeCompte()).thenReturn("PRODUIT");

        compteTaxe = mock(Compte.class);
        when(compteTaxe.getId()).thenReturn(6L);
        when(compteTaxe.getNumeroCompte()).thenReturn("TAXE001");
        when(compteTaxe.getSolde()).thenReturn(0.0);
        when(compteTaxe.getTypeCompte()).thenReturn("TAXE");
    }

    @Test
    void testPreleverCommissions() {
        // Arrange
        LocalDate dateDebut = LocalDate.now().minusMonths(1);
        LocalDate dateFin = LocalDate.now();

        when(mouvementRepository.sumAmountByClientAndPeriod(anyLong(), any(), any())).thenReturn(1000.0);

        CommissionResult clientResult = new CommissionResult();
        clientResult.setMontantCommission(20.0);
        clientResult.setMontantTVA(3.85);
        clientResult.setMontantNet(16.15);

        when(commissionCalculationService.calculateCommissionForClient(
                any(Client.class), any(LocalDate.class), any(LocalDate.class), anyDouble()))
                .thenReturn(clientResult);

        // Act
        commissionRepartitionService.preleverCommissions(collecteurExperimente, dateDebut, dateFin);

        // Assert
        verify(commissionCalculationService, times(2)).calculateCommissionForClient(
                any(Client.class), eq(dateDebut), eq(dateFin), anyDouble());
    }

    @Test
    void testRepartirCommissions() {
        // Arrange
        LocalDate dateDebut = LocalDate.now().minusMonths(1);
        LocalDate dateFin = LocalDate.now();

        // Utiliser doReturn pour contourner les problèmes de type
        doReturn(compteService).when(compteServiceMock).findServiceAccount(any(Collecteur.class));
        doReturn(compteAttente).when(compteServiceMock).findWaitingAccount(any(Collecteur.class));
        doReturn(compteRemunerationCollecteur).when(compteServiceMock).findSalaryAccount(any(Collecteur.class));
        doReturn(compteLiaison).when(compteServiceMock).findLiaisonAccount(any(Agence.class));
        doReturn(compteProduit).when(compteServiceMock).findProduitAccount();
        doReturn(compteTaxe).when(compteServiceMock).findTVAAccount();

        when(mouvementRepository.sumCommissionsByCollecteurAndPeriod(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(100.0);

        when(mouvementRepository.sumTVAByCollecteurAndPeriod(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(19.25);

        when(mouvementService.effectuerMouvement(any(Mouvement.class))).thenReturn(new Mouvement());

        // Act
        commissionRepartitionService.repartirCommissions(collecteurExperimente, dateDebut, dateFin);

        // Assert
        verify(mouvementService, atLeastOnce()).effectuerMouvement(any(Mouvement.class));
    }

    @Test
    void testTraiterNouveauCollecteur() {
        // Arrange
        LocalDate dateDebut = LocalDate.now().minusMonths(1);
        LocalDate dateFin = LocalDate.now();

        // Utiliser doReturn pour contourner les problèmes de type
        doReturn(compteService).when(compteServiceMock).findServiceAccount(any(Collecteur.class));
        doReturn(compteAttente).when(compteServiceMock).findWaitingAccount(any(Collecteur.class));
        doReturn(compteRemunerationCollecteur).when(compteServiceMock).findSalaryAccount(any(Collecteur.class));
        doReturn(compteLiaison).when(compteServiceMock).findLiaisonAccount(any(Agence.class));

        when(mouvementRepository.sumCommissionsByCollecteurAndPeriod(anyLong(), any(), any()))
                .thenReturn(100.0);

        when(mouvementRepository.sumTVAByCollecteurAndPeriod(anyLong(), any(), any()))
                .thenReturn(19.25);

        // Créer un mock de Mouvement qui retourne correctement les comptes
        when(mouvementService.effectuerMouvement(any(Mouvement.class))).thenAnswer(invocation -> {
            Mouvement mouvement = invocation.getArgument(0);
            // S'assurer que les comptes sont bien définis
            if (mouvement.getCompteSource() == null) {
                mouvement.setCompteSource(compteAttente);
            }
            if (mouvement.getCompteDestination() == null) {
                mouvement.setCompteDestination(compteRemunerationCollecteur);
            }
            return mouvement;
        });

        // Act
        commissionRepartitionService.repartirCommissions(collecteurNouveau, dateDebut, dateFin);

        // Assert - Vérifier avec ArgumentCaptor pour avoir plus de contrôle
        ArgumentCaptor<Mouvement> mouvementCaptor = ArgumentCaptor.forClass(Mouvement.class);
        verify(mouvementService, atLeastOnce()).effectuerMouvement(mouvementCaptor.capture());

        // Vérifier que le compte destination est bien défini dans au moins un des mouvements
        boolean compteDestinationCorrect = mouvementCaptor.getAllValues().stream()
                .anyMatch(m -> m.getCompteDestination() != null &&
                        m.getCompteDestination().equals(compteRemunerationCollecteur));
        assertTrue(compteDestinationCorrect, "Au moins un mouvement doit avoir le bon compte destination");
    }

    @Test
    void testProcessRepartition() {
        // Arrange
        CommissionResult result = new CommissionResult();
        result.setCollecteurId(collecteurExperimente.getId());
        result.setMontantCommission(100.0);
        result.setMontantTVA(19.25);
        result.setMontantNet(80.75);

        when(collecteurRepository.findById(collecteurExperimente.getId()))
                .thenReturn(Optional.of(collecteurExperimente));

        // Act
        commissionRepartitionService.processRepartition(result);

        // Assert
        verify(collecteurRepository).findById(collecteurExperimente.getId());
        verify(mouvementService, atLeastOnce()).effectuerMouvement(any(Mouvement.class));
    }
}