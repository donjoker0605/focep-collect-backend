package org.example.collectfocep.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.entities.Admin;
import org.example.collectfocep.entities.Collecteur;
import org.example.collectfocep.repositories.AdminRepository;
import org.example.collectfocep.repositories.CollecteurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpirationInMs;

    private Key key;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CollecteurRepository collecteurRepository;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    // Méthode principale avec tous les paramètres requis
    public String generateToken(String username, String role, Long userId, Long agenceId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("userId", userId)
                .claim("agenceId", agenceId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    // Méthode améliorée qui récupère automatiquement userId et agenceId
    public String generateToken(String username, String role) {
        Long userId = -1L;
        Long agenceId = -1L;

        // Rechercher l'utilisateur et récupérer ses informations en fonction du rôle
        if (role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER_ADMIN")) {
            Optional<Admin> adminOpt = adminRepository.findByAdresseMail(username);
            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                userId = admin.getId();
                if (admin.getAgence() != null) {
                    agenceId = admin.getAgence().getId();
                } else if (role.equals("ROLE_SUPER_ADMIN")) {
                    // SuperAdmin n'a pas d'agence spécifique
                    agenceId = null;
                }
                log.info("Admin/SuperAdmin trouvé: id={}, agenceId={}, role={}", userId, agenceId, role);
            } else {
                log.warn("Admin/SuperAdmin non trouvé pour l'email: {}", username);
            }
        } else if (role.equals("ROLE_COLLECTEUR")) {
            Optional<Collecteur> collecteurOpt = collecteurRepository.findByAdresseMail(username);
            if (collecteurOpt.isPresent()) {
                Collecteur collecteur = collecteurOpt.get();
                userId = collecteur.getId();
                if (collecteur.getAgence() != null) {
                    agenceId = collecteur.getAgence().getId();
                }
                log.info("Collecteur trouvé: id={}, agenceId={}", userId, agenceId);
            } else {
                log.warn("Collecteur non trouvé pour l'email: {}", username);
            }
        }

        return generateToken(username, role, userId, agenceId);
    }

    public String getUsernameFromJWT(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getRoleFromJWT(String token) {
        return (String) Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
    }

    public Long getUserIdFromJWT(String token) {
        Object userIdClaim = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId");
        return userIdClaim != null ? ((Number) userIdClaim).longValue() : null;
    }

    public Long getAgenceIdFromJWT(String token) {
        Object agenceIdClaim = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .get("agenceId");
        return agenceIdClaim != null ? ((Number) agenceIdClaim).longValue() : null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException ex) {
            log.error("Erreur lors de la validation du token: {}", ex.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}