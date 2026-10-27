package aula.microservicos.login.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import aula.microservicos.login.entity.LoginAttemptEntity;
import aula.microservicos.login.entity.UsuarioEntity;
import aula.microservicos.login.repository.LoginAttemptRepository;
import aula.microservicos.login.repository.UsuarioRepository;

// carga inicial idempotente dos usuarios de demonstracao (padrao "Aplicacao Console")
// as senhas vem somente do ambiente; usuarios ja existentes nunca sao sobrescritos
@Component
public class CargaInicialUsuarios implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CargaInicialUsuarios.class);

    private static final String[] USUARIOS_SISTEMA = { "joao", "maria" };

    @Value("${demo.admin.senha:}")
    private String senhaAdmin;

    @Value("${demo.manutencao.senha:}")
    private String senhaManutencao;

    @Value("${demo.usuarios.senha:}")
    private String senhaUsuarios;

    private UsuarioRepository usuarioRepo = null;
    private LoginAttemptRepository tentativaRepo = null;
    private PasswordEncoder passwordEncoder = null;

    public CargaInicialUsuarios(UsuarioRepository usuarioRepo, LoginAttemptRepository tentativaRepo,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.tentativaRepo = tentativaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // cria somente se ainda nao existir; nunca sobrescreve senhas alteradas pela API
        for (String username : USUARIOS_SISTEMA) {
            criarUsuarioSistema(username, senhaUsuarios);
        }
    }

    private void criarUsuarioSistema(String username, String senha) {

        if (usuarioRepo.existsById(username)) {
            return;
        }

        if (senha == null || senha.isBlank()) {
            logger.warn("Senha de demonstracao nao definida; usuario nao criado: " + username);
            return;
        }

        String hash = passwordEncoder.encode(senha);
        usuarioRepo.save(new UsuarioEntity(username, hash));
        if (!tentativaRepo.existsById(username)) {
            tentativaRepo.save(new LoginAttemptEntity(username));
        }
        logger.info("Usuario inicial criado: " + username);
    }

}
