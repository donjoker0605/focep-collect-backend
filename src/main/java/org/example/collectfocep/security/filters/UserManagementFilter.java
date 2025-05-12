package org.example.collectfocep.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class UserManagementFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Si l'URI commence par /api/users, on vérifie que l'utilisateur authentifié a le rôle ADMIN
        if (request.getRequestURI().startsWith("/api/users")) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getAuthorities().stream().noneMatch(
                    auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès interdit aux endpoints de gestion des utilisateurs");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

