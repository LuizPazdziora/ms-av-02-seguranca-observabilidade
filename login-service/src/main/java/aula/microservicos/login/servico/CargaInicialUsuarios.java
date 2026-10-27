package aula.microservicos.login.servico;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
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
    private JdbcUserDetailsManager userDetailsManager = null;

    public CargaInicialUsuarios(UsuarioRepository usuarioRepo, LoginAttemptRepository tentativaRepo,
            PasswordEncoder passwordEncoder, JdbcUserDetailsManager userDetailsManager) {
        this.usuarioRepo = usuarioRepo;
        this.tentativaRepo = tentativaRepo;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsManager = userDetailsManager;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // cria somente se ainda nao existir; nunca sobrescreve senhas alteradas pela API
        for (String username : USUARIOS_SISTEMA) {
            criarUsuarioSistema(username, senhaUsuarios);
        }
        for (UsuarioEntity usuario : usuarioRepo.findAll()) {
            sincronizarComUsers(usuario);
        }
        // operadores existem somente em users/authorities (nao aparecem no GET / e nao sao bloqueados pelo POST /login)
        criarOperador("admin", senhaAdmin, "ADMIN");
        criarOperador("manutencao", senhaManutencao, "MANUTENCAO");
    }

    private void criarOperador(String username, String senha, String role) {

        if (userDetailsManager.userExists(username)) {
            return;
        }

        if (senha == null || senha.isBlank()) {
            logger.warn("Senha de demonstracao nao definida; usuario nao criado: " + username);
            return;
        }

        userDetailsManager.createUser(User.withUsername(username).password(passwordEncoder.encode(senha)).roles(role).build());
        logger.info("Usuario inicial criado: " + username);
    }

    // garante a regra de consistencia: todo usuario de usuarios tem uma linha em users com o mesmo hash,
    // ROLE_USER e enabled = NOT login_attempts.locked (inclusive para bancos criados antes da fase 3)
    private void sincronizarComUsers(UsuarioEntity usuario) {

        if (userDetailsManager.userExists(usuario.getUsername())) {
            return;
        }

        Optional<LoginAttemptEntity> tentativa = tentativaRepo.findById(usuario.getUsername());
        boolean bloqueado = tentativa.isPresent() && tentativa.get().isLocked();
        userDetailsManager.createUser(User.withUsername(usuario.getUsername()).password(usuario.getSenha())
                .roles("USER").disabled(bloqueado).build());
        logger.info("Usuario sincronizado com users: " + usuario.getUsername());
    }

    private void criarUsuarioSistema(String username, String senha) {

        Optional<UsuarioEntity> existente = usuarioRepo.findById(username);
        if (existente.isPresent()) {
            sincronizarComUsers(existente.get());
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
        userDetailsManager.createUser(User.withUsername(username).password(hash).roles("USER").build());
        logger.info("Usuario inicial criado: " + username);
    }

}
