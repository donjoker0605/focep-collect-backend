package org.example.collectfocep.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .interceptors(loggingInterceptor())
                .requestFactory(() -> new BufferingClientHttpRequestFactory(
                        new SimpleClientHttpRequestFactory()))
                .build();
    }

    /**
     * Intercepteur pour logger les requêtes/réponses HTTP (utile pour debug)
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            // Log de la requête
            log.debug("🔷 HTTP Request: {} {}",
                    request.getMethod(),
                    request.getURI());

            // Exécution
            var response = execution.execute(request, body);

            // Log de la réponse
            log.debug("🔶 HTTP Response: {} - {}",
                    response.getStatusCode(),
                    request.getURI());

            return response;
        };
    }
}