package org.example.collectfocep.web.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
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
}