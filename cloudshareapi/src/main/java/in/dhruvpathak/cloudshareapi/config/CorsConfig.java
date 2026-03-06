package in.dhruvpathak.cloudshareapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration to allow your Vercel frontend
 * to securely communicate with this Spring Boot API.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // This injects the URL you saved in Render's Environment Variables
    @Value("${ALLOWED_ORIGIN}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all endpoints
                .allowedOrigins(allowedOrigin) // Only allow your specific frontend URL
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Allow all standard REST methods
                .allowedHeaders("*") // Allow all headers (useful for Auth/JSON)
                .allowCredentials(true) // Required if you use cookies or Clerk sessions
                .maxAge(3600); // Cache the CORS response for 1 hour to improve performance
    }
}