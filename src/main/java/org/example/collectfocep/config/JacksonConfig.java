package org.example.collectfocep.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration Jackson pour éviter les récursions infinies et gérer correctement la sérialisation
 * Cette configuration est OBLIGATOIRE pour éviter les StackOverflowError sur les entités JPA
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // ✅ CONFIGURATION ANTI-RÉCURSION
        // Évite les erreurs sur les beans vides (entités avec relations lazy)
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Ignore les propriétés inconnues lors de la désérialisation
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // N'inclut que les propriétés non nulles dans le JSON
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // ✅ GESTION DES DATES
        // Support pour LocalDateTime, LocalDate, etc.
        mapper.registerModule(new JavaTimeModule());

        // Sérialise les dates au format ISO-8601 au lieu de timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // ✅ GESTION DES RELATIONS JPA
        // Évite les problèmes avec les proxies Hibernate
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        return mapper;
    }
}