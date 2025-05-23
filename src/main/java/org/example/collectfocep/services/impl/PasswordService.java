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
import org.example.collectfocep.repositories.CollecteurRepository;
import org.example.collectfocep.exceptions.ResourceNotFoundException;
import org.example.collectfocep.exceptions.UnauthorizedException;
import org.example.collectfocep.entities.Collecteur;
import lombok.RequiredArgsConstructor;


@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordService {

    private final CollecteurRepository collecteurRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;

    /**
     * Valide la complexité du mot de passe
     *
     * @param password Mot de passe à valider
     * @throws IllegalArgumentException si le mot de passe ne respecte pas les critères
     */
    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères");
        }

        if (password.length() > 128) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas dépasser 128 caractères");
        }

        // Vérifier qu'il contient au moins une lettre majuscule
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins une lettre majuscule");
        }

        // Vérifier qu'il contient au moins une lettre minuscule
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins une lettre minuscule");
        }

        // Vérifier qu'il contient au moins un chiffre
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins un chiffre");
        }

        // Vérifier qu'il contient au moins un caractère spécial
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins un caractère spécial");
        }
    }

    /**
     * Change le mot de passe d'un collecteur (par lui-même)
     *
     * @param collecteurId ID du collecteur
     * @param currentPassword Mot de passe actuel
     * @param newPassword Nouveau mot de passe
     * @param authentication Authentification du collecteur
     */
    @Transactional
    public void changePassword(Long collecteurId, String currentPassword, String newPassword, Authentication authentication) {
        log.info("Changement de mot de passe pour le collecteur: {}", collecteurId);

        // Vérifier que l'utilisateur change son propre mot de passe
        if (!authentication.getName().equals(getCollecteurEmail(collecteurId))) {
            throw new UnauthorizedException("Vous ne pouvez changer que votre propre mot de passe");
        }

        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé avec l'ID: " + collecteurId));

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(currentPassword, collecteur.getPassword())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect");
        }

        // Vérifier que le nouveau mot de passe est différent de l'ancien
        if (passwordEncoder.matches(newPassword, collecteur.getPassword())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        // Valider le nouveau mot de passe
        validatePassword(newPassword);

        // Encoder et sauvegarder
        String encodedPassword = passwordEncoder.encode(newPassword);
        collecteur.setPassword(encodedPassword);

        collecteurRepository.save(collecteur);

        log.info("Mot de passe changé avec succès pour le collecteur: {}", collecteurId);
    }

    /**
     * Génère un mot de passe temporaire pour un nouveau collecteur
     *
     * @return Mot de passe temporaire
     */
    public String generateTemporaryPassword() {
        // Générer un mot de passe temporaire sécurisé
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();

        // Assurer qu'on a au moins un caractère de chaque type requis
        password.append(getRandomChar("ABCDEFGHIJKLMNOPQRSTUVWXYZ")); // majuscule
        password.append(getRandomChar("abcdefghijklmnopqrstuvwxyz")); // minuscule
        password.append(getRandomChar("0123456789")); // chiffre
        password.append(getRandomChar("!@#$%^&*")); // spécial

        // Compléter avec des caractères aléatoires
        for (int i = 4; i < 12; i++) {
            password.append(getRandomChar(chars));
        }

        // Mélanger les caractères
        return shuffleString(password.toString());
    }

    private char getRandomChar(String chars) {
        return chars.charAt((int) (Math.random() * chars.length()));
    }

    private String shuffleString(String string) {
        char[] chars = string.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    private String getCollecteurEmail(Long collecteurId) {
        return collecteurRepository.findById(collecteurId)
                .map(Collecteur::getAdresseMail)
                .orElse("");
    }


    @Transactional
    public void resetPassword(Long collecteurId, String newPassword, Authentication authentication) {
        log.info("Réinitialisation du mot de passe pour le collecteur: {}", collecteurId);

        // Vérifier les permissions
        if (!securityService.canResetPassword(authentication, collecteurId)) {
            throw new UnauthorizedException("Vous n'avez pas les droits pour réinitialiser ce mot de passe");
        }

        // Récupérer le collecteur
        Collecteur collecteur = collecteurRepository.findById(collecteurId)
                .orElseThrow(() -> new ResourceNotFoundException("Collecteur non trouvé avec l'ID: " + collecteurId));

        // Valider le nouveau mot de passe
        validatePassword(newPassword);

        // Encoder et sauvegarder le nouveau mot de passe
        String encodedPassword = passwordEncoder.encode(newPassword);
        collecteur.setPassword(encodedPassword);

        // Optionnel : marquer que le mot de passe doit être changé à la prochaine connexion
        // collecteur.setMustChangePassword(true);

        collecteurRepository.save(collecteur);

        log.info("Mot de passe réinitialisé avec succès pour le collecteur: {}", collecteurId);
    }
}