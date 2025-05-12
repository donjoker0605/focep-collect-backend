package org.example.collectfocep.collectfocep.services.impl;

import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.services.impl.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client client;
    private Collecteur collecteur;
    private Agence agence;
    private List<Client> clients;

    @BeforeEach
    void setUp() {
        // Initialisation des objets pour les tests
        agence = new Agence();
        agence.setId(1L);
        agence.setCodeAgence("A01");
        agence.setNomAgence("Agence Test");

        collecteur = new Collecteur();
        collecteur.setId(1L);
        collecteur.setNom("Nom Collecteur");
        collecteur.setPrenom("Prénom Collecteur");
        collecteur.setAgence(agence);

        client = new Client();
        client.setId(1L);
        client.setNom("Nom Client");
        client.setPrenom("Prénom Client");
        client.setAgence(agence);
        client.setCollecteur(collecteur);
        client.setNumeroCni("1234567890");
        client.setValide(true);

        Client client2 = new Client();
        client2.setId(2L);
        client2.setNom("Nom Client 2");
        client2.setPrenom("Prénom Client 2");
        client2.setAgence(agence);
        client2.setCollecteur(collecteur);
        client2.setNumeroCni("0987654321");
        client2.setValide(true);

        clients = Arrays.asList(client, client2);
    }

    @Test
    void testGetAllClients() {
        // Arrange
        when(clientRepository.findAll()).thenReturn(clients);

        // Act
        List<Client> result = clientService.getAllClients();

        // Assert
        assertEquals(2, result.size());
        verify(clientRepository).findAll();
    }

    @Test
    void testGetAllClientsPaginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Client> clientsPage = new PageImpl<>(clients, pageable, clients.size());
        when(clientRepository.findAll(pageable)).thenReturn(clientsPage);

        // Act
        Page<Client> result = clientService.getAllClients(pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        verify(clientRepository).findAll(pageable);
    }

    @Test
    void testGetClientById_Exists() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        // Act
        Optional<Client> result = clientService.getClientById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Nom Client", result.get().getNom());
    }

    @Test
    void testGetClientById_NotExists() {
        // Arrange
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Client> result = clientService.getClientById(999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testSaveClient() {
        // Arrange
        when(clientRepository.save(any(Client.class))).thenReturn(client);

        // Act
        Client result = clientService.saveClient(client);

        // Assert
        assertNotNull(result);
        assertEquals("Nom Client", result.getNom());
        verify(clientRepository).save(client);
    }

    @Test
    void testDeleteClient() {
        // Arrange
        doNothing().when(clientRepository).deleteById(1L);

        // Act
        clientService.deleteClient(1L);

        // Assert
        verify(clientRepository).deleteById(1L);
    }

    @Test
    void testFindByAgenceId() {
        // Arrange
        when(clientRepository.findByAgenceId(1L)).thenReturn(clients);

        // Act
        List<Client> result = clientService.findByAgenceId(1L);

        // Assert
        assertEquals(2, result.size());
        verify(clientRepository).findByAgenceId(1L);
    }

    @Test
    void testFindByAgenceIdPaginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Client> clientsPage = new PageImpl<>(clients, pageable, clients.size());
        when(clientRepository.findByAgenceId(eq(1L), any(Pageable.class))).thenReturn(clientsPage);

        // Act
        Page<Client> result = clientService.findByAgenceId(1L, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        verify(clientRepository).findByAgenceId(eq(1L), any(Pageable.class));
    }

    @Test
    void testFindByCollecteurId() {
        // Arrange
        when(clientRepository.findByCollecteurId(1L)).thenReturn(clients);

        // Act
        List<Client> result = clientService.findByCollecteurId(1L);

        // Assert
        assertEquals(2, result.size());
        verify(clientRepository).findByCollecteurId(1L);
    }

    @Test
    void testFindByCollecteurIdPaginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Client> clientsPage = new PageImpl<>(clients, pageable, clients.size());
        when(clientRepository.findByCollecteurId(eq(1L), any(Pageable.class))).thenReturn(clientsPage);

        // Act
        Page<Client> result = clientService.findByCollecteurId(1L, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        verify(clientRepository).findByCollecteurId(eq(1L), any(Pageable.class));
    }

    @Test
    void testUpdateClient() {
        // Arrange
        Client updatedClient = new Client();
        updatedClient.setId(1L);
        updatedClient.setNom("Nom Client Modifié");
        updatedClient.setPrenom("Prénom Client");

        when(clientRepository.save(any(Client.class))).thenReturn(updatedClient);

        // Act
        Client result = clientService.updateClient(updatedClient);

        // Assert
        assertNotNull(result);
        assertEquals("Nom Client Modifié", result.getNom());
        verify(clientRepository).save(updatedClient);
    }

    @Test
    void testUpdateClient_NoId() {
        // Arrange
        Client clientWithoutId = new Client();
        clientWithoutId.setNom("Nom Client");
        clientWithoutId.setPrenom("Prénom Client");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            clientService.updateClient(clientWithoutId);
        });

        verify(clientRepository, never()).save(any(Client.class));
    }
}
