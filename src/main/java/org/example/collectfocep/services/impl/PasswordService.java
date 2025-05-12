package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Utilisateur;
import org.example.collectfocep.exceptions.InvalidPasswordException;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.repositories.UtilisateurRepository;
import org.example.collectfocep.security.service.SecurityService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordService {
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public void resetPassword(Long userId, String newPassword, Authentication auth) {
        if (!securityService.canResetPassword(auth, userId)) {
            throw new UnauthorizedException("Non autorisé à réinitialiser le mot de passe");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        String encodedPassword = passwordEncoder.encode(newPassword);
        utilisateur.setPassword(encodedPassword);

        utilisateurRepository.save(utilisateur);
        log.info("Mot de passe réinitialisé pour l'utilisateur: {}", userId);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword, Authentication auth) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(oldPassword, utilisateur.getPassword())) {
            throw new InvalidPasswordException("Ancien mot de passe incorrect");
        }

        if (!securityService.canResetPassword(auth, userId)) {
            throw new UnauthorizedException("Non autorisé à changer le mot de passe");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        utilisateur.setPassword(encodedPassword);

        utilisateurRepository.save(utilisateur);
        log.info("Mot de passe changé pour l'utilisateur: {}", userId);
    }
}