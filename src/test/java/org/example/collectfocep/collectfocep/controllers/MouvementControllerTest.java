package org.example.collectfocep.collectfocep.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.collectfocep.dto.EpargneRequest;
import org.example.collectfocep.dto.RetraitRequest;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Journal;
import org.example.collectfocep.entities.Mouvement;
import org.example.collectfocep.repositories.ClientRepository;
import org.example.collectfocep.repositories.JournalRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.impl.MouvementServiceImpl;
import org.example.collectfocep.web.controllers.MouvementController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@ExtendWith(MockitoExtension.class)
public class MouvementControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MouvementServiceImpl mouvementServiceImpl;

    @Mock
    private SecurityService securityService;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private JournalRepository journalRepository;

    @InjectMocks
    private MouvementController mouvementController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Client client;
    private Collecteur collecteur;
    private Journal journal;
    private Mouvement mouvement;
    private EpargneRequest epargneRequest;
    private RetraitRequest retraitRequest;

    @BeforeEach
    void setUp() {
        // Configuration du MockMvc avec support de sécurité
        mockMvc = MockMvcBuilders.standaloneSetup(mouvementController)
                .build();

        // Configuration du collecteur
        collecteur = new Collecteur();
        collecteur.setId(1L);
        collecteur.setNom("Nom Collecteur");
        collecteur.setPrenom("Prénom Collecteur");
        collecteur.setMontantMaxRetrait(200000.0);

        // Configuration du client
        client = new Client();
        client.setId(1L);
        client.setNom("Nom Client");
        client.setPrenom("Prénom Client");
        client.setCollecteur(collecteur);

        // Configuration du journal
        journal = new Journal();
        journal.setId(1L);
        journal.setCollecteur(collecteur);

        // Configuration du mouvement
        mouvement = new Mouvement();
        mouvement.setId(1L);
        mouvement.setMontant(5000.0);
        mouvement.setSens("epargne");
        mouvement.setLibelle("Test épargne");
        mouvement.setDateOperation(LocalDateTime.now());
        mouvement.setJournal(journal);

        // Configuration des requêtes DTO
        epargneRequest = new EpargneRequest();
        epargneRequest.setClientId(1L);
        epargneRequest.setCollecteurId(1L);
        epargneRequest.setMontant(5000.0);
        epargneRequest.setJournalId(1L);

        retraitRequest = new RetraitRequest();
        retraitRequest.setClientId(1L);
        retraitRequest.setCollecteurId(1L);
        retraitRequest.setMontant(2000.0);
        retraitRequest.setJournalId(1L);
    }

    @Test
    void testEffectuerEpargne() throws Exception {
        // Arrange
        when(securityService.canManageClient(any(), eq(1L))).thenReturn(true);
        when(securityService.isClientInCollecteurAgence(1L, 1L)).thenReturn(true);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(journalRepository.findById(1L)).thenReturn(Optional.of(journal));
        when(mouvementServiceImpl.enregistrerEpargne(eq(client), eq(5000.0), eq(journal))).thenReturn(mouvement);

        // Act & Assert
        mockMvc.perform(post("/api/mouvements/epargne")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(epargneRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.montant", is(5000.0)))
                .andExpect(jsonPath("$.sens", is("epargne")));

        verify(securityService).canManageClient(any(), eq(1L));
        verify(securityService).isClientInCollecteurAgence(1L, 1L);
        verify(clientRepository).findById(1L);
        verify(journalRepository).findById(1L);
        verify(mouvementServiceImpl).enregistrerEpargne(eq(client), eq(5000.0), eq(journal));
    }


    @Test
    void testEffectuerRetrait() throws Exception {
        // Arrange
        when(securityService.canManageClient(any(), eq(1L))).thenReturn(true);
        when(securityService.isClientInCollecteurAgence(1L, 1L)).thenReturn(true);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(journalRepository.findById(1L)).thenReturn(Optional.of(journal));

        Mouvement mouvementRetrait = new Mouvement();
        mouvementRetrait.setId(2L);
        mouvementRetrait.setMontant(2000.0);
        mouvementRetrait.setSens("retrait");
        mouvementRetrait.setLibelle("Test retrait");
        mouvementRetrait.setDateOperation(LocalDateTime.now());
        mouvementRetrait.setJournal(journal);

        when(mouvementServiceImpl.enregistrerRetrait(eq(client), eq(2000.0), eq(journal))).thenReturn(mouvementRetrait);

        // Act & Assert
        mockMvc.perform(post("/api/mouvements/retrait")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(retraitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.montant", is(2000.0)))
                .andExpect(jsonPath("$.sens", is("retrait")));

        verify(securityService).canManageClient(any(), eq(1L));
        verify(securityService).isClientInCollecteurAgence(1L, 1L);
        verify(clientRepository).findById(1L);
        verify(journalRepository).findById(1L);
        verify(mouvementServiceImpl).enregistrerRetrait(eq(client), eq(2000.0), eq(journal));
    }

    @Test
    void testGetMouvementsByJournal() throws Exception {
        // Arrange
        List<Mouvement> mouvements = Arrays.asList(mouvement);
        when(securityService.canAccessJournal(any(), eq(1L))).thenReturn(true);
        when(mouvementServiceImpl.findByJournalId(1L)).thenReturn(mouvements);

        // Act & Assert
        mockMvc.perform(get("/api/mouvements/journal/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].montant", is(5000.0)));

        verify(securityService).canAccessJournal(any(), eq(1L));
        verify(mouvementServiceImpl).findByJournalId(1L);
    }

    @Test
    void testEffectuerEpargne_ClientNotFound() throws Exception {
        // Arrange
        when(securityService.canManageClient(any(), eq(1L))).thenReturn(true);
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/mouvements/epargne")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(epargneRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Client non trouvé")));

        verify(securityService).canManageClient(any(), eq(1L));
        verify(clientRepository).findById(1L);
        verify(mouvementServiceImpl, never()).enregistrerEpargne(any(), anyDouble(), any());
    }

    @Test
    void testEffectuerEpargne_UnauthorizedAgency() throws Exception {
        // Arrange
        when(securityService.canManageClient(any(), eq(1L))).thenReturn(true);
        when(securityService.isClientInCollecteurAgence(1L, 1L)).thenReturn(false);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        // Act & Assert
        mockMvc.perform(post("/api/mouvements/epargne")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(epargneRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Client n'appartient pas à votre agence")));

        verify(securityService).canManageClient(any(), eq(1L));
        verify(securityService).isClientInCollecteurAgence(1L, 1L);
        verify(clientRepository).findById(1L);
        verify(mouvementServiceImpl, never()).enregistrerEpargne(any(), anyDouble(), any());
    }
}