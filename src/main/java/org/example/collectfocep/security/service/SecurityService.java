package org.example.collectfocep.security.service;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.*;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.config.RoleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Optional;

@Valid
@Service
@Slf4j
@CacheConfig(cacheNames = "security-permissions")
public class SecurityService {
    private final AgenceRepository agenceRepository;
    private final AdminRepository adminRepository;
    private final CollecteurRepository collecteurRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CompteRepository compteRepository;
    private final CompteClientRepository compteClientRepository;
    private final CompteCollecteurRepository compteCollecteurRepository;
    private final CompteLiaisonRepository compteLiaisonRepository;
    private final ClientRepository clientRepository;

    @Autowired
    public SecurityService(AgenceRepository agenceRepository,
                           AdminRepository adminRepository,
                           CollecteurRepository collecteurRepository,
                           UtilisateurRepository utilisateurRepository,
                           CompteRepository compteRepository,
                           CompteClientRepository compteClientRepository,
                           CompteCollecteurRepository compteCollecteurRepository,
                           CompteLiaisonRepository compteLiaisonRepository,
                           ClientRepository clientRepository) {
        this.agenceRepository = agenceRepository;
        this.adminRepository = adminRepository;
        this.collecteurRepository = collecteurRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.compteRepository = compteRepository;
        this.compteClientRepository = compteClientRepository;
        this.compteCollecteurRepository = compteCollecteurRepository;
        this.compteLiaisonRepository = compteLiaisonRepository;
        this.clientRepository = clientRepository;
    }

    /**
     * ✅ NOUVELLE MÉTHODE: Vérifie si l'utilisateur authentifié est le propriétaire du collecteur
     * Optimisée avec mise en cache pour éviter les requêtes répétées
     */
    @Cacheable(key = "{'owner-collecteur', #authentication.name, #collecteurId}")
    public boolean isOwnerCollecteur(Authentication authentication, Long collecteurId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Authentication null ou non authentifiée");
            return false;
        }

        String email = authentication.getName();
        log.debug("Vérification propriétaire collecteur: email={}, collecteurId={}", email, collecteurId);

