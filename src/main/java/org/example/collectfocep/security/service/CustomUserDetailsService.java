package org.example.collectfocep.security.service;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Admin;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.entities.Utilisateur;
import org.example.collectfocep.repositories.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("🔍 Chargement utilisateur: {}", username);
        
        Utilisateur user = utilisateurRepository.findByAdresseMail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email : " + username));

        // Vérifier le statut actif pour Admin et Collecteur
        if (user instanceof Admin || user instanceof Collecteur) {
            Boolean isActive = null;
            String userType = null;
            
            if (user instanceof Admin) {
                // Les admins sont toujours actifs dans la table utilisateur
                // Pas de champ actif spécifique pour Admin
                isActive = true;
                userType = "Admin";
            } else if (user instanceof Collecteur) {
                Collecteur collecteur = (Collecteur) user;
                isActive = collecteur.getActive();
                userType = "Collecteur";
            }
            
            if (isActive != null && !isActive) {
                log.warn("🚫 Tentative de connexion d'un {} désactivé: {}", userType, username);
                throw new DisabledException("Votre compte " + userType.toLowerCase() + " a été désactivé. Contactez votre administrateur.");
            }
        }
        
        // Note: Les clients ne se connectent pas via ce service, ils ont leur propre logique
        // Et s'ils se connectaient, ils pourraient le faire même désactivés (seuls les retraits sont bloqués)
        
        log.debug("✅ Utilisateur chargé: {} ({})", username, user.getRole());
        
        return User.builder()
                .username(user.getAdresseMail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false) // Gestion faite au niveau métier ci-dessus
                .build();
    }
}
