package fr.michelin.guide.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Description de l'API affichée par Swagger (/swagger-ui.html et /v3/api-docs.yaml)
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "API Guide Michelin", version = "1.0",
                description = "Gestion des restaurants et de leurs évaluations"),
        security = @SecurityRequirement(name = "keycloak"))
// Les routes protégées attendent un token JWT Keycloak (bouton "Authorize" dans Swagger)
@SecurityScheme(name = "keycloak", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
