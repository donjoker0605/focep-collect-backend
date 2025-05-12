package org.example.collectfocep.collectfocep.services.impl;

import org.example.collectfocep.entities.Utilisateur;
import org.example.collectfocep.exceptions.InvalidPasswordException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.repositories.UtilisateurRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.impl.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PasswordServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityService securityService;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PasswordService passwordService;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = mock(Utilisateur.class);
        when(utilisateur.getId()).thenReturn(1L);
        when(utilisateur.getPassword()).thenReturn("encoded_old_password");
    }

    @Test
    void testResetPassword_Success() {
        // Arrange
        Long userId = 1L;
        String newPassword = "new_password";
        String encodedNewPassword = "encoded_new_password";

        when(securityService.canResetPassword(authentication, userId)).thenReturn(true);
        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

        // Act
        passwordService.resetPassword(userId, newPassword, authentication);

        // Assert
        verify(securityService).canResetPassword(authentication, userId);
        verify(utilisateurRepository).findById(userId);
        verify(passwordEncoder).encode(newPassword);
        verify(utilisateur).setPassword(encodedNewPassword);
        verify(utilisateurRepository).save(utilisateur);
    }

    @Test
    void testResetPassword_Unauthorized() {
        // Arrange
        Long userId = 1L;
        String newPassword = "new_password";

        when(securityService.canResetPassword(authentication, userId)).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            passwordService.resetPassword(userId, newPassword, authentication);
        });

        verify(securityService).canResetPassword(authentication, userId);
        verify(utilisateurRepository, never()).findById(anyLong());
        verify(passwordEncoder, never()).encode(anyString());
        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }

    @Test
    void testResetPassword_UserNotFound() {
        // Arrange
        Long userId = 999L;
        String newPassword = "new_password";

        when(securityService.canResetPassword(authentication, userId)).thenReturn(true);
        when(utilisateurRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            passwordService.resetPassword(userId, newPassword, authentication);
        });

        verify(securityService).canResetPassword(authentication, userId);
        verify(utilisateurRepository).findById(userId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }

    @Test
    void testChangePassword_Success() {
        // Arrange
        Long userId = 1L;
        String oldPassword = "old_password";
        String newPassword = "new_password";
        String encodedNewPassword = "encoded_new_password";

        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches(oldPassword, utilisateur.getPassword())).thenReturn(true);
        when(securityService.canResetPassword(authentication, userId)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(utilisateur);

        // Act
        passwordService.changePassword(userId, oldPassword, newPassword, authentication);

        // Assert
        verify(utilisateurRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, utilisateur.getPassword());
        verify(securityService).canResetPassword(authentication, userId);
        verify(passwordEncoder).encode(newPassword);
        verify(utilisateur).setPassword(encodedNewPassword);
        verify(utilisateurRepository).save(utilisateur);
    }

    @Test
    void testChangePassword_WrongOldPassword() {
        // Arrange
        Long userId = 1L;
        String oldPassword = "wrong_password";
        String newPassword = "new_password";

        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches(oldPassword, utilisateur.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () -> {
            passwordService.changePassword(userId, oldPassword, newPassword, authentication);
        });

        verify(utilisateurRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, utilisateur.getPassword());
        verify(securityService, never()).canResetPassword(any(), anyLong());
        verify(passwordEncoder, never()).encode(anyString());
        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }

    @Test
    void testChangePassword_Unauthorized() {
        // Arrange
        Long userId = 1L;
        String oldPassword = "old_password";
        String newPassword = "new_password";

        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches(oldPassword, utilisateur.getPassword())).thenReturn(true);
        when(securityService.canResetPassword(authentication, userId)).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            passwordService.changePassword(userId, oldPassword, newPassword, authentication);
        });

        verify(utilisateurRepository).findById(userId);
        verify(passwordEncoder).matches(oldPassword, utilisateur.getPassword());
        verify(securityService).canResetPassword(authentication, userId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(utilisateurRepository, never()).save(any(Utilisateur.class));
    }
}
