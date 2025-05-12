package org.example.collectfocep.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.vote.RoleHierarchyVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.authorization.AuthorityAuthorizationManager;

@Configuration
public class RoleConfig {
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String COLLECTEUR = "COLLECTEUR";

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("""
            ROLE_SUPER_ADMIN > ROLE_ADMIN
            ROLE_ADMIN > ROLE_COLLECTEUR
        """);
        return hierarchy;
    }

    @Bean
    public AuthorityAuthorizationManager<Object> authorityAuthorizationManager() {
        return AuthorityAuthorizationManager.hasRole(ADMIN);
    }
}
