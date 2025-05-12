package org.example.collectfocep.collectfocep.config;

import org.example.collectfocep.aspects.AuditAspect;
import org.example.collectfocep.repositories.*;
import org.example.collectfocep.security.service.CustomUserDetailsService;
import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.security.filters.JwtAuthenticationFilter;
import org.example.collectfocep.security.filters.UserManagementFilter;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
@Profile("controller-test")
public class TestControllerConfig {

    @Bean
    @Primary
    public AuditLogRepository controllerAuditLogRepository() {
        return Mockito.mock(AuditLogRepository.class);
    }

    @Bean
    @Primary
    public AuditAspect auditAspect(AuditLogRepository auditLogRepository) {
        return new AuditAspect(auditLogRepository);
    }

    @Bean
    @Primary
    public UtilisateurRepository utilisateurRepository() {
        return Mockito.mock(UtilisateurRepository.class);
    }

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }

    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return Mockito.mock(JwtAuthenticationFilter.class);
    }

    @Bean
    @Primary
    public UserManagementFilter userManagementFilter() {
        return Mockito.mock(UserManagementFilter.class);
    }

    @Bean
    @Primary
    public AuthenticationManager authenticationManager() {
        return Mockito.mock(AuthenticationManager.class);
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return Mockito.mock(PasswordEncoder.class);
    }
}