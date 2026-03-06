package in.dhruvpathak.cloudshareapi.config;

import in.dhruvpathak.cloudshareapi.security.ClerkJwtAuthFilter;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClerkJwtAuthFilter clerkJwtAuthFilter;

    public SecurityConfig(ClerkJwtAuthFilter clerkJwtAuthFilter) {
        this.clerkJwtAuthFilter = clerkJwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Links to the corsConfigurationSource bean below
                .csrf(AbstractHttpConfigurer::disable) // Required for stateless APIs
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/register",      // Permit user sync
                                "/webhooks/**",
                                "/files/public/**",
                                "/files/download/**",
                                "/users/credits", // Permit initial dashboard load
                                "/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(clerkJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allows any origin to resolve CORS issues during testing
        config.setAllowedOriginPatterns(List.of("*"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allows all headers (Crucial for Clerk/Axios headers)
        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}