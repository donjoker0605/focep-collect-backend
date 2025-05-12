package org.example.collectfocep.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI collecteFocepAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API FOCEP Collecte Journalière")
                        .description("API pour la gestion des collectes journalières, calcul des commissions et répartition des rémunérations")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe FOCEP")
                                .email("support@focep.com"))
                        .license(new License()
                                .name("Propriétaire")
                                .url("https://www.focep.com")))
                .tags(List.of(
                        new Tag().name("Collecteurs").description("Opérations sur les collecteurs"),
                        new Tag().name("Clients").description("Opérations sur les clients"),
                        new Tag().name("Mouvements").description("Opérations d'épargne et de retrait"),
                        new Tag().name("Commissions").description("Calcul et répartition des commissions"),
                        new Tag().name("Rapports").description("Génération de rapports")
                ));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/admin/**", "/api/collecteurs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi collecteursApi() {
        return GroupedOpenApi.builder()
                .group("collecteurs")
                .pathsToMatch("/api/collecteurs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi clientsApi() {
        return GroupedOpenApi.builder()
                .group("clients")
                .pathsToMatch("/api/clients/**")
                .build();
    }

    @Bean
    public GroupedOpenApi mouvementsApi() {
        return GroupedOpenApi.builder()
                .group("mouvements")
                .pathsToMatch("/api/mouvements/**")
                .build();
    }

    @Bean
    public GroupedOpenApi commissionsApi() {
        return GroupedOpenApi.builder()
                .group("commissions")
                .pathsToMatch("/api/commissions/**")
                .build();
    }

    @Bean
    public GroupedOpenApi rapportsApi() {
        return GroupedOpenApi.builder()
                .group("rapports")
                .pathsToMatch("/api/reports/**")
                .build();
    }
}