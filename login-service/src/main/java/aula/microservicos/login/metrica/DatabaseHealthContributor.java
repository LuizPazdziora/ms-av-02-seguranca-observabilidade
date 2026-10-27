package aula.microservicos.login.metrica;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

// health check personalizado: verifica se a tabela de usuarios esta acessivel
@Component("dbLogin")
public class DatabaseHealthContributor implements HealthIndicator {
    @Autowired
    private DataSource ds;

    @Override
    public Health health() {
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("select COUNT(*) from USUARIOS");
        } catch (SQLException ex) {
            return Health.outOfService().withException(ex).build();
        }
        return Health.up().build();
    }
}
