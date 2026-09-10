package com.alanensina.inventory.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;

@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name}")
    private String title;

    @Value("${spring.application.version}")
    private String version;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version(version)
                        .description("A simple API to manage inventory."));
    }

}