        try {
            // Utiliser la méthode optimisée existante si disponible
            if (hasRole(authentication.getAuthorities(), RoleConfig.COLLECTEUR)) {
                Optional<Collecteur> collecteurAuth = collecteurRepository.findByAdresseMail(email);

                if (collecteurAuth.isEmpty()) {
                    log.debug("Collecteur non trouvé pour email: {}", email);
                    return false;
                }

                boolean isOwner = collecteurAuth.get().getId().equals(collecteurId);
                log.debug("Vérification propriétaire: collecteurAuth.id={}, collecteurId={}, isOwner={}",
                        collecteurAuth.get().getId(), collecteurId, isOwner);

                return isOwner;
            }

            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du propriétaire collecteur", e);
            return false;
        }
    }

    /**
     * Vérifie si l'utilisateur peut accéder à une agence donnée
     *
     * @param auth     L'authentification de l'utilisateur
     * @param agenceId L'ID de l'agence à vérifier
     * @return true si l'accès est autorisé, false sinon
     */
    @Cacheable(key = "{'agence-access', #auth.name, #agenceId}")
    public boolean canAccessAgence(Authentication auth, Long agenceId) {
        try {
            if (auth == null) {
                log.warn("Authentication est null lors de la vérification d'accès à l'agence");
                return false;
            }

            if (agenceId == null) {
                log.warn("agenceId est null lors de la vérification d'accès");
                return false;
            }

            String userEmail = auth.getName();
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

            log.debug("Vérification d'accès à l'agence {} pour l'utilisateur {}", agenceId, userEmail);

            // Super Admin a accès à tout
            if (hasRole(authorities, RoleConfig.SUPER_ADMIN)) {
                log.debug("Accès autorisé pour Super Admin: {}", userEmail);
                return true;
            }

            // Admin ne peut accéder qu'à son agence
            if (hasRole(authorities, RoleConfig.ADMIN)) {
                boolean hasAccess = verifyAdminAgenceAccess(userEmail, agenceId);
                log.debug("Accès admin {} à l'agence {}: {}", userEmail, agenceId, hasAccess);
                return hasAccess;
            }

            // Collecteur ne peut accéder qu'à son agence
            if (hasRole(authorities, RoleConfig.COLLECTEUR)) {
                boolean hasAccess = verifyCollecteurAgenceAccess(userEmail, agenceId);
                log.debug("Accès collecteur {} à l'agence {}: {}", userEmail, agenceId, hasAccess);
                return hasAccess;
            }

            log.warn("Tentative d'accès non autorisée à l'agence {} par {}", agenceId, userEmail);
            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification d'accès à l'agence {}: {}", agenceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Méthode à ajouter à la classe SecurityService
     */
    @Cacheable(key = "{'collecteur-permission', #collecteur.id}")
    public boolean hasPermissionForCollecteur(Collecteur collecteur) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        // Super Admin a tous les droits
        if (hasRole(auth.getAuthorities(), RoleConfig.SUPER_ADMIN)) {
            log.debug("Accès autorisé pour Super Admin: {}", auth.getName());
            return true;
        }

        // Admin de l'agence du collecteur
        if (hasRole(auth.getAuthorities(), RoleConfig.ADMIN)) {
            return adminRepository.findByAdresseMail(auth.getName())
                    .map(admin -> admin.getAgence().getId().equals(collecteur.getAgence().getId()))
                    .orElse(false);
        }

        // Le collecteur lui-même
        if (hasRole(auth.getAuthorities(), RoleConfig.COLLECTEUR)) {
            return auth.getName().equals(collecteur.getAdresseMail());
        }

        return false;
    }

    /**
     * Vérifie si un admin a accès à une agence spécifique
     * Mise en cache pour éviter des requêtes répétées
     */
    @Cacheable(key = "{'admin-agence', #email, #agenceId}")
    private boolean verifyAdminAgenceAccess(String email, Long agenceId) {
        return adminRepository.findByAdresseMailWithAgence(email)
                .map(admin -> {
                    boolean hasAccess = admin.getAgence().getId().equals(agenceId);
                    if (!hasAccess) {
                        log.warn("Admin {} a tenté d'accéder à l'agence {}", email, agenceId);
                    }
                    return hasAccess;
                })
                .orElse(false);
    }

    /**
     * Vérifie si un collecteur a accès à une agence spécifique
     * Mise en cache pour éviter des requêtes répétées
     */
    @Cacheable(key = "{'collecteur-agence', #email, #agenceId}")
    private boolean verifyCollecteurAgenceAccess(String email, Long agenceId) {
        return collecteurRepository.findByAdresseMailWithAgence(email)
                .map(collecteur -> collecteur.getAgence().getId().equals(agenceId))
                .orElse(false);
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
        if (authorities == null) return false;

        String roleToCheck = "ROLE_" + role;
        return authorities.stream()
                .anyMatch(a -> roleToCheck.equals(a.getAuthority()));
    }

    /**
     * Fournit une décision d'autorisation pour Spring Security
     */
    public AuthorizationDecision authorizeAgenceAccess(Authentication authentication, Long agenceId) {
        boolean hasAccess = canAccessAgence(authentication, agenceId);
        return new AuthorizationDecision(hasAccess);
    }

    /**
     * ✅ MÉTHODE MODIFIÉE: Vérifie si l'utilisateur peut accéder à un collecteur spécifique
     * Mise en cache pour optimiser les performances
     * Permet maintenant au collecteur d'accéder à ses propres données
     */
    @Cacheable(key = "{'collecteur-access', #authentication.name, #collecteurId}")
    public boolean canManageCollecteur(Authentication authentication, Long collecteurId) {
        if (authentication == null) return false;

        // Vérifier les rôles
        if (hasRole(authentication.getAuthorities(), RoleConfig.SUPER_ADMIN)) {
            log.debug("Accès collecteur autorisé pour Super Admin: {}", authentication.getName());
            return true;
        }

        String userEmail = authentication.getName();

        if (hasRole(authentication.getAuthorities(), RoleConfig.ADMIN)) {
            return adminRepository.findByAdresseMail(userEmail)
                    .map(admin -> {
                        // Charger le collecteur
                        Optional<Collecteur> collecteurOpt = collecteurRepository.findById(collecteurId);
                        if (collecteurOpt.isEmpty()) return false;

                        // Vérifier que le collecteur appartient à l'agence de l'admin
                        return collecteurOpt.get().getAgence().getId().equals(admin.getAgence().getId());
                    })
                    .orElse(false);
        }

        // ✅ CORRECTION CRITIQUE: Permettre au collecteur de gérer ses propres données
        if (hasRole(authentication.getAuthorities(), RoleConfig.COLLECTEUR)) {
            boolean isOwner = isOwnerCollecteur(authentication, collecteurId);
            log.debug("Collecteur {} peut gérer collecteur {}: {}", userEmail, collecteurId, isOwner);
            return isOwner;
        }

        return false;
    }

    /**
     * ✅ MÉTHODE EXISTANTE CONSERVÉE: Vérifie si l'utilisateur peut accéder à un client spécifique
     */
    @Cacheable(key = "{'client-access', #authentication.name, #clientId}")
    public boolean canManageClient(Authentication authentication, Long clientId) {
        // Vérification de l'authentification en premier
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Vérification du rôle Super Admin en premier
        if (hasRole(authentication.getAuthorities(), RoleConfig.SUPER_ADMIN)) {
            log.debug("Accès client autorisé pour Super Admin: {}", authentication.getName());
            return true;
        }

        try {
            String userEmail = authentication.getName();

            // Vérification pour les Admins avec une requête optimisée
            if (hasRole(authentication.getAuthorities(), RoleConfig.ADMIN)) {
                return adminRepository.existsByAdresseMailAndAgenceId(userEmail,
                        clientRepository.findAgenceIdByClientId(clientId));
            }

            // Vérification pour les Collecteurs avec une requête optimisée
            if (hasRole(authentication.getAuthorities(), RoleConfig.COLLECTEUR)) {
                return collecteurRepository.existsByAdresseMailAndClientId(userEmail, clientId);
            }

            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification des droits d'accès au client {}: {}", clientId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Vérifie si l'utilisateur peut accéder à un compte
     */
    @Cacheable(key = "{'compte-access', #authentication.name, #compteId}")
    public boolean canAccessCompte(Authentication authentication, Long compteId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Super Admin a accès à tous les comptes
        if (hasRole(authentication.getAuthorities(), RoleConfig.SUPER_ADMIN)) {
            return true;
        }

        String userEmail = authentication.getName();
        Optional<Compte> compteOpt = compteRepository.findById(compteId);

        if (compteOpt.isEmpty()) {
            return false;
        }

        Compte compte = compteOpt.get();

        // Vérifier le type de compte et l'autorisation appropriée
        if (compte instanceof CompteClient) {
            CompteClient compteClient = (CompteClient) compte;
            Client client = compteClient.getClient();

            if (client != null) {
                return canManageClient(authentication, client.getId());
            }
        } else if (compte instanceof CompteCollecteur) {
            CompteCollecteur compteCollecteur = (CompteCollecteur) compte;
            Collecteur collecteur = compteCollecteur.getCollecteur();

            if (collecteur != null) {
                return canManageCollecteur(authentication, collecteur.getId());
            }
        } else if (compte instanceof CompteLiaison) {
            CompteLiaison compteLiaison = (CompteLiaison) compte;
            Agence agence = compteLiaison.getAgence();

            if (agence != null) {
                return canAccessAgence(authentication, agence.getId());
            }
        }

        return false;
    }

    /**
     * Vérifie si l'utilisateur peut accéder à un journal
     */
    @Cacheable(key = "{'journal-access', #authentication.name, #journalId}")
    public boolean canAccessJournal(Authentication authentication, Long journalId) {
        // Implémentation à compléter
        return true; // Pour l'instant, autoriser l'accès en attendant l'implémentation complète
    }

    /**
     * Vérifie si l'utilisateur peut gérer un journal
     */
    @Cacheable(key = "{'journal-management', #authentication.name, #journalId}")
    public boolean canManageJournal(Authentication authentication, Long journalId) {
        // Implémentation à compléter
        return true; // Pour l'instant, autoriser l'accès en attendant l'implémentation complète
    }

    /**
     * Vérifie si l'administrateur est responsable du collecteur
     */
    @Cacheable(key = "{'admin-collecteur', #authentication.name, #collecteurId}")
    public boolean isAdminOfCollecteur(Authentication authentication, Long collecteurId) {
        if (authentication == null) return false;

        if (hasRole(authentication.getAuthorities(), RoleConfig.SUPER_ADMIN)) {
            return true;
        }

        String userEmail = authentication.getName();

        if (hasRole(authentication.getAuthorities(), RoleConfig.ADMIN)) {
            return adminRepository.findByAdresseMail(userEmail)
                    .map(admin -> {
                        Optional<Collecteur> collecteurOpt = collecteurRepository.findById(collecteurId);
                        if (collecteurOpt.isEmpty()) return false;

                        return collecteurOpt.get().getAgence().getId().equals(admin.getAgence().getId());
                    })
                    .orElse(false);
        }

        return false;
    }

    /**
     * Vérifie si le client est dans la même agence que le collecteur
     */
    @Cacheable(key = "{'client-in-collecteur-agence', #clientId, #collecteurId}")
    public boolean isClientInCollecteurAgence(Long clientId, Long collecteurId) {
        // Version optimisée qui évite les lazy loading
        try {
            // Récupérer directement l'ID de l'agence du collecteur
            Long collecteurAgenceId = collecteurRepository.findAgenceIdByCollecteurId(collecteurId);
            if (collecteurAgenceId == null) return false;

            // Récupérer directement l'ID de l'agence du client
            Long clientAgenceId = clientRepository.findAgenceIdByClientId(clientId);
            if (clientAgenceId == null) return false;

            // Vérifier que les deux sont dans la même agence
            return collecteurAgenceId.equals(clientAgenceId);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification client-agence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si l'utilisateur peut réinitialiser le mot de passe
     */
    public boolean canResetPassword(Authentication authentication, Long userId) {
        if (authentication == null) return false;

        String userEmail = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        // Super Admin peut réinitialiser n'importe quel mot de passe
        if (hasRole(authorities, RoleConfig.SUPER_ADMIN)) {
            log.debug("Super Admin {} autorisé à réinitialiser le mot de passe pour {}", userEmail, userId);
            return true;
        }

        // Admin ne peut réinitialiser que les mots de passe des utilisateurs de son agence
        if (hasRole(authorities, RoleConfig.ADMIN)) {
            return adminRepository.findByAdresseMail(userEmail)
                    .map(admin -> {
                        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(userId);
                        if (utilisateur.isEmpty()) return false;

                        if (utilisateur.get() instanceof Collecteur) {
                            Collecteur collecteur = (Collecteur) utilisateur.get();
                            return collecteur.getAgence().getId().equals(admin.getAgence().getId());
                        }

                        return false;
                    })
                    .orElse(false);
        }

        // Un utilisateur peut réinitialiser son propre mot de passe
        Optional<Utilisateur> utilisateur = utilisateurRepository.findByAdresseMail(userEmail);
        return utilisateur.map(u -> u.getId().equals(userId)).orElse(false);
    }

    /**
     * Obtient le nom d'utilisateur actuellement connecté
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    /**
     * Vide le cache de sécurité périodiquement
     * Cela permet de rafraîchir les autorisations quand les rôles changent
     */
    @Scheduled(fixedRate = 3600000) // Toutes les heures
    @CacheEvict(allEntries = true)
    public void clearSecurityCache() {
        log.info("Vidage du cache de sécurité");
    }

    /**
     * Vide explicitement le cache pour un utilisateur spécifique
     * À appeler quand les rôles ou affiliations d'un utilisateur changent
     */
    @CacheEvict(allEntries = true)
    public void clearCacheForUser(String username) {
        log.info("Vidage du cache de sécurité pour l'utilisateur: {}", username);
    }

    /**
     * Vérifie si l'utilisateur peut accéder à un collecteur
     */
    @Cacheable(key = "{'collecteur-access-check', #authentication.name, #collecteurId}")
    public boolean canAccessCollecteur(Authentication authentication, Long collecteurId) {
        return canManageCollecteur(authentication, collecteurId);
    }
}
