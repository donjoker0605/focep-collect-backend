package org.example.collectfocep.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 🔥 Configuration des caches - VERSION CORRIGÉE
 * ✅ Inclut TOUS les caches référencés dans le code
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                // ================================
                // 🔥 CACHES EXISTANTS (conservés)
                // ================================
                new ConcurrentMapCache("clients"),
                new ConcurrentMapCache("collecteurs"),
                new ConcurrentMapCache("comptes"),
                new ConcurrentMapCache("security-permissions"),
                new ConcurrentMapCache("dashboard-data"),
                new ConcurrentMapCache("client-stats"), // 🔥 CORRIGÉ: était "client-stafs"
                new ConcurrentMapCache("admin-activities"),
                new ConcurrentMapCache("admin-dashboard"),
                new ConcurrentMapCache("admin-notifications"),
                new ConcurrentMapCache("notifications"),

                // ================================
                // 🔥 CACHES JOURNAUX (ajoutés)
                // ================================
                new ConcurrentMapCache("journaux"),
                new ConcurrentMapCache("journal-actuel"),
                new ConcurrentMapCache("monthly-entries"), // 🔥 CRITIQUE: Cache manquant
                new ConcurrentMapCache("journal-range"),
                new ConcurrentMapCache("journal-collecteur-range"),

                // ================================
                // 🔥 CACHES MOUVEMENTS (ajoutés)
                // ================================
                new ConcurrentMapCache("mouvements"),
                new ConcurrentMapCache("mouvement-journal"),
                new ConcurrentMapCache("mouvement-collecteur"),

                // ================================
                // 🔥 CACHES VERSEMENTS (ajoutés)
                // ================================
                new ConcurrentMapCache("versements"),
                new ConcurrentMapCache("versement-preview"),
                new ConcurrentMapCache("versement-stats"),

                // ================================
                // 🔥 CACHES COMPTES AVANCÉS (ajoutés)
                // ================================
                new ConcurrentMapCache("compte-service"),
                new ConcurrentMapCache("compte-manquant"),
                new ConcurrentMapCache("compte-agence"),
                new ConcurrentMapCache("comptes-collecteur"),

                // ================================
                // 🔥 CACHES RAPPORTS (ajoutés)
                // ================================
                new ConcurrentMapCache("rapport-mensuel"),
                new ConcurrentMapCache("rapport-collecteur"),
                new ConcurrentMapCache("rapport-agence"),
                new ConcurrentMapCache("statistiques-globales"),

                // ================================
                // 🔥 CACHES GÉOLOCALISATION (ajoutés)
                // ================================
                new ConcurrentMapCache("geocoding-results"),
                new ConcurrentMapCache("client-locations"),
                new ConcurrentMapCache("collecteur-zones"),

                // ================================
                // 🔥 CACHES AUDIT ET SÉCURITÉ (ajoutés)
                // ================================
                new ConcurrentMapCache("audit-logs"),
                new ConcurrentMapCache("user-permissions"),
                new ConcurrentMapCache("agence-permissions")
        ));
        return cacheManager;
    }
}