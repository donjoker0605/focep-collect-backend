package org.example.collectfocep.collectfocep.services.impl;

import org.example.collectfocep.Validation.CollecteurValidator;
import org.example.collectfocep.dto.CollecteurDTO;
import org.example.collectfocep.entities.Agence;
import org.example.collectfocep.entities.Client;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.HistoriqueMontantMax;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedAccessException;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.repositories.HistoriqueMontantMaxRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.MetricsService;
import org.example.collectfocep.services.impl.CollecteurServiceImpl;
import org.example.collectfocep.services.interfaces.CompteService;
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
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CollecteurServiceTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private CollecteurRepository collecteurRepository;

    @Mock
    private CollecteurValidator collecteurValidator;

    @Mock
    private SecurityService securityService;

    @Mock
    private HistoriqueMontantMaxRepository historiqueRepository;

    @Mock
    private CompteService compteService;

    @InjectMocks
    private CollecteurServiceImpl collecteurService;

    private Collecteur collecteur;
    private Agence agence;
    private List<Collecteur> collecteurs;

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
        collecteur.setMontantMaxRetrait(200000.0);
        collecteur.setAncienneteEnMois(5);
        collecteur.setAdresseMail("collecteur@example.com");
        collecteur.setNumeroCni("1234567890");
        collecteur.setRole("COLLECTEUR");
        collecteur.setPassword("password");

        // Liste de collecteurs pour les tests
        collecteurs = new ArrayList<>();

        Collecteur collecteur2 = new Collecteur();
        collecteur2.setId(2L);
        collecteur2.setNom("Nom Collecteur 2");
        collecteur2.setPrenom("Prénom Collecteur 2");
        collecteur2.setAgence(agence);
        collecteur2.setMontantMaxRetrait(150000.0);
        collecteur2.setAncienneteEnMois(3);

        collecteurs.add(collecteur);
        collecteurs.add(collecteur2);

        // Configurez les mocks pour éviter les NullPointerException
        doNothing().when(metricsService).incrementCounter(anyString(), any());
    }

    @Test
    void testGetAllCollecteurs() {
        // Arrange
        when(collecteurRepository.findAll()).thenReturn(collecteurs);

        // Act
        List<Collecteur> result = collecteurService.getAllCollecteurs();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Nom Collecteur", result.get(0).getNom());
        assertEquals("Nom Collecteur 2", result.get(1).getNom());
    }

    @Test
    void testGetAllCollecteursPaginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Collecteur> collecteursPage = new PageImpl<>(collecteurs, pageable, collecteurs.size());
        when(collecteurRepository.findAll(pageable)).thenReturn(collecteursPage);

        // Act
        Page<Collecteur> result = collecteurService.getAllCollecteurs(pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        assertEquals("Nom Collecteur", result.getContent().get(0).getNom());
    }

    @Test
    void testGetCollecteurById_Exists() {
        // Arrange
        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(collecteur));

        // Act
        Optional<Collecteur> result = collecteurService.getCollecteurById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Nom Collecteur", result.get().getNom());
    }

    @Test
    void testGetCollecteurById_NotExists() {
        // Arrange
        when(collecteurRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Collecteur> result = collecteurService.getCollecteurById(999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testSaveCollecteur() {
        // Arrange
        when(collecteurRepository.save(any(Collecteur.class))).thenReturn(collecteur);
        doNothing().when(compteService).createCollecteurAccounts(any(Collecteur.class));

        // Act
        Collecteur result = collecteurService.saveCollecteur(collecteur);

        // Assert
        assertNotNull(result);
        assertEquals("Nom Collecteur", result.getNom());
        verify(collecteurRepository).save(collecteur);
        verify(compteService).createCollecteurAccounts(collecteur);
    }

    @Test
    void testDeactivateCollecteur() {
        // Arrange
        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(collecteur));
        when(collecteurRepository.save(any(Collecteur.class))).thenReturn(collecteur);

        // Act
        collecteurService.deactivateCollecteur(1L);

        // Assert
        assertFalse(collecteur.isActive());
        verify(collecteurRepository).save(collecteur);
    }

    @Test
    void testDeactivateCollecteur_NotFound() {
        // Arrange
        when(collecteurRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            collecteurService.deactivateCollecteur(999L);
        });

        verify(collecteurRepository, never()).save(any(Collecteur.class));
    }

    @Test
    void testFindByAgenceId() {
        // Arrange
        when(collecteurRepository.findByAgenceId(1L)).thenReturn(collecteurs);

        // Act
        List<Collecteur> result = collecteurService.findByAgenceId(1L);

        // Assert
        assertEquals(2, result.size());
        verify(collecteurRepository).findByAgenceId(1L);
    }

    @Test
    void testUpdateMontantMaxRetrait_Success() {
        // Arrange
        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(collecteur));
        when(securityService.hasPermissionForCollecteur(collecteur)).thenReturn(true);
        when(securityService.getCurrentUsername()).thenReturn("admin");
        when(collecteurRepository.save(any(Collecteur.class))).thenReturn(collecteur);

        double nouveauMontant = 250000.0;
        String justification = "Augmentation du plafond pour les besoins du client";

        // Act
        Collecteur result = collecteurService.updateMontantMaxRetrait(1L, nouveauMontant, justification);

        // Assert
        assertEquals(nouveauMontant, result.getMontantMaxRetrait());
        assertEquals("admin", result.getModifiePar());
        assertNotNull(result.getDateModificationMontantMax());

        // Vérifier que l'historique a été créé
        verify(historiqueRepository).save(any(HistoriqueMontantMax.class));
    }

    @Test
    void testUpdateMontantMaxRetrait_CollecteurNotFound() {
        // Arrange
        when(collecteurRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            collecteurService.updateMontantMaxRetrait(999L, 250000.0, "Test");
        });

        verify(historiqueRepository, never()).save(any(HistoriqueMontantMax.class));
        verify(collecteurRepository, never()).save(any(Collecteur.class));
    }

    @Test
    void testUpdateMontantMaxRetrait_UnauthorizedAccess() {
        // Arrange
        when(collecteurRepository.findById(1L)).thenReturn(Optional.of(collecteur));
        when(securityService.hasPermissionForCollecteur(collecteur)).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedAccessException.class, () -> {
            collecteurService.updateMontantMaxRetrait(1L, 250000.0, "Test");
        });

        verify(historiqueRepository, never()).save(any(HistoriqueMontantMax.class));
        verify(collecteurRepository, never()).save(any(Collecteur.class));
    }

    @Test
    void testConvertToDTO() {
        // Act
        CollecteurDTO dto = collecteurService.convertToDTO(collecteur);

        // Assert
        assertNotNull(dto);
        assertEquals(collecteur.getId(), dto.getId());
        assertEquals(collecteur.getNom(), dto.getNom());
        assertEquals(collecteur.getPrenom(), dto.getPrenom());
        assertEquals(collecteur.getAgence().getId(), dto.getAgenceId());
        assertEquals(collecteur.getMontantMaxRetrait(), dto.getMontantMaxRetrait());
    }

    @Test
    void testConvertToEntity() {
        // Arrange
        CollecteurDTO dto = new CollecteurDTO();
        dto.setId(1L);
        dto.setNom("Nom Test");
        dto.setPrenom("Prénom Test");
        dto.setAgenceId(1L);
        dto.setMontantMaxRetrait(200000.0);

        // Act
        Collecteur entity = collecteurService.convertToEntity(dto);

        // Assert
        assertNotNull(entity);
        assertEquals(dto.getId(), entity.getId());
        assertEquals(dto.getNom(), entity.getNom());
        assertEquals(dto.getPrenom(), entity.getPrenom());
        assertEquals(dto.getMontantMaxRetrait(), entity.getMontantMaxRetrait());
        // L'agence n'est pas définie dans convertToEntity
        assertNull(entity.getAgence());
    }

    @Test
    void testHasActiveOperations_WithClients() {
        // Arrange
        List<Client> clients = new ArrayList<>();
        clients.add(new Client());
        collecteur.setClients(clients);

        // Act
        boolean result = collecteurService.hasActiveOperations(collecteur);

        // Assert
        assertTrue(result);
    }

    @Test
    void testHasActiveOperations_NoClients() {
        // Arrange
        collecteur.setClients(new ArrayList<>());

        // Act
        boolean result = collecteurService.hasActiveOperations(collecteur);

        // Assert
        assertFalse(result);
    }
}