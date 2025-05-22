package org.example.collectfocep.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/diagnostic/ping")
    public ResponseEntity<?> diagnosticPing() {
        log.info("🏓 Ping reçu - serveur opérationnel");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "FOCEP Collect API is up and running");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("version", "1.0.0");

        return ResponseEntity.ok("Diagnostic actif");
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}