package org.example.collectfocep.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.collectfocep.security.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SecurityService securityService;

    /**
     * Vide tous les caches de l'application
     */
    public void clearAllCaches() {
        log.info("Nettoyage de tous les caches");
        cacheManager.getCacheNames().forEach(name -> {
            cacheManager.getCache(name).clear();
            log.debug("Cache '{}' vidé", name);
        });
    }

    /**
     * Vide le cache de sécurité pour un utilisateur spécifique
     * Utile lors des changements de droits, rôles ou affiliations
     */
    public void clearSecurityCacheForUser(String username) {
        log.info("Nettoyage du cache de sécurité pour l'utilisateur: {}", username);
        securityService.clearCacheForUser(username);
    }

    /**
     * Vide le cache pour une entité spécifique
     * @param cacheName Nom du cache
     * @param key Clé spécifique à vider
     */
    public void clearCacheEntry(String cacheName, Object key) {
        log.info("Nettoyage du cache '{}' pour la clé: {}", cacheName, key);
        cacheManager.getCache(cacheName).evict(key);
    }
}