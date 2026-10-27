package aula.microservicos.login.metrica;

import org.springframework.stereotype.Component;

import aula.microservicos.login.repository.LoginAttemptRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

// indicadores do POST /login:
// - Counter (login.validos / login.invalidos): registrados com 0 e zerados quando o servico reinicia
// - Gauge (usuarios.bloqueados): consulta o banco a cada leitura, refletindo sempre o estado real
@Component
public class MetricaLogin {

    private Counter loginValido;
    private Counter loginInvalido;

    MetricaLogin(MeterRegistry registry, LoginAttemptRepository tentativaRepo) {
        loginValido = registry.counter("login.validos", Tags.of("servico", "login"));
        loginInvalido = registry.counter("login.invalidos", Tags.of("servico", "login"));
        Gauge.builder("usuarios.bloqueados", tentativaRepo, repo -> repo.countByLocked(true))
                .tags(Tags.of("servico", "login"))
                .register(registry);
    }

    public void registrarLoginValido() {
        loginValido.increment();
    }

    public void registrarLoginInvalido() {
        loginInvalido.increment();
    }
}
