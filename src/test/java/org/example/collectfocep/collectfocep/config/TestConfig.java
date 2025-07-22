package org.example.collectfocep.collectfocep.config;

import org.example.collectfocep.repositories.AuditLogRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"test", "service-test"})
public class TestConfig {
    @Bean
    @Primary
    public AuditLogRepository testAuditLogRepository() {
        return Mockito.mock(AuditLogRepository.class);
    }
}

