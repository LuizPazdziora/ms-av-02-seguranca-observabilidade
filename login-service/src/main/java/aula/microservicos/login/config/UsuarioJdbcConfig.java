package aula.microservicos.login.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

// sem profile: os dois profiles (padrao e oauth2) usam o bean para sincronizar os usuarios
@Configuration
public class UsuarioJdbcConfig {

    @Bean
    public JdbcUserDetailsManager userDetailsService(DataSource datasource) {

        return new JdbcUserDetailsManager(datasource);
    }
}
