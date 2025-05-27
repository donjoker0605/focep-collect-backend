package org.example.collectfocep.web.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@Slf4j
public class DiagnosticController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("timestamp", new Date());
        response.put("message", "API server is up and running");
        response.put("server", "Spring Boot");
        return ResponseEntity.ok(response);
    }

    // Endpoint pour tester la configuration
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("clientIp", request.getRemoteAddr());
        response.put("serverHost", request.getServerName());
        response.put("serverPort", request.getServerPort());
        response.put("origin", request.getHeader("Origin"));
        return ResponseEntity.ok(response);
    }

    /**
     * Test avec authentification
     */
    @GetMapping("/auth-test")
    public ResponseEntity<Map<String, Object>> testAuth() {
        log.info("🔐 AUTH-TEST ENDPOINT APPELÉ !");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("username", auth != null ? auth.getName() : "anonymous");
        response.put("authorities", auth != null ? auth.getAuthorities().toString() : "none");
        response.put("principal", auth != null ? auth.getPrincipal().getClass().getSimpleName() : "none");

        if (auth != null && auth.getDetails() != null) {
            response.put("details", auth.getDetails().toString());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Test avec paramètre d'URL
     */
    @GetMapping("/collecteur/{id}")
    public ResponseEntity<Map<String, Object>> testCollecteurEndpoint(@PathVariable Long id) {
        log.info("🎯 TEST COLLECTEUR ENDPOINT APPELÉ avec ID: {}", id);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("collecteurId", id);
        response.put("message", "Paramètre reçu correctement");

        return ResponseEntity.ok(response);
    }

    /**
     * Test dashboard simple
     */
    @GetMapping("/dashboard/{id}")
    public ResponseEntity<Map<String, Object>> testDashboard(@PathVariable Long id) {
        log.info("🎯 TEST DASHBOARD ENDPOINT APPELÉ avec ID: {}", id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("collecteurId", id);
        response.put("authenticated", auth != null && auth.isAuthenticated());
        response.put("username", auth != null ? auth.getName() : "anonymous");

        return ResponseEntity.ok(response);
    }
}