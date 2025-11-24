package com.pauloneill.arcraidersplanner.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for ARC Raiders Loot Planner API.
 * WHY: Provides interactive API documentation at /swagger-ui.html
 * for developers and frontend integration.
 */
@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI arcRaidersPlannerOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("ARC Raiders Loot Planner API")
                                                .description("""
                                                                REST API for optimizing raid routes in ARC Raiders.

                                                                **Core Features:**
                                                                - Search items and enemies from the Metaforge API
                                                                - Recommend optimal maps based on loot types
                                                                - Generate optimized routes with 4 routing profiles:
                                                                  - PURE_SCAVENGER: Maximize loot area count
                                                                  - EASY_EXFIL: Prioritize Raider Hatch proximity
                                                                  - AVOID_PVP: Edge positioning + High Tier zone avoidance
                                                                  - SAFE_EXFIL: Combined safety + extraction optimization
                                                                - Target specific ARC enemies for bonus routing points
                                                                """)
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("Paul O'Neill")
                                                                .url("https://github.com/paul-anthony-oneill"))
                                                .license(new License()
                                                                .name("MIT License")
                                                                .url("https://opensource.org/licenses/MIT")))
                                .servers(List.of(
                                                new Server()
                                                                .url("http://localhost:8080")
                                                                .description("Development Server")));
        }
}
