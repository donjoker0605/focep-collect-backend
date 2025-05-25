package org.example.collectfocep.collectfocep.services;

import org.example.collectfocep.dto.TransferDetailDTO;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.CompteTransferService;
import org.example.collectfocep.services.impl.MouvementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompteTransferServiceTest {

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private CompteClientRepository compteClientRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CollecteurRepository collecteurRepository;

    @Mock
    private MouvementRepository mouvementRepository;

    @Mock
    private MouvementServiceImpl mouvementServiceImpl;

    @Mock
    private CompteLiaisonRepository compteLiaisonRepository;

    @Mock
    private TransfertCompteRepository transfertCompteRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private CompteTransferService compteTransferService;

    private Collecteur sourceCollecteur;
    private Collecteur targetCollecteur;
    private Client client;
    private Agence agence1;
    private Agence agence2;
    private CompteClient compteClient;
    private TransfertCompte transfert;
    private List<Client> clients;
    private List<Mouvement> mouvements;

    @BeforeEach
    void setUp() {
        // Configuration des agences
        agence1 = new Agence();
        agence1.setId(1L);
        agence1.setCodeAgence("A01");
        agence1.setNomAgence("Agence 1");

        agence2 = new Agence();
        agence2.setId(2L);
        agence2.setCodeAgence("A02");
        agence2.setNomAgence("Agence 2");

        // Configuration des collecteurs
        sourceCollecteur = new Collecteur();
        sourceCollecteur.setId(1L);
        sourceCollecteur.setNom("Source Collecteur");
        sourceCollecteur.setPrenom("Test");
        sourceCollecteur.setAgence(agence1);

        targetCollecteur = new Collecteur();
        targetCollecteur.setId(2L);
        targetCollecteur.setNom("Target Collecteur");
        targetCollecteur.setPrenom("Test");
        targetCollecteur.setAgence(agence2);

        // Configuration du client
        client = new Client();
        client.setId(1L);
        client.setNom("Client");
        client.setPrenom("Test");
        client.setCollecteur(sourceCollecteur);
        client.setAgence(agence1);

        clients = new ArrayList<>();
        clients.add(client);

        // Configuration du compte client
        compteClient = new CompteClient();
        compteClient.setId(1L);
        compteClient.setNumeroCompte("CL001");
        compteClient.setSolde(1000.0);
        compteClient.setClient(client);

        // Configuration du transfert
        transfert = new TransfertCompte();
        transfert.setId(1L);
        transfert.setSourceCollecteurId(sourceCollecteur.getId());
        transfert.setTargetCollecteurId(targetCollecteur.getId());
        transfert.setNombreComptes(1);
        transfert.setMontantTotal(1000.0);
        transfert.setMontantCommissions(50.0);

        // Configuration des mouvements
        Mouvement mouvement = new Mouvement();
        mouvement.setId(1L);
        mouvement.setMontant(1000.0);
        mouvement.setSens("TRANSFERT");
        mouvement.setLibelle("Transfert inter-agences");
        mouvement.setTransfert(transfert);

        mouvements = new ArrayList<>();
        mouvements.add(mouvement);
    }

    @Test
    void testTransferComptes_SameAgence() {
        // Arrange
        targetCollecteur.setAgence(agence1); // Même agence que sourceCollecteur
        List<Long> clientIds = Arrays.asList(1L);

        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(sourceCollecteur));
        when(collecteurRepository.findById(2L)).thenReturn(Optional.of(targetCollecteur));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(client));
        when(compteClientRepository.findByClient(client)).thenReturn(Optional.of(compteClient));
        when(clientRepository.save(any(Client.class))).thenReturn(client);

        // Act
        int result = compteTransferService.transferComptes(
                sourceCollecteur.getId(), targetCollecteur.getId(), clientIds);

        // Assert
        assertEquals(1, result);
        assertEquals(targetCollecteur, client.getCollecteur());
        verify(clientRepository).save(client);
        // Vérifier que handleInterAgencyTransfer n'est PAS appelée (même agence)
        verify(compteLiaisonRepository, never()).findByAgenceAndTypeCompte(any(), any());
    }

    @Test
    void testTransferComptes_DifferentAgences() {
        // Arrange
        List<Long> clientIds = Arrays.asList(1L);
        CompteLiaison compteLiaisonSource = new CompteLiaison();
        compteLiaisonSource.setId(3L);
        compteLiaisonSource.setAgence(agence1);
        compteLiaisonSource.setTypeCompte("LIAISON");

        CompteLiaison compteLiaisonTarget = new CompteLiaison();
        compteLiaisonTarget.setId(4L);
        compteLiaisonTarget.setAgence(agence2);
        compteLiaisonTarget.setTypeCompte("LIAISON");

        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(sourceCollecteur));
        when(collecteurRepository.findById(2L)).thenReturn(Optional.of(targetCollecteur));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(client));
        when(compteClientRepository.findByClient(client)).thenReturn(Optional.of(compteClient));
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(compteLiaisonRepository.findByAgenceAndTypeCompte(agence1, "LIAISON"))
                .thenReturn(Optional.of(compteLiaisonSource));
        when(compteLiaisonRepository.findByAgenceAndTypeCompte(agence2, "LIAISON"))
                .thenReturn(Optional.of(compteLiaisonTarget));
        when(mouvementServiceImpl.effectuerMouvement(any(Mouvement.class))).thenReturn(new Mouvement());
        when(mouvementRepository.calculatePendingCommissions(anyLong(), anyLong(), any()))
                .thenReturn(50.0);

        // Act
        int result = compteTransferService.transferComptes(
                sourceCollecteur.getId(), targetCollecteur.getId(), clientIds);

        // Assert
        assertEquals(1, result);
        assertEquals(targetCollecteur, client.getCollecteur());
        verify(clientRepository).save(client);
        // Vérifier que handleInterAgencyTransfer EST appelée (agences différentes)
        verify(compteLiaisonRepository).findByAgenceAndTypeCompte(agence1, "LIAISON");
        verify(compteLiaisonRepository).findByAgenceAndTypeCompte(agence2, "LIAISON");
        verify(mouvementServiceImpl).effectuerMouvement(any(Mouvement.class));
    }

    @Test
    void testTransferComptes_ClientNotFound() {
        // Arrange
        List<Long> clientIds = Arrays.asList(999L);

        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(sourceCollecteur));
        when(collecteurRepository.findById(2L)).thenReturn(Optional.of(targetCollecteur));
        when(clientRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        // Act
        int result = compteTransferService.transferComptes(
                sourceCollecteur.getId(), targetCollecteur.getId(), clientIds);

        // Assert
        assertEquals(0, result);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void testTransferComptes_WrongSourceCollecteur() {
        // Arrange
        List<Long> clientIds = Arrays.asList(1L);
        client.setCollecteur(targetCollecteur); // Client appartient au collecteur cible, pas source

        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(sourceCollecteur));
        when(collecteurRepository.findById(2L)).thenReturn(Optional.of(targetCollecteur));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(client));

        // Act
        int result = compteTransferService.transferComptes(
                sourceCollecteur.getId(), targetCollecteur.getId(), clientIds);

        // Assert
        assertEquals(0, result);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void testGetTransferDetails() {
        // Arrange
        when(transfertCompteRepository.findById(1L)).thenReturn(Optional.of(transfert));
        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(sourceCollecteur));
        when(collecteurRepository.findById(2L)).thenReturn(Optional.of(targetCollecteur));
        when(clientRepository.findByTransfertId(1L)).thenReturn(clients);
        when(mouvementRepository.findByTransfertId(1L)).thenReturn(mouvements);
        when(auditLogRepository.findByEntityTypeAndEntityId("TRANSFER", 1L))
                .thenReturn(new ArrayList<>());

        // Act
        TransferDetailDTO result = compteTransferService.getTransferDetails(1L);

        // Assert
        assertNotNull(result);
        assertEquals(transfert.getId(), result.getTransferId());
        assertEquals(sourceCollecteur.getId(), result.getSourceCollecteurId());
        assertEquals(targetCollecteur.getId(), result.getTargetCollecteurId());
        assertEquals(1, result.getClientsTransferes().size());
        assertEquals(1, result.getMouvements().size());
    }

    @Test
    void testGetTransferDetails_NotFound() {
        // Arrange
        when(transfertCompteRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            compteTransferService.getTransferDetails(999L);
        });
    }
}
