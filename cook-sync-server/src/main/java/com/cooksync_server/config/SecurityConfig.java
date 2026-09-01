package com.cooksync_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cooksync_server.constants.ApiRoutes;

import lombok.RequiredArgsConstructor;

/**
 * Main Spring Security configuration class defining authentication filters,
 * password encoders, and URI access rules.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Configures HttpSecurity chain settings, endpoint permissions, stateless
     * session policies, and custom filters.
     *
     * @param http HttpSecurity configuration builder
     * @return constructed SecurityFilterChain bean
     * @throws Exception if security filter building fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_REGISTER, ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_LOGIN,
                        ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_REFRESH_TOKEN,
                        ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_FORGOT_PASSWORD, ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_RESET_PASSWORD,
                        ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_VERIFY_REGISTRATION_OTP, ApiRoutes.AUTH_BASE + ApiRoutes.AUTH_RESEND_REGISTRATION_OTP).permitAll()
                // Docker's/Render's health checks hit this with no JWT to send; only "health" is
                // exposed at all (see management.endpoints.web.exposure.include), and
                // show-details=never keeps its response to a bare status, so nothing sensitive
                // is reachable here.
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures BCrypt password encoder bean for secure password hashing.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
