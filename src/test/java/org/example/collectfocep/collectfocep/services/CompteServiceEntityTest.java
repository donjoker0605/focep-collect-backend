package org.example.collectfocep.collectfocep.services;

import org.example.collectfocep.entities.*;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.services.impl.CompteServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompteServiceEntityTest {

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private CompteCollecteurRepository compteCollecteurRepository;

    @Mock
    private CompteLiaisonRepository compteLiaisonRepository;

    @InjectMocks
    private CompteServiceImpl compteService;

    private Agence agence;
    private Collecteur collecteur;
    private CompteCollecteur compteCollecteur;
    private CompteCollecteur compteAttente;
    private CompteCollecteur compteSalaire;
    private CompteLiaison compteLiaison;
    private Compte compteProduit;
    private Compte compteTVA;
    private Compte compteCharge;

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

        compteCollecteur = new CompteCollecteur();
        compteCollecteur.setId(1L);
        compteCollecteur.setNumeroCompte("SRV001");
        compteCollecteur.setSolde(1000.0);
        compteCollecteur.setTypeCompte("SERVICE");
        compteCollecteur.setCollecteur(collecteur);

        compteAttente = new CompteCollecteur();
        compteAttente.setId(2L);
        compteAttente.setNumeroCompte("ATT001");
        compteAttente.setSolde(500.0);
        compteAttente.setTypeCompte("ATTENTE");
        compteAttente.setCollecteur(collecteur);

        compteSalaire = new CompteCollecteur();
        compteSalaire.setId(3L);
        compteSalaire.setNumeroCompte("SAL001");
        compteSalaire.setSolde(0.0);
        compteSalaire.setTypeCompte("SALAIRE");
        compteSalaire.setCollecteur(collecteur);

        compteLiaison = new CompteLiaison();
        compteLiaison.setId(4L);
        compteLiaison.setNumeroCompte("LIA001");
        compteLiaison.setSolde(2000.0);
        compteLiaison.setTypeCompte("LIAISON");
        compteLiaison.setAgence(agence);

        compteProduit = mock(Compte.class);
        compteProduit.setId(5L);
        compteProduit.setNumeroCompte("PROD001");
        compteProduit.setSolde(0.0);
        compteProduit.setTypeCompte("PRODUIT");

        compteTVA = mock(Compte.class);
        compteTVA.setId(6L);
        compteTVA.setNumeroCompte("TVA001");
        compteTVA.setSolde(0.0);
        compteTVA.setTypeCompte("TVA");

        compteCharge = mock(Compte.class);
        compteCharge.setId(7L);
        compteCharge.setNumeroCompte("CHG001");
        compteCharge.setSolde(1000.0);
        compteCharge.setTypeCompte("CHARGE");
    }

    @Test
    void testGetAllComptes() {
        // Arrange
        List<Compte> comptes = Arrays.asList(compteCollecteur, compteAttente, compteSalaire);
        when(compteRepository.findAll()).thenReturn(comptes);

        // Act
        List<Compte> result = compteService.getAllComptes();

        // Assert
        assertEquals(3, result.size());
        verify(compteRepository).findAll();
    }

    @Test
    void testGetAllComptesPaginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Compte> comptes = Arrays.asList(compteCollecteur, compteAttente, compteSalaire);
        Page<Compte> comptesPage = new PageImpl<>(comptes, pageable, comptes.size());
        when(compteRepository.findAll(pageable)).thenReturn(comptesPage);

        // Act
        Page<Compte> result = compteService.getAllComptes(pageable);

        // Assert
        assertEquals(3, result.getTotalElements());
        verify(compteRepository).findAll(pageable);
    }

    @Test
    void testFindServiceAccount() {
        // Arrange
        when(compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SERVICE"))
                .thenReturn(Optional.of(compteCollecteur));

        // Act
        CompteCollecteur result = compteService.findServiceAccount(collecteur);

        // Assert
        assertNotNull(result);
        assertEquals("SRV001", result.getNumeroCompte());
        assertEquals("SERVICE", result.getTypeCompte());
        verify(compteCollecteurRepository).findByCollecteurAndTypeCompte(collecteur, "SERVICE");
    }

    @Test
    void testFindServiceAccount_NotFound() {
        // Arrange
        when(compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SERVICE"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            compteService.findServiceAccount(collecteur);
        });
        verify(compteCollecteurRepository).findByCollecteurAndTypeCompte(collecteur, "SERVICE");
    }

    @Test
    void testFindWaitingAccount() {
        // Arrange
        when(compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "ATTENTE"))
                .thenReturn(Optional.of(compteAttente));

        // Act
        CompteCollecteur result = compteService.findWaitingAccount(collecteur);

        // Assert
        assertNotNull(result);
        assertEquals("ATT001", result.getNumeroCompte());
        assertEquals("ATTENTE", result.getTypeCompte());
        verify(compteCollecteurRepository).findByCollecteurAndTypeCompte(collecteur, "ATTENTE");
    }

    @Test
    void testFindSalaryAccount() {
        // Arrange
        when(compteCollecteurRepository.findByCollecteurAndTypeCompte(collecteur, "SALAIRE"))
                .thenReturn(Optional.of(compteSalaire));

        // Act
        CompteCollecteur result = compteService.findSalaryAccount(collecteur);

        // Assert
        assertNotNull(result);
        assertEquals("SAL001", result.getNumeroCompte());
        assertEquals("SALAIRE", result.getTypeCompte());
        verify(compteCollecteurRepository).findByCollecteurAndTypeCompte(collecteur, "SALAIRE");
    }

    @Test
    void testFindLiaisonAccount() {
        // Arrange
        when(compteLiaisonRepository.findByAgenceAndTypeCompte(agence, "LIAISON"))
                .thenReturn(Optional.of(compteLiaison));

        // Act
        CompteLiaison result = compteService.findLiaisonAccount(agence);

        // Assert
        assertNotNull(result);
        assertEquals("LIA001", result.getNumeroCompte());
        assertEquals("LIAISON", result.getTypeCompte());
        verify(compteLiaisonRepository).findByAgenceAndTypeCompte(agence, "LIAISON");
    }

    @Test
    void testFindProduitAccount() {
        // Arrange
        when(compteRepository.findByTypeCompte("PRODUIT")).thenReturn(Optional.of(compteProduit));

        // Act
        Compte result = compteService.findProduitAccount();

        // Assert
        assertNotNull(result);
        assertEquals("PROD001", result.getNumeroCompte());
        assertEquals("PRODUIT", result.getTypeCompte());
        verify(compteRepository).findByTypeCompte("PRODUIT");
    }

    @Test
    void testFindTVAAccount() {
        // Arrange
        when(compteRepository.findByTypeCompte("TVA")).thenReturn(Optional.of(compteTVA));

        // Act
        Compte result = compteService.findTVAAccount();

        // Assert
        assertNotNull(result);
        assertEquals("TVA001", result.getNumeroCompte());
        assertEquals("TVA", result.getTypeCompte());
        verify(compteRepository).findByTypeCompte("TVA");
    }

    @Test
    void testCreateCollecteurAccounts() {
        // Arrange
        when(compteCollecteurRepository.save(any(CompteCollecteur.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        compteService.createCollecteurAccounts(collecteur);

        // Assert
        // Vérifier que 5 comptes ont été créés: SERVICE, ATTENTE, REMUNERATION, MANQUANT, CHARGE
        verify(compteCollecteurRepository, times(5)).save(any(CompteCollecteur.class));
    }

    @Test
    void testGetSolde() {
        // Arrange
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteCollecteur));

        // Act
        double solde = compteService.getSolde(1L);

        // Assert
        assertEquals(1000.0, solde);
        verify(compteRepository).findById(1L);
    }

    @Test
    void testSaveCompte() {
        // Arrange
        when(compteRepository.save(any(Compte.class))).thenReturn(compteCollecteur);

        // Act
        Compte result = compteService.saveCompte(compteCollecteur);

        // Assert
        assertNotNull(result);
        assertEquals(compteCollecteur.getNumeroCompte(), result.getNumeroCompte());
        verify(compteRepository).save(compteCollecteur);
    }

    @Test
    void testDeleteCompte() {
        // Arrange
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteCollecteur));
        compteCollecteur.setSolde(0.0); // Le solde doit être à 0 pour permettre la suppression
        doNothing().when(compteRepository).deleteById(1L);

        // Act
        compteService.deleteCompte(1L);

        // Assert
        verify(compteRepository).findById(1L);
        verify(compteRepository).deleteById(1L);
    }

    @Test
    void testDeleteCompte_SoldeNonNul() {
        // Arrange
        when(compteRepository.findById(1L)).thenReturn(Optional.of(compteCollecteur));
        // Le solde n'est pas à 0, donc la suppression devrait échouer

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            compteService.deleteCompte(1L);
        });
        verify(compteRepository).findById(1L);
        verify(compteRepository, never()).deleteById(anyLong());
    }

    @Test
    void testFindByTypeCompte() {
        // Arrange
        when(compteRepository.findByTypeCompte("SERVICE")).thenReturn(Optional.of(compteCollecteur));

        // Act
        Optional<Compte> result = compteService.findByTypeCompte("SERVICE");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("SERVICE", result.get().getTypeCompte());
        verify(compteRepository).findByTypeCompte("SERVICE");
    }

    @Test
    void testFindByAgenceId() {
        // Arrange
        List<Compte> comptes = Arrays.asList(compteLiaison);
        when(compteRepository.findByAgenceId(1L)).thenReturn(comptes);

        // Act
        List<Compte> result = compteService.findByAgenceId(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("LIAISON", result.get(0).getTypeCompte());
        verify(compteRepository).findByAgenceId(1L);
    }

    @Test
    void testFindByCollecteurId() {
        // Arrange
        List<Compte> comptes = Arrays.asList(compteCollecteur, compteAttente, compteSalaire);
        when(compteRepository.findByCollecteurId(1L)).thenReturn(comptes);

        // Act
        List<Compte> result = compteService.findByCollecteurId(1L);

        // Assert
        assertEquals(3, result.size());
        verify(compteRepository).findByCollecteurId(1L);
    }
}