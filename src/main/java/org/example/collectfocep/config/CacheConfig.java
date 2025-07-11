package org.example.collectfocep.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("clients"),
                new ConcurrentMapCache("collecteurs"),
                new ConcurrentMapCache("comptes"),
                new ConcurrentMapCache("security-permissions"),
                new ConcurrentMapCache("journal-actuel"),
                new ConcurrentMapCache("journaux"),
                new ConcurrentMapCache("dashboard-data"),
                new ConcurrentMapCache("client-stafs"),
                new ConcurrentMapCache("admin-activities"),
                new ConcurrentMapCache("admin-dashboard"),
                new ConcurrentMapCache("admin-notifications")
        ));
        return cacheManager;
    }
}