package org.example.collectfocep.security.filters;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.security.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(1)
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = null;

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        if (token != null && jwtUtil.validateToken(token)) {
            try {
                String username = jwtUtil.getUsernameFromJWT(token);
                String role = jwtUtil.getRoleFromJWT(token);
                Long userId = jwtUtil.getUserIdFromJWT(token);
                Long agenceId = jwtUtil.getAgenceIdFromJWT(token);

                log.debug("🔐 JWT décodé: username={}, role={}, userId={}, agenceId={}",
                        username, role, userId, agenceId);

                if (role != null && !role.trim().isEmpty()) {
                    // ✅ CORRECTION CRITIQUE: Créer un Principal personnalisé avec toutes les infos
                    JwtUserPrincipal principal = new JwtUserPrincipal(username, userId, agenceId, role);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal, // ✅ Principal personnalisé au lieu du simple username
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(role))
                    );

                    // ✅ Ajouter des détails supplémentaires dans l'Authentication
                    Map<String, Object> details = new HashMap<>();
                    details.put("userId", userId);
                    details.put("agenceId", agenceId);
                    details.put("role", role);
                    auth.setDetails(details);

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("✅ Authentication définie pour: {} (userId={})", username, userId);
                }
            } catch (Exception e) {
                log.error("❌ Erreur lors du traitement du JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    @VisibleForTesting
    public void doFilterInternalForTest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        doFilterInternal(request, response, filterChain);
    }

    // ✅ CLASSE INTERNE pour le Principal personnalisé
    public static class JwtUserPrincipal {
        private final String username;
        private final Long userId;
        private final Long agenceId;
        private final String role;

        public JwtUserPrincipal(String username, Long userId, Long agenceId, String role) {
            this.username = username;
            this.userId = userId;
            this.agenceId = agenceId;
            this.role = role;
        }

        public String getUsername() { return username; }
        public Long getUserId() { return userId; }
        public Long getAgenceId() { return agenceId; }
        public String getRole() { return role; }

        @Override
        public String toString() {
            return username; // Pour compatibilité avec authentication.getName()
        }
    }
}