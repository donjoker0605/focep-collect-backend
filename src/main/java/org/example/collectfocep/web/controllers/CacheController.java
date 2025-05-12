package org.example.collectfocep.web.controllers;

import org.example.collectfocep.services.impl.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cache")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @PostMapping("/clear-all")
    public ResponseEntity<String> clearAllCaches() {
        cacheService.clearAllCaches();
        return ResponseEntity.ok("Tous les caches ont été vidés");
    }

    @PostMapping("/security/user/{username}")
    public ResponseEntity<String> clearSecurityCacheForUser(@PathVariable String username) {
        cacheService.clearSecurityCacheForUser(username);
        return ResponseEntity.ok("Cache de sécurité vidé pour l'utilisateur: " + username);
    }
}