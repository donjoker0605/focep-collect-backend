package org.example.collectfocep.collectfocep.services;

import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private AgenceRepository agenceRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private CollecteurRepository collecteurRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private CompteClientRepository compteClientRepository;

    @Mock
    private CompteCollecteurRepository compteCollecteurRepository;

    @Mock
    private CompteLiaisonRepository compteLiaisonRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SecurityService securityService;

    private Admin admin;
    private Collecteur collecteur;
    private Client client;
    private Agence agence1;
    private Agence agence2;
    private CompteClient compteClient;
    private CompteCollecteur compteCollecteur;

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

        // Configuration de l'admin
        admin = new Admin();
        admin.setId(1L);
        admin.setNom("Admin");
        admin.setPrenom("Test");
        admin.setAdresseMail("admin@example.com");
        admin.setRole("ADMIN");
        admin.setAgence(agence1);

        // Configuration du collecteur
        collecteur = new Collecteur();
        collecteur.setId(1L);
        collecteur.setNom("Collecteur");
        collecteur.setPrenom("Test");
        collecteur.setAdresseMail("collecteur@example.com");
        collecteur.setRole("COLLECTEUR");
        collecteur.setAgence(agence1);

        // Configuration du client
        client = new Client();
        client.setId(1L);
        client.setNom("Client");
        client.setPrenom("Test");
        client.setAgence(agence1);
        client.setCollecteur(collecteur);

        // Configuration des comptes
        compteClient = new CompteClient();
        compteClient.setId(1L);
        compteClient.setNumeroCompte("CL001");
        compteClient.setClient(client);

        compteCollecteur = new CompteCollecteur();
        compteCollecteur.setId(2L);
        compteCollecteur.setNumeroCompte("CO001");
        compteCollecteur.setCollecteur(collecteur);

        // Configuration du SecurityContext pour simuler l'authentification
        SecurityContextHolder.setContext(securityContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCanAccessAgence_SuperAdmin() {
        // Arrange
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("superadmin@example.com");

        // Act
        boolean result = securityService.canAccessAgence(authentication, 1L);

        // Assert
        assertTrue(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCanAccessAgence_AdminSameAgence() {
        // Arrange
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("admin@example.com");

        when(adminRepository.findByAdresseMailWithAgence("admin@example.com"))
                .thenReturn(Optional.of(admin));

        // Act
        boolean result = securityService.canAccessAgence(authentication, 1L);

        // Assert
        assertTrue(result);
        verify(adminRepository).findByAdresseMailWithAgence("admin@example.com");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCanAccessAgence_AdminDifferentAgence() {
        // Arrange
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("admin@example.com");

        when(adminRepository.findByAdresseMailWithAgence("admin@example.com"))
                .thenReturn(Optional.of(admin));

        // Act
        boolean result = securityService.canAccessAgence(authentication, 2L);

        // Assert
        assertFalse(result);
        verify(adminRepository).findByAdresseMailWithAgence("admin@example.com");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCanManageCollecteur_SuperAdmin() {
        // Arrange
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();

        // Act
        boolean result = securityService.canManageCollecteur(authentication, 1L);

        // Assert
        assertTrue(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCanManageCollecteur_AdminSameAgence() {
        // Arrange
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("admin@example.com");

        when(adminRepository.findByAdresseMail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(collecteurRepository.findById(1L))
                .thenReturn(Optional.of(collecteur));

        // Act
        boolean result = securityService.canManageCollecteur(authentication, 1L);

        // Assert
        assertTrue(result);
        verify(adminRepository).findByAdresseMail("admin@example.com");
        verify(collecteurRepository).findById(1L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCanManageClient_SuperAdmin() {
        // Arrange
        SimpleGrantedAuthority superAdminAuthority = new SimpleGrantedAuthority("ROLE_SUPER_ADMIN");
        // Utilisez la méthode doReturn/when plutôt que when/thenReturn pour éviter les problèmes de types génériques
        doReturn(true).when(authentication).isAuthenticated();
        doReturn(Collections.singletonList(superAdminAuthority)).when(authentication).getAuthorities();
        doReturn("superadmin@example.com").when(authentication).getName();

        // Act
        boolean result = securityService.canManageClient(authentication, 1L);

        // Assert
        assertTrue(result, "Un Super Admin devrait toujours pouvoir gérer n'importe quel client");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCanAccessCompte_SuperAdmin() {
        // Arrange
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(authentication.isAuthenticated()).thenReturn(true);

        // Act
        boolean result = securityService.canAccessCompte(authentication, 1L);

        // Assert
        assertTrue(result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCanResetPassword_SuperAdmin() {
        // Arrange
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("superadmin@example.com");

        // Act
        boolean result = securityService.canResetPassword(authentication, 1L);

        // Assert
        assertTrue(result);
    }

    @Test
    void testGetCurrentUsername() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user@example.com");

        // Act
        String result = securityService.getCurrentUsername();

        // Assert
        assertEquals("user@example.com", result);
    }
}