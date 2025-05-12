package org.example.collectfocep.collectfocep.services.impl;

import org.example.collectfocep.security.service.SecurityService;
import org.example.collectfocep.services.impl.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Arrays;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private SecurityService securityService;

    @Mock
    private Cache clientsCache;

    @Mock
    private Cache collecteursCache;

    @Mock
    private Cache securityCache;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCacheNames()).thenReturn(Arrays.asList("clients", "collecteurs", "security-permissions"));
        when(cacheManager.getCache("clients")).thenReturn(clientsCache);
        when(cacheManager.getCache("collecteurs")).thenReturn(collecteursCache);
        when(cacheManager.getCache("security-permissions")).thenReturn(securityCache);
    }

    @Test
    void testClearAllCaches() {
        // Arrange
        doNothing().when(clientsCache).clear();
        doNothing().when(collecteursCache).clear();
        doNothing().when(securityCache).clear();

        // Act
        cacheService.clearAllCaches();

        // Assert
        verify(cacheManager).getCacheNames();
        verify(cacheManager, times(3)).getCache(anyString());
        verify(clientsCache).clear();
        verify(collecteursCache).clear();
        verify(securityCache).clear();
    }

    @Test
    void testClearSecurityCacheForUser() {
        // Arrange
        String username = "testuser";
        doNothing().when(securityService).clearCacheForUser(username);

        // Act
        cacheService.clearSecurityCacheForUser(username);

        // Assert
        verify(securityService).clearCacheForUser(username);
    }

    @Test
    void testClearCacheEntry() {
        // Arrange
        String cacheName = "clients";
        Long key = 123L;
        doNothing().when(clientsCache).evict(key);

        // Act
        cacheService.clearCacheEntry(cacheName, key);

        // Assert
        verify(cacheManager).getCache(cacheName);
        verify(clientsCache).evict(key);
    }
}