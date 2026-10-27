package aula.microservicos.login.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

// esquema "auth" do profile oauth2: Authorization Code + PKCE no Keycloak (URLs vistas pelo navegador)
@Configuration
@Profile("oauth2")
@SecurityScheme(name = "auth", type = SecuritySchemeType.OAUTH2,
        description = "Bearer JWT emitido pelo Keycloak (Authorization Code + PKCE)",
        flows = @OAuthFlows(authorizationCode = @OAuthFlow(
                authorizationUrl = "http://localhost:9090/realms/seguranca/protocol/openid-connect/auth",
                tokenUrl = "http://localhost:9090/realms/seguranca/protocol/openid-connect/token")))
public class OpenApiOAuth2Config {

}
