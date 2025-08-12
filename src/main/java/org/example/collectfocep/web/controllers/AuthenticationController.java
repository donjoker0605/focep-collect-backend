package org.example.collectfocep.web.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.example.collectfocep.dto.LoginRequest;
import org.example.collectfocep.entities.Utilisateur;
import org.example.collectfocep.security.jwt.JwtUtil;
import org.example.collectfocep.services.impl.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuditService auditService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("🔐 Tentative de connexion pour l'utilisateur: {}", loginRequest.getEmail());

        try {
            // Validation explicite des données
            if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
                log.warn("❌ Email manquant dans la requête de connexion");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Email requis");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                log.warn("❌ Mot de passe manquant dans la requête de connexion");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Mot de passe requis");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.debug("📝 Données reçues - Email: {}, Password: [MASQUÉ]", loginRequest.getEmail());

            // Authentification de l'utilisateur
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail().trim(),
                            loginRequest.getPassword()
                    )
            );

            log.info("✅ Authentification réussie pour: {}", loginRequest.getEmail());

            // Génération du token JWT
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().iterator().next().getAuthority();
            String token = jwtUtil.generateToken(userDetails.getUsername(), role);

            log.info("🎫 Token généré avec succès pour l'utilisateur: {} avec le rôle: {}",
                    loginRequest.getEmail(), role);

            // Réponse structurée
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role", role);
            response.put("message", "Connexion réussie");

            // Enregistrer l'audit de connexion
            String deviceInfo = request.getHeader("User-Agent");
            auditService.logAction("LOGIN", "AUTHENTICATION",
                    null, "Connexion réussie depuis " + deviceInfo);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException ex) {
            log.warn("❌ Identifiants invalides pour l'utilisateur: {}", loginRequest.getEmail());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Identifiants invalides");
            errorResponse.put("message", "Email ou mot de passe incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (LockedException ex) {
            log.warn("🔒 Compte verrouillé pour l'utilisateur: {}", loginRequest.getEmail());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Compte verrouillé");
            errorResponse.put("message", "Votre compte a été temporairement verrouillé");
            return ResponseEntity.status(HttpStatus.LOCKED).body(errorResponse);

        } catch (DisabledException ex) {
            log.warn("🚫 Compte désactivé pour l'utilisateur: {}", loginRequest.getEmail());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Compte désactivé");
            errorResponse.put("message", "Votre compte a été désactivé");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (AccountExpiredException ex) {
            log.warn("⏰ Compte expiré pour l'utilisateur: {}", loginRequest.getEmail());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Compte expiré");
            errorResponse.put("message", "Votre compte a expiré");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (CredentialsExpiredException ex) {
            log.warn("🔑 Credentials expirés pour l'utilisateur: {}", loginRequest.getEmail());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Mot de passe expiré");
            errorResponse.put("message", "Votre mot de passe a expiré");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);

        } catch (Exception ex) {
            log.error("💥 Erreur inattendue lors de l'authentification pour {}: {}",
                    loginRequest.getEmail(), ex.getMessage(), ex);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur serveur");
            errorResponse.put("message", "Une erreur inattendue s'est produite");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, Authentication authentication) {
        log.info("👋 Demande de déconnexion reçue");

        try {
            String token = extractTokenFromRequest(request);
            String userEmail = authentication != null ? authentication.getName() : "Utilisateur inconnu";
            
            log.info("✅ Déconnexion traitée pour l'utilisateur: {}", userEmail);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Déconnexion réussie");
            response.put("status", "success");
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));

            if (authentication != null) {
                auditService.logAction("LOGOUT", "AUTHENTICATION",
                        null, "Déconnexion utilisateur: " + authentication.getName());
            }

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la déconnexion: {}", e.getMessage(), e);
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Déconnexion effectuée avec avertissement");
            errorResponse.put("status", "warning");
            errorResponse.put("error", "Token processing failed but logout completed");
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken() {
        log.debug("🔍 Vérification de token demandée");

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("message", "Token valide");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}