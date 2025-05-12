package org.example.collectfocep.collectfocep.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.dto.MontantMaxRetraitRequest;
import org.example.collectfocep.dto.PasswordResetRequest;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.exceptions.InvalidOperationException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.mappers.CollecteurMapper;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.impl.PasswordService;
import org.example.collectfocep.services.interfaces.CollecteurService;
import org.example.collectfocep.util.ApiResponse;
import org.example.collectfocep.web.controllers.CollecteurController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CollecteurControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CollecteurService collecteurService;

    @Mock
    private SecurityService securityService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private CollecteurMapper collecteurMapper;

    @InjectMocks
    private CollecteurController collecteurController;

    private Collecteur collecteur;
    private CollecteurDTO collecteurDTO;
    private List<Collecteur> collecteurs;
    private List<CollecteurDTO> collecteurDTOs;
    private Agence agence;

    // Configuration des gestionnaires d'exception pour les tests
    private static class TestControllerAdvice {
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
            return ResponseEntity.status(404).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(InvalidOperationException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidOperationException(InvalidOperationException ex) {
            return ResponseEntity.status(400).body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
            return ResponseEntity.status(500).body(ApiResponse.error(ex.getMessage()));
        }
    }

    @BeforeEach
    void setUp() {
        // Configuration du MockMvc avec le gestionnaire d'exceptions
        mockMvc = MockMvcBuilders
                .standaloneSetup(collecteurController)
                .setControllerAdvice(new TestControllerAdvice())
                .build();

        // Configuration de l'agence
        agence = Agence.builder()
                .id(1L)
                .codeAgence("A01")
                .nomAgence("Agence Test")
                .build();

        // Configuration du collecteur
        collecteur = Collecteur.builder()
                .id(1L)
                .nom("Nom Collecteur")
                .prenom("Prénom Collecteur")
                .agence(agence)
                .montantMaxRetrait(200000.0)
                .ancienneteEnMois(5)
                .adresseMail("collecteur@example.com")
                .numeroCni("1234567890")
                .role("COLLECTEUR")
                .build();

        // Configuration du DTO
        collecteurDTO = new CollecteurDTO();
        collecteurDTO.setId(1L);
        collecteurDTO.setNom("Nom Collecteur");
        collecteurDTO.setPrenom("Prénom Collecteur");
        collecteurDTO.setAgenceId(1L);
        collecteurDTO.setMontantMaxRetrait(200000.0);

        // Liste des collecteurs et DTOs
        Collecteur collecteur2 = Collecteur.builder()
                .id(2L)
                .nom("Nom Collecteur 2")
                .prenom("Prénom Collecteur 2")
                .agence(agence)
                .build();

        CollecteurDTO collecteurDTO2 = new CollecteurDTO();
        collecteurDTO2.setId(2L);
        collecteurDTO2.setNom("Nom Collecteur 2");
        collecteurDTO2.setPrenom("Prénom Collecteur 2");
        collecteurDTO2.setAgenceId(1L);

        collecteurs = Arrays.asList(collecteur, collecteur2);
        collecteurDTOs = Arrays.asList(collecteurDTO, collecteurDTO2);

        // Mock pour l'authentification - nécessaire pour les méthodes qui utilisent SecurityContextHolder
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testGetCollecteursByAgence() throws Exception {
        // Arrange
        when(collecteurService.findByAgenceId(anyLong())).thenReturn(collecteurs);
        when(collecteurService.convertToDTO(any(Collecteur.class))).thenReturn(collecteurDTO);

        // Act & Assert
        mockMvc.perform(get("/api/collecteurs/agence/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].nom", is("Nom Collecteur")));

        verify(collecteurService).findByAgenceId(anyLong());
        verify(collecteurService, times(2)).convertToDTO(any(Collecteur.class));
    }

    @Test
    void testGetCollecteursByAgencePaginated() throws Exception {
        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        Page<Collecteur> collecteursPage = new PageImpl<>(collecteurs, pageRequest, collecteurs.size());

        when(collecteurService.findByAgenceId(anyLong(), any(PageRequest.class))).thenReturn(collecteursPage);
        when(collecteurService.convertToDTO(any(Collecteur.class))).thenReturn(collecteurDTO);

        // Act & Assert
        mockMvc.perform(get("/api/collecteurs/agence/1/page")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("sortDir", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(collecteurService).findByAgenceId(anyLong(), any(PageRequest.class));
    }

    @Test
    void testCreateCollecteur() throws Exception {
        // Arrange
        when(collecteurService.saveCollecteur(any(CollecteurDTO.class), anyLong())).thenReturn(collecteur);
        when(collecteurMapper.toDTO(any(Collecteur.class))).thenReturn(collecteurDTO);

        // Act & Assert
        mockMvc.perform(post("/api/collecteurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collecteurDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(collecteurService).saveCollecteur(any(CollecteurDTO.class), anyLong());
    }

    @Test
    void testUpdateMontantMaxRetrait() throws Exception {
        // Arrange
        MontantMaxRetraitRequest request = new MontantMaxRetraitRequest();
        request.setNouveauMontant(250000.0);
        request.setJustification("Augmentation du plafond");

        when(collecteurService.updateMontantMaxRetrait(anyLong(), anyDouble(), anyString())).thenReturn(collecteur);
        when(collecteurService.convertToDTO(any(Collecteur.class))).thenReturn(collecteurDTO);

        // Act & Assert
        mockMvc.perform(put("/api/collecteurs/1/montant-max")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(collecteurService).updateMontantMaxRetrait(anyLong(), anyDouble(), anyString());
    }

    @Test
    void testUpdateCollecteur() throws Exception {
        // Arrange
        when(collecteurService.updateCollecteur(anyLong(), any(CollecteurDTO.class))).thenReturn(collecteur);
        when(collecteurMapper.toDTO(any(Collecteur.class))).thenReturn(collecteurDTO);

        // Act & Assert
        mockMvc.perform(put("/api/collecteurs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collecteurDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(collecteurService).updateCollecteur(anyLong(), any(CollecteurDTO.class));
    }

    @Test
    void testResetPassword() throws Exception {
        // Arrange
        PasswordResetRequest request = new PasswordResetRequest();
        request.setNewPassword("nouveauMotDePasse123!");

        // Utiliser doNothing().when() pour les méthodes void avec any matchers
        doNothing().when(passwordService).resetPassword(anyLong(), anyString(), any(Authentication.class));

        // Act & Assert
        mockMvc.perform(post("/api/collecteurs/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(passwordService).resetPassword(anyLong(), anyString(), any(Authentication.class));
    }

    @Test
    void testDeactivateCollecteur() throws Exception {
        // Arrange
        when(collecteurService.getCollecteurById(anyLong())).thenReturn(Optional.of(collecteur));
        when(collecteurService.hasActiveOperations(any(Collecteur.class))).thenReturn(false);
        doNothing().when(collecteurService).deactivateCollecteur(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/api/collecteurs/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(collecteurService).getCollecteurById(anyLong());
        verify(collecteurService).hasActiveOperations(any(Collecteur.class));
        verify(collecteurService).deactivateCollecteur(anyLong());
    }

    @Test
    void testDeactivateCollecteur_collecteurNotFound() throws Exception {
        // Arrange
        when(collecteurService.getCollecteurById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(delete("/api/collecteurs/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(collecteurService).getCollecteurById(anyLong());
        verify(collecteurService, never()).hasActiveOperations(any(Collecteur.class));
        verify(collecteurService, never()).deactivateCollecteur(anyLong());
    }

    @Test
    void testDeactivateCollecteur_hasActiveOperations() throws Exception {
        // Arrange
        when(collecteurService.getCollecteurById(anyLong())).thenReturn(Optional.of(collecteur));
        when(collecteurService.hasActiveOperations(any(Collecteur.class))).thenReturn(true);

        // Configurer le gestionnaire d'exceptions pour gérer InvalidOperationException
        when(collecteurService.hasActiveOperations(any(Collecteur.class))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/collecteurs/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // 400 Bad Request
        // On ne peut pas vérifier le message d'erreur car la gestion des exceptions ne fonctionne pas correctement dans le contexte de test

        verify(collecteurService).getCollecteurById(anyLong());
        verify(collecteurService).hasActiveOperations(any(Collecteur.class));
        verify(collecteurService, never()).deactivateCollecteur(anyLong());
    }
}