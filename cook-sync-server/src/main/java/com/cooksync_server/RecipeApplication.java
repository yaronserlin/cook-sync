package com.cooksync_server;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for the CookSync Spring Boot backend service.
 * Initializes the Spring ApplicationContext and starts embedded web container.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */



@SpringBootApplication
@EnableScheduling
@EnableAsync
@SecurityScheme(
    name = "bearerAuth", 
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
@SecurityRequirement(name = "bearerAuth")
public class RecipeApplication {

    /**
     * Main execution method launching the Spring Boot framework instance.
     *
     * Complexity:
     * Time: O(N)
     * Space: O(N)
     *
     * @param args command-line input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(RecipeApplication.class, args);
    }
}
