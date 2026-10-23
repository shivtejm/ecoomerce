package com.example.RegisterPage.config;

import com.example.RegisterPage.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .httpBasic(httpBasic -> httpBasic.disable())

                .logout(logout -> logout.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Unauthorized. Please log in.\"}");
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/login", "/api/register", "/api/logout", "/api/forgot-password", "/api/reset-password", "/api/validate-reset-token"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**", "/api/products", "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/products", "/api/products/**").authenticated()
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByEmail(username)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(Collections.emptyList())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // Pre-warm BCrypt / SecureRandom on application startup so the first registration is instant
        encoder.encode("startup-warmup");
        return encoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "https://*.ngrok-free.dev",
                "https://*.ngrok-free.app",
                "https://*.ngrok.io",
                "http://172.16.*.*:*"
        ));

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(List.of("*"));

        // Required so the browser stores/sends the JWT cookie
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}