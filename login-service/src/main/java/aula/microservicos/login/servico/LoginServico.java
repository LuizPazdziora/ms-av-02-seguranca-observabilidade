package aula.microservicos.login.servico;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import aula.microservicos.login.bean.UsuarioBean;
import aula.microservicos.login.entity.LoginAttemptEntity;
import aula.microservicos.login.entity.UsuarioEntity;
import aula.microservicos.login.exception.SenhaForaDoPadraoException;
import aula.microservicos.login.exception.SenhaInvalidaException;
import aula.microservicos.login.exception.UsuarioBloqueadoException;
import aula.microservicos.login.exception.UsuarioJaCadastradoException;
import aula.microservicos.login.exception.UsuarioNaoLocalizadoException;
import aula.microservicos.login.repository.LoginAttemptRepository;
import aula.microservicos.login.repository.UsuarioRepository;

@Service
public class LoginServico {

    private static final Logger logger = LoggerFactory.getLogger(LoginServico.class);

    // o enunciado diz "falhar mais de uma vez": bloqueia quando failedAttempts > MAXIMO_FALHAS (2a falha seguida)
    private static final int MAXIMO_FALHAS = 1;

    // o BCrypt considera no maximo 72 bytes
    private static final int MAXIMO_BYTES_SENHA = 72;

    private UsuarioRepository usuarioRepo = null;
    private LoginAttemptRepository tentativaRepo = null;
    private PasswordEncoder passwordEncoder = null;

    public LoginServico(UsuarioRepository usuarioRepo, LoginAttemptRepository tentativaRepo,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.tentativaRepo = tentativaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // senha nula, vazia ou com mais de 72 bytes em UTF-8 e considerada invalida
    private boolean senhaDentroDoPadrao(String senha) {
        return senha != null && !senha.isBlank()
                && senha.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_BYTES_SENHA;
    }

    private LoginAttemptEntity obterTentativas(String username) {
        Optional<LoginAttemptEntity> resultado = tentativaRepo.findById(username);
        if (resultado.isPresent()) {
            return resultado.get();
        }
        return tentativaRepo.save(new LoginAttemptEntity(username));
    }

    @Transactional
    public boolean login(String username, String senha) {

        if (username == null || username.isBlank() || !senhaDentroDoPadrao(senha)) {
            logger.debug("Login invalido: dados incompletos ou fora do padrao");
            return false;
        }

        Optional<UsuarioEntity> resultado = usuarioRepo.findById(username);
        if (resultado.isEmpty()) {
            logger.debug("Login invalido: usuario inexistente = " + username);
            return false;
        }

        UsuarioEntity usuario = resultado.get();
        LoginAttemptEntity tentativa = obterTentativas(username);

        if (tentativa.isLocked()) {
            logger.debug("Login negado: usuario bloqueado = " + username);
            return false;
        }

        LocalDateTime agora = LocalDateTime.now();

        if (passwordEncoder.matches(senha, usuario.getSenha())) {
            tentativa.setFailedAttempts(0);
            tentativa.setUpdatedAt(agora);
            tentativaRepo.save(tentativa);
            logger.debug("Login efetuado para usuario = " + username);
            return true;
        }

        tentativa.setFailedAttempts(tentativa.getFailedAttempts() + 1);
        tentativa.setLastFailedAt(agora);
        tentativa.setUpdatedAt(agora);
        if (tentativa.getFailedAttempts() > MAXIMO_FALHAS) {
            tentativa.setLocked(true);
            tentativa.setLockedAt(agora);
            logger.debug("Usuario bloqueado = " + username);
        }
        tentativaRepo.save(tentativa);
        logger.debug("Senha invalida para usuario = " + username);
        return false;
    }

    @Transactional
    public void trocarSenha(String userid, String atual, String nova) {

        Optional<UsuarioEntity> resultado = usuarioRepo.findById(userid);
        if (resultado.isEmpty()) {
            throw new UsuarioNaoLocalizadoException();
        }

        UsuarioEntity usuario = resultado.get();
        LoginAttemptEntity tentativa = obterTentativas(userid);

        if (tentativa.isLocked()) {
            throw new UsuarioBloqueadoException();
        }

        if (!senhaDentroDoPadrao(atual) || !passwordEncoder.matches(atual, usuario.getSenha())) {
            throw new SenhaInvalidaException();
        }

        if (!senhaDentroDoPadrao(nova)) {
            throw new SenhaForaDoPadraoException();
        }

        String hash = passwordEncoder.encode(nova);
        usuario.setSenha(hash);
        usuarioRepo.save(usuario);

        tentativa.setUpdatedAt(LocalDateTime.now());
        tentativaRepo.save(tentativa);

        logger.debug("Senha alterada para usuario = " + userid);
    }

    @Transactional
    public UsuarioBean cadastrar(String username, String senha) {

        if (usuarioRepo.existsById(username)) {
            throw new UsuarioJaCadastradoException();
        }

        if (!senhaDentroDoPadrao(senha)) {
            throw new SenhaForaDoPadraoException();
        }

        String hash = passwordEncoder.encode(senha);
        usuarioRepo.save(new UsuarioEntity(username, hash));
        tentativaRepo.save(new LoginAttemptEntity(username));

        logger.debug("Usuario cadastrado = " + username);
        return new UsuarioBean(username, false, 0);
    }

    @Transactional
    public void desbloquear(String userid) {

        if (!usuarioRepo.existsById(userid)) {
            throw new UsuarioNaoLocalizadoException();
        }

        LoginAttemptEntity tentativa = obterTentativas(userid);
        tentativa.setLocked(false);
        tentativa.setFailedAttempts(0);
        tentativa.setLockedAt(null);
        tentativa.setUpdatedAt(LocalDateTime.now());
        tentativaRepo.save(tentativa);

        logger.debug("Usuario desbloqueado = " + userid);
    }

    @Transactional(readOnly = true)
    public Iterable<UsuarioBean> listar() {

        List<UsuarioBean> usuarios = new ArrayList<UsuarioBean>();
        for (UsuarioEntity usuario : usuarioRepo.findAllByOrderByUsernameAsc()) {
            Optional<LoginAttemptEntity> tentativa = tentativaRepo.findById(usuario.getUsername());
            if (tentativa.isPresent()) {
                usuarios.add(new UsuarioBean(usuario.getUsername(), tentativa.get().isLocked(),
                        tentativa.get().getFailedAttempts()));
            } else {
                usuarios.add(new UsuarioBean(usuario.getUsername(), false, 0));
            }
        }
        return usuarios;
    }

}
