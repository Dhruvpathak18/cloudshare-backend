package in.dhruvpathak.cloudshareapi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ClerkJwtAuthFilter extends OncePerRequestFilter {
    @Value("${clerk.issuer}")
    private String clerkIssuer;
    private final ClerkJwksProvider jwksProvider;

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().contains("/webhooks") && !request.getRequestURI().contains("/public") && !request.getRequestURI().contains("/download") && !request.getRequestURI().contains("/health")) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    String[] chunks = token.split("\\.");
                    if (chunks.length < 3) {
                        response.sendError(403, "Invalid JWT token format");
                    } else {
                        String headerJson = new String(Base64.getUrlDecoder().decode(chunks[0]));
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode headerNode = mapper.readTree(headerJson);
                        if (!headerNode.has("kid")) {
                            response.sendError(403, "Token header is missing kid");
                        } else {
                            String kid = headerNode.get("kid").asText();
                            PublicKey publicKey = this.jwksProvider.getPublicKey(kid);
                            Claims claims = (Claims)Jwts.parserBuilder().setSigningKey(publicKey).setAllowedClockSkewSeconds(60L).requireIssuer(this.clerkIssuer).build().parseClaimsJws(token).getBody();
                            String clerkId = claims.getSubject();
                            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(clerkId, (Object)null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
                            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                            filterChain.doFilter(request, response);
                        }
                    }
                } catch (Exception var15) {
                    response.sendError(403, "Invalid JWT token: " + var15.getMessage());
                }
            } else {
                response.sendError(403, "Authorization header missing/invalid");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    @Generated
    public ClerkJwtAuthFilter(final ClerkJwksProvider jwksProvider) {
        this.jwksProvider = jwksProvider;
    }
}
