package aula.microservicos.login.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// profile padrao: Basic + JdbcUserDetailsManager (tabelas users/authorities) com roles
@Configuration
@Profile("!oauth2")
public class SecurityConfig {

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests((requests) -> requests
                .requestMatchers(HttpMethod.POST, "/login").permitAll()
                .requestMatchers(HttpMethod.PUT, "/trocarsenha/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.PUT, "/unlock/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/").hasRole("MANUTENCAO")
                .requestMatchers(HttpMethod.GET, "/").hasRole("MANUTENCAO")
                .anyRequest().authenticated());

        // o formLogin padrao intercepta POST /login - precisa ser desabilitado
        http.formLogin(config -> config.disable());
        http.httpBasic(withDefaults());
        // API stateless (sem cookie de sessao): CSRF desabilitado
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
