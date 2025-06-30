package org.example.collectfocep.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CollectFoCEP API")
                        .version("1.0")
                        .description("Documentation de l'API CollectFoCEP"))
                .tags(List.of(
                        new Tag().name("Audit").description("Journal d'activité")
                        // Ajoutez d'autres tags ici
                ));
    }
}