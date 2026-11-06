package fr.michelin.guide.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// Sécurité de l'API : authentification par token JWT délivré par Keycloak
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Client Keycloak dont on lit les rôles dans le token
    private static final String CLIENT_ID = "coursm2";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // API sans session ni cookie : la protection CSRF n'est pas utile
        http.csrf(csrf -> csrf.disable());
        // Permet l'affichage de la console H2 dans le navigateur
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        // Vérification du token JWT et lecture des rôles USER / ADMIN
        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                // Token invalide ou expiré : même format d'erreur que le reste de l'API
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Token absent, invalide ou expiré\"}");
                }));
        return http.build();
    }

    // Utilise les rôles du token pour les autorisations (@PreAuthorize)
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extraireRoles);
        return converter;
    }

    // Les rôles sont dans resource_access.coursm2.roles, ils deviennent ROLE_USER / ROLE_ADMIN
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extraireRoles(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || !resourceAccess.containsKey(CLIENT_ID)) {
            return authorities;
        }
        Map<String, Object> client = (Map<String, Object>) resourceAccess.get(CLIENT_ID);
        List<String> roles = (List<String>) client.get("roles");
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }
}
