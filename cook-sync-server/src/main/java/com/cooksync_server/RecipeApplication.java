package com.cooksync_server;

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
public class RecipeApplication {

    /**
     * Main execution method launching the Spring Boot framework instance.
     *
     * @param args command-line input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(RecipeApplication.class, args);
    }

}
