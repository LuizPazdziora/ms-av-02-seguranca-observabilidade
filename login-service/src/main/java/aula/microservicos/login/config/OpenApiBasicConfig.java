package aula.microservicos.login.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

// esquema "auth" do profile padrao: autenticacao Basic dos operadores
@Configuration
@Profile("!oauth2")
@SecurityScheme(name = "auth", type = SecuritySchemeType.HTTP, scheme = "basic", description = "Basic - operadores admin e manutencao")
public class OpenApiBasicConfig {

}
