package org.example.collectfocep.collectfocep.services;

import io.micrometer.core.instrument.Timer;
import static org.mockito.ArgumentMatchers.any;
import org.example.collectfocep.dto.CommissionResult;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.CommissionCalculationService;
import org.example.collectfocep.services.MetricsService;
import org.example.collectfocep.services.impl.MouvementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommissionCalculationServiceTest {

    @Mock
    private CommissionParameterRepository commissionParameterRepository;

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private MouvementService mouvementService;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CollecteurRepository collecteurRepository;

    @Mock
    private MouvementRepository mouvementRepository;

    @Mock
    private CompteClientRepository compteClientRepository;

    @Mock
    private Timer commissionTimer;

    @InjectMocks
    private CommissionCalculationService commissionCalculationService;

    @Mock
    private MetricsService metricsService;

    private Client client;
    private Collecteur collecteur;
    private Agence agence;
    private CompteClient compteClient;
    private Compte compteAttente;
    private Compte compteTaxe;
    private Compte compteProduit;
    private CommissionParameter commissionParameterFixed;
    private CommissionParameter commissionParameterPercentage;
    private CommissionParameter commissionParameterTier;
    private List<CommissionTier> commissionTiers;

    @BeforeEach
    void setUp() {
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
        collecteur.setAncienneteEnMois(5);

        // Configuration du client
        client = new Client();
        client.setId(1L);
        client.setNom("Nom Client");
        client.setPrenom("Prénom Client");
        client.setAgence(agence);
        client.setCollecteur(collecteur);

        // Configuration du compte client
        compteClient = new CompteClient();
        compteClient.setId(1L);
        compteClient.setNumeroCompte("CL001");
        compteClient.setSolde(2000.0);
        compteClient.setClient(client);

        // Configuration des comptes spéciaux
        compteAttente = mock(Compte.class);
        compteAttente.setId(2L);
        compteAttente.setNumeroCompte("ATT001");
        compteAttente.setSolde(0.0);
        compteAttente.setTypeCompte("ATTENTE");

        compteTaxe = mock(Compte.class);
        compteTaxe.setId(3L);
        compteTaxe.setNumeroCompte("TAX001");
        compteTaxe.setSolde(0.0);
        compteTaxe.setTypeCompte("TAXE");

        compteProduit = mock(Compte.class);
        compteProduit.setId(4L);
        compteProduit.setNumeroCompte("PROD001");
        compteProduit.setSolde(0.0);
        compteProduit.setTypeCompte("PRODUIT");

        // Configuration des paramètres de commission
        commissionParameterFixed = new CommissionParameter();
        commissionParameterFixed.setId(1L);
        commissionParameterFixed.setType(CommissionType.FIXED);
        commissionParameterFixed.setValeur(50.0); // 50 FCFA de commission fixe
        commissionParameterFixed.setActive(true);

        commissionParameterPercentage = new CommissionParameter();
        commissionParameterPercentage.setId(2L);
        commissionParameterPercentage.setType(CommissionType.PERCENTAGE);
        commissionParameterPercentage.setValeur(2.0); // 2% de commission
        commissionParameterPercentage.setActive(true);

        // Configuration des paliers de commission
        commissionTiers = new ArrayList<>();

        CommissionTier tier1 = new CommissionTier();
        tier1.setId(1L);
        tier1.setMontantMin(0.0);
        tier1.setMontantMax(1000.0);
        tier1.setTaux(3.0); // 3%

        CommissionTier tier2 = new CommissionTier();
        tier2.setId(2L);
        tier2.setMontantMin(1000.0);
        tier2.setMontantMax(5000.0);
        tier2.setTaux(2.0); // 2%

        CommissionTier tier3 = new CommissionTier();
        tier3.setId(3L);
        tier3.setMontantMin(5000.0);
        tier3.setMontantMax(Double.MAX_VALUE);
        tier3.setTaux(1.0); // 1%

        commissionTiers.addAll(Arrays.asList(tier1, tier2, tier3));

        commissionParameterTier = new CommissionParameter();
        commissionParameterTier.setId(3L);
        commissionParameterTier.setType(CommissionType.TIER);
        commissionParameterTier.setTiers(commissionTiers);
        commissionParameterTier.setActive(true);

        lenient().when(compteRepository.findByTypeCompte("ATTENTE")).thenReturn(Optional.of(compteAttente));
        lenient().when(compteRepository.findByTypeCompte("TAXE")).thenReturn(Optional.of(compteTaxe));
    }

    @Test
    void testCalculateCommissionForClient_FixedCommission() {
        // Arrange
        when(commissionParameterRepository.findByClient(client)).thenReturn(Optional.of(commissionParameterFixed));
        when(compteClientRepository.findByClient(client)).thenReturn(Optional.of(compteClient));
        when(compteRepository.findByTypeCompte("ATTENTE")).thenReturn(Optional.of(compteAttente));
        when(compteRepository.findByTypeCompte("TAXE")).thenReturn(Optional.of(compteTaxe));
        when(compteRepository.save(any(Compte.class))).thenAnswer(i -> i.getArgument(0));
        when(mouvementRepository.save(any(Mouvement.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        CommissionResult result = commissionCalculationService.calculateCommissionForClient(
                client, LocalDate.now(), LocalDate.now().plusDays(30), 1000.0);

        // Assert
        assertNotNull(result);
        assertEquals(50.0, result.getMontantCommission());
        assertEquals(50.0 * 0.1925, result.getMontantTVA(), 0.001);
        assertEquals(50.0 - (50.0 * 0.1925), result.getMontantNet(), 0.001);
        assertEquals("FIXED", result.getTypeCalcul());

        // Vérifier que les comptes ont été mis à jour
        assertEquals(2000.0 - 50.0 - (50.0 * 0.1925), compteClient.getSolde(), 0.001);
        assertEquals(50.0, compteAttente.getSolde());
        assertEquals(50.0 * 0.1925, compteTaxe.getSolde(), 0.001);

        // Vérifier que les mouvements ont été créés
        verify(mouvementRepository, times(2)).save(any(Mouvement.class));
    }

    @Test
    void testCalculateCommissionForClient_PercentageCommission() {
        // Arrange
        when(commissionParameterRepository.findByClient(client)).thenReturn(Optional.of(commissionParameterPercentage));
        when(compteClientRepository.findByClient(client)).thenReturn(Optional.of(compteClient));
        when(compteRepository.findByTypeCompte("ATTENTE")).thenReturn(Optional.of(compteAttente));
        when(compteRepository.findByTypeCompte("TAXE")).thenReturn(Optional.of(compteTaxe));
        when(compteRepository.save(any(Compte.class))).thenAnswer(i -> i.getArgument(0));
        when(mouvementRepository.save(any(Mouvement.class))).thenAnswer(i -> i.getArgument(0));

        double montantTotal = 1000.0;
        double commissionAttendue = montantTotal * (2.0 / 100.0); // 2% de 1000 = 20
        double tvaAttendue = commissionAttendue * 0.1925; // 19.25% de 20 = 3.85

        // Act
        CommissionResult result = commissionCalculationService.calculateCommissionForClient(
                client, LocalDate.now(), LocalDate.now().plusDays(30), montantTotal);

        // Assert
        assertNotNull(result);
        assertEquals(commissionAttendue, result.getMontantCommission());
        assertEquals(tvaAttendue, result.getMontantTVA(), 0.001);
        assertEquals(commissionAttendue - tvaAttendue, result.getMontantNet(), 0.001);
        assertEquals("PERCENTAGE", result.getTypeCalcul());

        // Vérifier que les comptes ont été mis à jour
        assertEquals(2000.0 - commissionAttendue - tvaAttendue, compteClient.getSolde(), 0.001);
        assertEquals(commissionAttendue, compteAttente.getSolde());
        assertEquals(tvaAttendue, compteTaxe.getSolde(), 0.001);

        // Vérifier que les mouvements ont été créés
        verify(mouvementRepository, times(2)).save(any(Mouvement.class));
    }

    @Test
    void testCalculateCommissionForClient_TierCommission() {
        // Arrange
        when(commissionParameterRepository.findByClient(client)).thenReturn(Optional.of(commissionParameterTier));
        when(compteClientRepository.findByClient(client)).thenReturn(Optional.of(compteClient));
        when(compteRepository.findByTypeCompte("ATTENTE")).thenReturn(Optional.of(compteAttente));
        when(compteRepository.findByTypeCompte("TAXE")).thenReturn(Optional.of(compteTaxe));
        when(compteRepository.save(any(Compte.class))).thenAnswer(i -> i.getArgument(0));
        when(mouvementRepository.save(any(Mouvement.class))).thenAnswer(i -> i.getArgument(0));

        double montantTotal = 3000.0; // Tombe dans le deuxième palier (2%)
        double commissionAttendue = montantTotal * (2.0 / 100.0); // 2% de 3000 = 60
        double tvaAttendue = commissionAttendue * 0.1925; // 19.25% de 60 = 11.55

        // Act
        CommissionResult result = commissionCalculationService.calculateCommissionForClient(
                client, LocalDate.now(), LocalDate.now().plusDays(30), montantTotal);

        // Assert
        assertNotNull(result);
        assertEquals(commissionAttendue, result.getMontantCommission());
        assertEquals(tvaAttendue, result.getMontantTVA(), 0.001);
        assertEquals(commissionAttendue - tvaAttendue, result.getMontantNet(), 0.001);
        assertEquals("TIER", result.getTypeCalcul());

        // Vérifier que les comptes ont été mis à jour
        assertEquals(2000.0 - commissionAttendue - tvaAttendue, compteClient.getSolde(), 0.001);
        assertEquals(commissionAttendue, compteAttente.getSolde());
        assertEquals(tvaAttendue, compteTaxe.getSolde(), 0.001);

        // Vérifier que les mouvements ont été créés
        verify(mouvementRepository, times(2)).save(any(Mouvement.class));
    }

    @Test
    void testGetCommissionParameters_PrioritéClient() {
        // Arrange
        when(commissionParameterRepository.findByClient(client)).thenReturn(Optional.of(commissionParameterFixed));

        // Act
        CommissionParameter result = commissionCalculationService.getCommissionParameters(client);

        // Assert
        assertNotNull(result);
        assertEquals(CommissionType.FIXED, result.getType());
        assertEquals(50.0, result.getValeur());

        // Vérifier que seule la méthode findByClient a été appelée
        verify(commissionParameterRepository).findByClient(client);
        verify(commissionParameterRepository, never()).findByCollecteur(any());
        verify(commissionParameterRepository, never()).findByAgence(any());
    }

    @Test
    void testGetCommissionParameters_PrioritéCollecteur() {
        // Arrange
        when(commissionParameterRepository.findByClient(client)).thenReturn(Optional.empty());
        when(commissionParameterRepository.findByCollecteur(client.getCollecteur())).thenReturn(Optional.of(commissionParameterPercentage));

        // Act
        CommissionParameter result = commissionCalculationService.getCommissionParameters(client);

        // Assert
        assertNotNull(result);
        assertEquals(CommissionType.PERCENTAGE, result.getType());
        assertEquals(2.0, result.getValeur());

        // Vérifier l'ordre d'appel des méthodes
        verify(commissionParameterRepository).findByClient(client);
        verify(commissionParameterRepository).findByCollecteur(client.getCollecteur());
        verify(commissionParameterRepository, never()).findByAgence(any());
    }

    @Test
    void testGetCommissionParameters_PrioritéAgence() {
        // Arrange
        when(commissionParameterRepository.findByClient(client)).thenReturn(Optional.empty());
        when(commissionParameterRepository.findByCollecteur(client.getCollecteur())).thenReturn(Optional.empty());
        when(commissionParameterRepository.findByAgence(client.getAgence())).thenReturn(Optional.of(commissionParameterTier));

        // Act
        CommissionParameter result = commissionCalculationService.getCommissionParameters(client);

        // Assert
        assertNotNull(result);
        assertEquals(CommissionType.TIER, result.getType());
        assertNotNull(result.getTiers());
        assertEquals(3, result.getTiers().size());

        // Vérifier l'ordre d'appel des méthodes
        verify(commissionParameterRepository).findByClient(client);
        verify(commissionParameterRepository).findByCollecteur(client.getCollecteur());
        verify(commissionParameterRepository).findByAgence(client.getAgence());
    }

    @Test
    void testCalculateCommissions_PourCollecteur() {
        // Arrange
        List<Client> clients = Arrays.asList(client);
        collecteur.setClients(clients);
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();

        when(collecteurRepository.findById(collecteur.getId())).thenReturn(Optional.of(collecteur));
        // Mock pour simuler un montant collecté pour le client
        when(mouvementRepository.sumAmountByClientAndPeriod(
                client.getId(), startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay().minusSeconds(1)))
                .thenReturn(5000.0);

        // Mock pour calculateCommissionForClient
        // On utilise doReturn().when() pour éviter d'appeler la vraie méthode
        CommissionResult clientResult = new CommissionResult();
        clientResult.setMontantCommission(100.0);
        clientResult.setMontantTVA(19.25);
        clientResult.setMontantNet(80.75);
        clientResult.setTypeCalcul("PERCENTAGE");
        clientResult.setDateCalcul(LocalDateTime.now());
        clientResult.setClientId(client.getId());
        clientResult.setCollecteurId(collecteur.getId());

        doReturn(clientResult).when(commissionCalculationService)
                .calculateCommissionForClient(eq(client), eq(startDate), eq(endDate), anyDouble());

        // Act
        CommissionResult result = commissionCalculationService.calculateCommissions(
                collecteur.getId(), startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(100.0, result.getMontantCommission());
        assertEquals(19.25, result.getMontantTVA());
        assertEquals(80.75, result.getMontantNet());
        assertEquals("COLLECTEUR_GLOBAL", result.getTypeCalcul());
        assertEquals(collecteur.getId(), result.getCollecteurId());

        // Vérifier que les méthodes ont été appelées
        verify(collecteurRepository).findById(collecteur.getId());
        verify(mouvementRepository).sumAmountByClientAndPeriod(
                client.getId(), startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay().minusSeconds(1));
        verify(commissionCalculationService).calculateCommissionForClient(
                eq(client), eq(startDate), eq(endDate), eq(5000.0));
    }

    @Test
    void testCalculateCommissions_PasDeClient() {
        // Arrange
        collecteur.setClients(new ArrayList<>());
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();

        when(collecteurRepository.findById(collecteur.getId())).thenReturn(Optional.of(collecteur));

        // Act
        CommissionResult result = commissionCalculationService.calculateCommissions(
                collecteur.getId(), startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getMontantCommission());
        assertEquals(0.0, result.getMontantTVA());
        assertEquals(0.0, result.getMontantNet());
        assertEquals("COLLECTEUR_GLOBAL", result.getTypeCalcul());
        assertEquals(collecteur.getId(), result.getCollecteurId());

        // Vérifier qu'aucune autre méthode n'a été appelée
        verify(collecteurRepository).findById(collecteur.getId());
        verify(mouvementRepository, never()).sumAmountByClientAndPeriod(anyLong(), any(), any());
        verify(commissionCalculationService, never()).calculateCommissionForClient(any(), any(), any(), anyDouble());
    }

    @Test
    void testCalculateCollecteurRemuneration_NouveauCollecteur() {
        // Arrange
        collecteur.setAncienneteEnMois(2); // Moins de 3 mois
        double totalCommissions = 100000.0; // Total des commissions élevé

        // La méthode est privée, nous devons donc la tester indirectement
        // Pour cela, on peut :
        // 1. Utiliser la réflexion pour accéder à la méthode privée
        // 2. Tester via une méthode publique qui l'utilise
        // 3. Créer une sous-classe de test qui expose la méthode

        // Option 2: Nous testons via le calcul global des commissions
        when(collecteurRepository.findById(collecteur.getId())).thenReturn(Optional.of(collecteur));
        when(mouvementRepository.sumAmountByClientAndPeriod(anyLong(), any(), any())).thenReturn(0.0);

        // Act
        CommissionResult result = commissionCalculationService.calculateCommissions(collecteur.getId(),
                LocalDate.now(),
                LocalDate.now());

        // Le test ici est moins direct car la méthode est privée
        // On peut tester le comportement via d'autres méthodes publiques qui l'utilisent
        // ou refactoriser le code pour rendre la méthode testable

        // Assert
        assertEquals(collecteur.getId(), result.getCollecteurId());
    }

    @Test
    void testCalculateCollecteurRemuneration_CollecteurExperimente() {
        // Arrange
        collecteur.setAncienneteEnMois(6); // Plus de 3 mois
        double totalCommissions = 100000.0;

        // Voir commentaires du test précédent sur les méthodes privées

        // Option 2: Nous testons via le calcul global des commissions
        when(collecteurRepository.findById(collecteur.getId())).thenReturn(Optional.of(collecteur));
        when(mouvementRepository.sumAmountByClientAndPeriod(anyLong(), any(), any())).thenReturn(0.0);

        // Act
        CommissionResult result = commissionCalculationService.calculateCommissions(collecteur.getId(),
                LocalDate.now(),
                LocalDate.now());

        // Assert
        assertEquals(collecteur.getId(), result.getCollecteurId());
    }
}