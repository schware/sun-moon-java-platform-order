package com.sunmoon.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sunMoonOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("sun-moon-java-platform-order API")
                        .description("DDD-based order service — REST surface of the sun-moon-java-platform Order service.")
                        .version("0.1.0"))
                .servers(List.of(new Server().url("/")));
    }
}
