package aula.microservicos.login.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// profile oauth2 (extensao opcional): Bearer JWT emitido pelo Keycloak, mesmas regras de acesso, sem Basic
@Configuration
@Profile("oauth2")
public class OAuth2SecurityConfig {

    @Autowired
    JwtConverter jwtConverter;

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests((requests) -> requests
                .requestMatchers(HttpMethod.POST, "/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/token").permitAll()
                .requestMatchers(HttpMethod.PUT, "/trocarsenha/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.PUT, "/unlock/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/").hasRole("MANUTENCAO")
                .requestMatchers(HttpMethod.GET, "/").hasRole("MANUTENCAO")
                .anyRequest().authenticated());

        http.formLogin(config -> config.disable());
        http.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}
